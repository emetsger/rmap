package info.rmapproject.indexing.solr.repository;

import info.rmapproject.core.model.RMapIri;
import info.rmapproject.core.model.RMapStatus;
import info.rmapproject.core.model.agent.RMapAgent;
import info.rmapproject.core.model.event.RMapEvent;
import info.rmapproject.indexing.solr.IndexUtils;
import info.rmapproject.indexing.solr.model.DiscoSolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.PartialUpdate;

import java.net.URI;
import java.util.Collections;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static info.rmapproject.core.model.RMapStatus.ACTIVE;
import static info.rmapproject.core.model.RMapStatus.INACTIVE;
import static info.rmapproject.indexing.solr.IndexUtils.assertNotNull;
import static info.rmapproject.indexing.solr.IndexUtils.dateToString;
import static info.rmapproject.indexing.solr.IndexUtils.irisEqual;
import static info.rmapproject.indexing.solr.IndexUtils.ise;
import static info.rmapproject.indexing.solr.model.DiscoSolrDocument.CORE_NAME;
import static info.rmapproject.indexing.solr.model.DiscoSolrDocument.DISCO_STATUS;
import static info.rmapproject.indexing.solr.model.DiscoSolrDocument.DOC_ID;
import static info.rmapproject.indexing.solr.model.DiscoSolrDocument.DOC_LAST_UPDATED;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class CustomRepoImpl implements CustomRepo {

    private static final String ERR_INFER_STATUS = "Unable to infer the status for RMap DiSCO: %s Event: %s: Agent: %s";

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private DiscoRepository delegate;

    @Autowired
    private SolrTemplate template;

    @Autowired
    private StatusInferencer inferencer;

    @Autowired
    private IndexableThingMapper mapper;

    DiscoRepository getDelegate() {
        return delegate;
    }

    void setDelegate(DiscoRepository delegate) {
        IndexUtils.assertNotNull(delegate, "DiscoRepository delegate must not be null.");
        this.delegate = delegate;
    }

    SolrTemplate getTemplate() {
        return template;
    }

    void setTemplate(SolrTemplate template) {
        IndexUtils.assertNotNull(template, "SolrTemplate must not be null.");
        this.template = template;
    }

    StatusInferencer getInferencer() {
        return inferencer;
    }

    void setInferencer(StatusInferencer inferencer) {
        IndexUtils.assertNotNull(inferencer, "StatusInferencer must not be null.");
        this.inferencer = inferencer;
    }

    IndexableThingMapper getMapper() {
        return mapper;
    }

    void setMapper(IndexableThingMapper mapper) {
        IndexUtils.assertNotNull(mapper, "IndexableThingMapper must not be null.");
        this.mapper = mapper;
    }

    /**
     * Accepts the supplied {@link IndexDTO data transfer object} (DTO) for indexing.  The caller should assume that the
     * index is updated asynchronously, and that multiple transactions with the index may occur as a result.
     * <p>
     * Recall that the {@code IndexDTO} forms a connected graph rooted by the RMap event, and contains the source and
     * target resources of the event.  This method uses the information in the {@code IndexDTO} to issue one or more
     * updates to the index.
     * </p>
     *
     * @param toIndex the DTO containing the information to be indexed
     */
    @Override
    public void index(IndexDTO toIndex) {
        assertNotNull(toIndex, "The supplied IndexDTO must not be null.");

        RMapEvent event = assertNotNull(toIndex.getEvent(), ise("The IndexDTO must not have a null event."));
        RMapAgent agent = assertNotNull(toIndex.getAgent(), ise("The IndexDTO must not have a null agent."));

        if (!(irisEqual(event.getAssociatedAgent(), agent.getId()))) {
            throw new RuntimeException(String.format(
                    "Missing agent '%s' of event %s", event.getAssociatedAgent().getStringValue(), event));
        }

        // The source IRI will be null in the case of a creation event
        RMapIri source = toIndex.getEventSourceIri();
        // The target IRI will be null in the case of a delete, tombstone, or inactivation event
        RMapIri target = toIndex.getEventTargetIri();

        IndexableThing forSource = null;
        if (source != null) {
            forSource = new IndexableThing();

            forSource.eventSource = source;
            forSource.eventTarget = target;
            forSource.event = event;
            forSource.agent = agent;
            forSource.disco = toIndex.getSourceDisco();
            forSource.status = inferencer.inferDiscoStatus(forSource.disco, forSource.event, forSource.agent)
                    .orElseThrow(() -> new RuntimeException(
                            String.format(ERR_INFER_STATUS, toIndex.getSourceDisco().getId(),
                                    toIndex.getEvent().getId(), toIndex.getAgent().getId())));
        }

        IndexableThing forTarget = null;
        if (target != null) {
            forTarget = new IndexableThing();
            forTarget.eventSource = source;
            forTarget.eventTarget = target;
            forTarget.event = event;
            forTarget.agent = agent;
            forTarget.disco = toIndex.getTargetDisco();
            forTarget.status = inferencer.inferDiscoStatus(forTarget.disco, forTarget.event, forTarget.agent)
                    .orElseThrow(() -> new RuntimeException(
                            String.format(ERR_INFER_STATUS, toIndex.getTargetDisco().getId(),
                                    toIndex.getEvent().getId(), toIndex.getAgent().getId())));
        }


        DiscoSolrDocument indexedSourceDoc = (forSource != null) ? delegate.save(mapper.apply(forSource)) : null;
        DiscoSolrDocument indexedTargetDoc = (forTarget != null) ? delegate.save(mapper.apply(forTarget)) : null;

        /*
              Event            Source                    Target
              =====            ======                    ======
              CREATION         n/a                       created disco, or created agent
              UPDATE           disco being updated       updated disco
              DELETE           disco being deleted       n/a
              REPLACE          agent being updated       updated agent
              TOMBSTONE        disco being tombed        n/a
              DERIVATION       disco being derived from  the derived disco
              INACTIVATION     disco being inactivated   n/a
         */
        switch (event.getEventType()) {
            case CREATION:
                // no-op, nothing else needs to be done to the disco index
                // TODO: update the version index
                break;

            case UPDATE:
                // need to change the status flag on the _older_ versions of the disco in the disco index
                // TODO: update the version index
                inactivateDocumentsForDisco(forSource.disco.getId(), null);
                break;

            case DELETION:
                // need to change the status flag on _all_ versions of the disco in the disco index
                // TODO: update the version index
                updateDocumentStatusByDiscoIri(forSource.disco.getId(), RMapStatus.DELETED, null);
                break;

            case TOMBSTONE:
                // need to change the status flag on _all_ versions of the disco in the disco index
                // and/or delete all the old versions of the disco from the index and leave the tombstoned instance
                // TODO: update the version index
                updateDocumentStatusByDiscoIri(forSource.disco.getId(), RMapStatus.TOMBSTONED, null);
                break;

            case DERIVATION:
                // the disco being derived from does not need to change
                // the derived disco does not need to be updated either, I don't think.
                // TODO: update the version index
                break;

            case INACTIVATION:
                // need to change the status flag on _all_ versions of the disco in the disco index
                // TODO: update the version index
                updateDocumentStatusByDiscoIri(forSource.disco.getId(), RMapStatus.INACTIVE, null);
                break;

            default:
                throw new RuntimeException("Unknown event type: " + event.getEventType());

        }
    }

    /**
     * Searches the index for documents that contain the referenced DiSCO <em>and</em> that have a DiSCO status of
     * {@link RMapStatus#ACTIVE ACTIVE}.  Each matching document will have the DiSCO status set to {@link
     * RMapStatus#INACTIVE INACTIVE}, and have their last updated timestamp set.
     * <p>
     * Implementation note: this method uses the {@link SolrTemplate} in order to perform a <em>partial update</em> of
     * the matching documents.  This is for two reasons: 1) partial updates are more efficient, 2) round-tripping the
     * entire {@link DiscoSolrDocument} is not possible due to how the {@link org.apache.solr.common.util.JavaBinCodec}
     * writes dates in Solr responses.
     * </p>
     *
     * @param discoIri
     * @param filter
     */
    private void inactivateDocumentsForDisco(RMapIri discoIri, Predicate<DiscoSolrDocument> filter) {

        log.debug("Inactivating documents with DiSCO iri {}...", discoIri);

        Set<PartialUpdate> statusUpdates;

        try (Stream<DiscoSolrDocument> documentStream =
                     delegate.findDiscoSolrDocumentsByDiscoUriAndDiscoStatus(discoIri.getStringValue(), ACTIVE.toString()).stream()) {

            Stream<DiscoSolrDocument> filtered = documentStream;
            if (filter != null) {
                filtered = documentStream.filter(filter);
            }

            statusUpdates = preparePartialUpdateOverDocuments(filtered, (partialUpdate) -> {
                partialUpdate.setValueOfField(DISCO_STATUS, INACTIVE.toString());
                partialUpdate.setValueOfField(DOC_LAST_UPDATED, System.currentTimeMillis());
                log.debug("Set document id {} status to {}", partialUpdate.getIdField().getValue(), INACTIVE);
            });
        }

        if (statusUpdates.size() > 0) {
            template.saveBeans(CORE_NAME, statusUpdates);
            template.commit(CORE_NAME);
        }
    }

    /**
     * Updates the {@link DiscoSolrDocument#DISCO_STATUS disco_status} of Solr documents that have a
     * {@link DiscoSolrDocument#DISCO_URI disco_uri} matching the supplied {@code discoIri}.  The matching Solr
     * documents may be filtered by supplying a {@code Predicate}, in which case only the filtered Solr documents will
     * be updated.
     * <p>
     * Note that if the {@code filter} is being used to perform a potentially expensive computation, or if the response
     * from the index contains many matches that will be filtered by a trivial computation, it may be worth considering
     * adding a repository-specific method expressing the more narrow criteria, and invoking that method instead.  It is
     * likely the index will be able to apply the filtering logic in a more performant manner than the supplied
     * {@code filter}.
     * </p>
     *
     * @param discoIri the IRI of the DiSCO
     * @param newStatus the status matching DiSCOs will be updated to
     * @param filter an optional {@code Predicate} used to selectively apply status updates, may be {@code null}
     */
    void updateDocumentStatusByDiscoIri(RMapIri discoIri, RMapStatus newStatus, Predicate<DiscoSolrDocument> filter) {

        log.debug("Updating the status of the following documents with DiSCO iri {} to {}", discoIri, newStatus);

        Set<PartialUpdate> statusUpdates;

        try (Stream<DiscoSolrDocument> documentStream =
                     delegate.findDiscoSolrDocumentsByDiscoUri(discoIri.getStringValue()).stream()) {

            Stream<DiscoSolrDocument> filtered = documentStream;
            if (filter != null) {
                filtered = documentStream.filter(filter);
            }

            statusUpdates = preparePartialUpdateOverDocuments(filtered, (partialUpdate) -> {
                partialUpdate.setValueOfField(DISCO_STATUS, newStatus.toString());
                partialUpdate.setValueOfField(DOC_LAST_UPDATED, System.currentTimeMillis());
                log.debug("Set document id {} status to {}", partialUpdate.getIdField().getValue(), newStatus);
            });
        }

        if (statusUpdates.size() > 0) {
            template.saveBeans(CORE_NAME, statusUpdates);
            template.commit(CORE_NAME);
        }
    }

    /**
     * Creates a {@link PartialUpdate} instance for each {@code DiscoSolrDocument}. The supplied {@code Consumer} is
     * applied to each {@code PartialUpdate}, setting the state of each update in preparation for being sent to the
     * index.
     *
     * @param documents the {@code DiscoSolrDocument}s to update
     * @param updater sets the state of each {@code PartialUpdate}
     * @return a {@code Set} of {@code PartialUpdate} instances their state containing the commands to be sent to the
     *         index
     */
    private Set<PartialUpdate> preparePartialUpdateOverDocuments(Stream<DiscoSolrDocument> documents,
                                                                 Consumer<PartialUpdate> updater) {
        return documents.map(doc -> new PartialUpdate(DOC_ID, doc.getDocId()))
                .peek(updater)
                .collect(Collectors.toSet());
    }

}
