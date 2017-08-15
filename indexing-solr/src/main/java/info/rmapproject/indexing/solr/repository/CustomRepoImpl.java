package info.rmapproject.indexing.solr.repository;

import info.rmapproject.core.model.RMapIri;
import info.rmapproject.core.model.RMapStatus;
import info.rmapproject.core.model.agent.RMapAgent;
import info.rmapproject.core.model.disco.RMapDiSCO;
import info.rmapproject.core.model.event.RMapEvent;
import info.rmapproject.core.model.event.RMapEventType;
import info.rmapproject.indexing.solr.IndexUtils;
import info.rmapproject.indexing.solr.model.DiscoSolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.PartialUpdate;

import java.net.URI;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static info.rmapproject.core.model.RMapStatus.ACTIVE;
import static info.rmapproject.core.model.RMapStatus.DELETED;
import static info.rmapproject.core.model.RMapStatus.INACTIVE;
import static info.rmapproject.core.model.RMapStatus.TOMBSTONED;
import static info.rmapproject.core.model.event.RMapEventType.CREATION;
import static info.rmapproject.core.model.event.RMapEventType.DELETION;
import static info.rmapproject.core.model.event.RMapEventType.DERIVATION;
import static info.rmapproject.core.model.event.RMapEventType.REPLACE;
import static info.rmapproject.core.model.event.RMapEventType.UPDATE;
import static info.rmapproject.indexing.solr.IndexUtils.assertNotNull;
import static info.rmapproject.indexing.solr.IndexUtils.dateToString;
import static info.rmapproject.indexing.solr.IndexUtils.irisEqual;
import static info.rmapproject.indexing.solr.model.DiscoSolrDocument.CORE_NAME;
import static info.rmapproject.indexing.solr.model.DiscoSolrDocument.DISCO_STATUS;
import static info.rmapproject.indexing.solr.model.DiscoSolrDocument.DOC_ID;
import static info.rmapproject.indexing.solr.model.DiscoSolrDocument.DOC_LAST_UPDATED;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class CustomRepoImpl implements CustomRepo {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private DiscoRepository delegate;

    @Autowired
    private SolrTemplate template;

    @Override
    public void index(IndexDTO toIndex) {
        assertNotNull(toIndex);

        RMapEvent event = assertNotNull(toIndex.getEvent());
        RMapAgent agent = assertNotNull(toIndex.getAgent());

        if (!(irisEqual(event.getAssociatedAgent(), agent.getId()))) {
            throw new RuntimeException("Missing agent '" + event.getAssociatedAgent().getStringValue() +
                    "' of event " + event);
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
            forSource.status = inferDiscoStatus(forSource.disco, forSource.event, forSource.agent)
                    .orElseThrow(() -> new RuntimeException(
                            String.format("Unable to infer the status for RMap DiSCO: %s Event: %s: Agent: %s",
                                    toIndex.getSourceDisco().getId(), toIndex.getEvent().getId(), toIndex.getAgent().getId())));
        }

        IndexableThing forTarget = null;
        if (target != null) {
            forTarget = new IndexableThing();
            forTarget.eventSource = source;
            forTarget.eventTarget = target;
            forTarget.event = event;
            forTarget.agent = agent;
            forTarget.disco = toIndex.getTargetDisco();
            forTarget.status = inferDiscoStatus(forTarget.disco, forTarget.event, forTarget.agent)
                    .orElseThrow(() -> new RuntimeException(
                            String.format("Unable to infer the status for RMap DiSCO: %s Event: %s: Agent: %s",
                                    toIndex.getTargetDisco().getId(), toIndex.getEvent().getId(), toIndex.getAgent().getId())));
        }


        DiscoSolrDocument indexedSourceDoc = (forSource != null) ? delegate.save(toDocument(forSource)) : null;
        DiscoSolrDocument indexedTargetDoc = (forTarget != null) ? delegate.save(toDocument(forTarget)) : null;

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

    private void updateDocumentStatusByDiscoIri(RMapIri discoIri, RMapStatus newStatus, Predicate<DiscoSolrDocument> filter) {

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

    private Set<PartialUpdate> preparePartialUpdateOverDocuments(Stream<DiscoSolrDocument> documents,
                                                                 Consumer<PartialUpdate> updater) {
        return documents.map(doc -> new PartialUpdate(DOC_ID, doc.getDocId()))
                .peek(updater)
                .collect(Collectors.toSet());
    }


    DiscoSolrDocument toDocument(IndexableThing indexableThing) {
        DiscoSolrDocument doc = new DiscoSolrDocument();

        doc.setDiscoRelatedStatements(indexableThing.disco.getRelatedStatements().stream()
                .map(t -> String.format("%s %s %s", t.getSubject().getStringValue(), t.getPredicate().getStringValue(), t.getObject().getStringValue()))
                .collect(Collectors.toList()));
        doc.setDiscoUri(indexableThing.disco.getId().getStringValue());
        doc.setDiscoCreatorUri(indexableThing.disco.getCreator().getStringValue());               // TODO: Resolve creator and index creator properties?
        doc.setDiscoAggregatedResourceUris(indexableThing.disco.getAggregatedResources()
                .stream().map(URI::toString).collect(Collectors.toList()));
        doc.setDiscoDescription(indexableThing.disco.getDescription().getStringValue());
        doc.setDiscoProvenanceUri(indexableThing.disco.getProvGeneratedBy() != null ? indexableThing.disco.getProvGeneratedBy().getStringValue() : null);
        doc.setDiscoStatus(indexableThing.status.toString());

        doc.setAgentUri(indexableThing.agent.getId().getStringValue());
        doc.setAgentDescription(indexableThing.agent.getName().getStringValue());
        doc.setAgentProviderUri(indexableThing.agent.getIdProvider().getStringValue());
        // TODO? toIndex.agent.getAuthId()

        doc.setEventUri(indexableThing.event.getId().getStringValue());
        doc.setEventAgentUri(indexableThing.event.getAssociatedAgent().getStringValue());
        doc.setEventDescription(indexableThing.event.getDescription() != null ? indexableThing.event.getDescription().getStringValue() : null);
        doc.setEventStartTime(dateToString(indexableThing.event.getStartTime()));
        doc.setEventEndTime(dateToString(indexableThing.event.getEndTime()));
        doc.setEventType(indexableThing.event.getEventType().name());
        doc.setEventTargetObjectUris(Collections.singletonList(indexableThing.eventTarget.getStringValue()));
        if (indexableThing.eventSource != null) {
            doc.setEventSourceObjectUris(Collections.singletonList(indexableThing.eventSource.getStringValue()));
        }

        return doc;
    }

    /**
     * Infer the status of the supplied DiSCO.  The DiSCO can be considered as input to, or output from, the supplied
     * {@code event}, depending on whether the DiSCO is the source or target referenced by the {@code event}.
     * <p>
     * For example, if the DiSCO is the <em>target</em> of a {@link RMapEventType#CREATION CREATION event}, then it is
     * inferred to have an {@link RMapStatus#ACTIVE ACTIVE status}.  If the DiSCO is the <em>source</em> of a {@link
     * RMapEventType#UPDATE UPDATE event}, then it is inferred to have an {@link RMapStatus#INACTIVE INACTIVE status}.
     * Likewise, if the DiSCO is the <em>target</em> of an {@code UPDATE} event, then it would be inferred to have an
     * {@code ACTIVE} status.
     * </p>
     * <p>
     * The {@code agent} is supplied for completeness, but it not considered in the implementation.
     * </p>
     *
     * @param disco the DiSCO referenced by {@code event}
     * @param event the event referencing the {@code disco}; the {@code disco} may be the source or target of the
     *              {@code event}
     * @param agent the agent that generated the event, supplied for completeness but not used
     * @return the inferred status
     * @throws IllegalStateException if the {@code event} target or source does not reference the supplied
     *                               {@code disco}, or if a reference is missing
     * @throws RuntimeException if the {@code event} source or target IRI is {@code null}
     */
    Optional<RMapStatus> inferDiscoStatus(RMapDiSCO disco, RMapEvent event, RMapAgent agent) {
        log.trace("Inferring DiSCO status for DiSCO {}, {} event {}, agent {}",
                (disco != null) ? disco.getId().getStringValue() : "null",
                (event != null) ? event.getEventType().toString() : "null",
                (event != null) ? event.getId().getStringValue() : "null",
                (agent != null) ? agent.getId().getStringValue() : "null");

        IndexUtils.assertNotNull(disco,"Supplied disco must not be null");
        IndexUtils.assertNotNull(event, "Supplied event must not be null");
        IndexUtils.assertNotNull(agent, "Supplied agent must not be null");

        RMapStatus status = null;

        Optional<RMapIri> sourceIri = null;
        Optional<RMapIri> targetIri = null;
        try {
            sourceIri = IndexUtils.findEventIri(event, IndexUtils.EventDirection.SOURCE);
            targetIri = IndexUtils.findEventIri(event, IndexUtils.EventDirection.TARGET);
        } catch (RuntimeException e) {
            throw new RuntimeException(String.format(
                    "Error resolving an event IRI for DiSCO %s, Event %s, Agent %s: %s",
                    disco.getId().getStringValue(), event.getId().getStringValue(), agent.getId().getStringValue(),
                    e.getMessage()), e);
        }

        log.trace("Found source event: '{}', target event: '{}'",
                sourceIri.orElse(null), targetIri.orElse(null));

        switch (event.getEventType()) {
            case CREATION:
                if (!targetIri.isPresent()) {
                    throw new IllegalStateException("No CREATION event target for event " +
                            event.getId().getStringValue());
                }

                if (irisEqual(targetIri, disco.getId())) {
                    logInference(CREATION, disco.getId(), IndexUtils.EventDirection.TARGET);
                    status = ACTIVE;
                } else {
                    throw new IllegalStateException(String.format(
                            "Missing DiSCO %s for CREATION event target %s%n",
                            targetIri.get().getStringValue(), event.getId().getStringValue()));
                }

                break;

            case DERIVATION:
                if (targetIri.isPresent() && irisEqual(targetIri, disco.getId())) {
                    logInference(DERIVATION, disco.getId(), IndexUtils.EventDirection.TARGET);
                    status = ACTIVE;
                } else if (sourceIri.isPresent() && irisEqual(sourceIri, disco.getId())) {
                    logInference(DERIVATION, disco.getId(), IndexUtils.EventDirection.SOURCE);
                    status = INACTIVE;
                } else {
                    throw new IllegalStateException("Missing DERIVATION event source and target IRI for event " +
                            event.getId().getStringValue());
                }
                break;

            case UPDATE:
                if (targetIri.isPresent() && irisEqual(targetIri, disco.getId())) {
                    logInference(UPDATE, disco.getId(), IndexUtils.EventDirection.TARGET);
                    status = ACTIVE;
                } else if (sourceIri.isPresent() && irisEqual(sourceIri, disco.getId())) {
                    logInference(UPDATE, disco.getId(), IndexUtils.EventDirection.SOURCE);
                    status = INACTIVE;
                } else {
                    throw new IllegalStateException("Missing UPDATE event source and target IRI for event " +
                            event.getId().getStringValue());
                }
                break;

            case DELETION:
                if (targetIri.isPresent() || sourceIri.isPresent() &&
                        (irisEqual(targetIri, disco.getId()) || irisEqual(sourceIri, disco.getId()))) {
                    logInference(DELETION, disco.getId(), IndexUtils.EventDirection.TARGET);
                    status = DELETED;
                } else {
                    throw new IllegalStateException("Missing DELETION event source or target IRI for event " +
                            event.getId().getStringValue());
                }
                break;

            case TOMBSTONE:
                if (targetIri.isPresent() || sourceIri.isPresent() &&
                        (irisEqual(targetIri, disco.getId()) || irisEqual(sourceIri, disco.getId()))) {
                    status = TOMBSTONED;
                } else {
                    throw new IllegalStateException("Missing TOMBSTONED event source or target IRI for event " +
                            event.getId().getStringValue());
                }
                break;

            case INACTIVATION:
                if (targetIri.isPresent() || sourceIri.isPresent() &&
                        (irisEqual(targetIri, disco.getId()) || irisEqual(sourceIri, disco.getId()))) {
                    status = INACTIVE;
                } else {
                    throw new IllegalStateException("Missing INACTIVATION event source or target IRI for event " +
                            event.getId().getStringValue());
                }
                break;

            case REPLACE:
                if (targetIri.isPresent() && irisEqual(targetIri, disco.getId())) {
                    logInference(REPLACE, disco.getId(), IndexUtils.EventDirection.TARGET);
                    status = ACTIVE;
                } else {
                    throw new IllegalStateException("Missing REPLACE event target IRI for event " +
                            event.getId().getStringValue());
                }
                break;

            default:
                throw new RuntimeException("Unknown RMap event type: " + event.getEventType());
        }

        log.debug("Inferred DiSCO status for [DiSCO {}, {} event {}, agent {}] to be {}",
                disco.getId().getStringValue(), event.getEventType().toString(), event.getId().getStringValue(),
                agent.getId().getStringValue(), status);

        return Optional.of(status);
    }

    /**
     * Logs the reason behind the inferencing result at TRACE level
     *
     * @param type event type
     * @param iri the iri of the disco
     * @param direction whether the disco is the source of the event, the target of the event, or either (i.e. it
     *                  the direction doesn't matter)
     */
    private void logInference(RMapEventType type, RMapIri iri, IndexUtils.EventDirection direction) {
        if (!log.isTraceEnabled()) {
            return;
        }

        if (direction != IndexUtils.EventDirection.EITHER) {
            log.trace("{} event {} iri equals disco iri: {}",
                    type, (direction == IndexUtils.EventDirection.SOURCE ? "source" : "target"), iri.getStringValue());
        } else {
            log.trace("{} event source or target iri equals disco iri: {}", type, direction, iri.getStringValue());
        }
    }

    /**
     * Encapsulates an indexable unit.
     */
    class IndexableThing {
        RMapEvent event;
        RMapDiSCO disco;
        RMapAgent agent;
        RMapStatus status;
        RMapIri eventSource;
        RMapIri eventTarget;
    }
}
