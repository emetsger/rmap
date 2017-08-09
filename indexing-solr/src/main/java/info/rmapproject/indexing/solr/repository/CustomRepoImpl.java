package info.rmapproject.indexing.solr.repository;

import com.sun.javafx.event.EventDispatchTree;
import info.rmapproject.core.model.RMapIri;
import info.rmapproject.core.model.RMapObject;
import info.rmapproject.core.model.RMapObjectType;
import info.rmapproject.core.model.RMapStatus;
import info.rmapproject.core.model.agent.RMapAgent;
import info.rmapproject.core.model.disco.RMapDiSCO;
import info.rmapproject.core.model.event.RMapEvent;
import info.rmapproject.core.model.event.RMapEventCreation;
import info.rmapproject.core.model.event.RMapEventDeletion;
import info.rmapproject.core.model.event.RMapEventDerivation;
import info.rmapproject.core.model.event.RMapEventInactivation;
import info.rmapproject.core.model.event.RMapEventTombstone;
import info.rmapproject.core.model.event.RMapEventType;
import info.rmapproject.core.model.event.RMapEventUpdate;
import info.rmapproject.core.model.event.RMapEventUpdateWithReplace;
import info.rmapproject.core.model.event.RMapEventWithNewObjects;
import info.rmapproject.indexing.solr.IndexUtils;
import info.rmapproject.indexing.solr.model.DiscoSolrDocument;
import org.apache.zookeeper.Op;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.solr.repository.SolrCrudRepository;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static info.rmapproject.core.model.RMapStatus.ACTIVE;
import static info.rmapproject.core.model.RMapStatus.DELETED;
import static info.rmapproject.core.model.RMapStatus.INACTIVE;
import static info.rmapproject.core.model.RMapStatus.TOMBSTONED;
import static info.rmapproject.indexing.solr.IndexUtils.assertNotNull;
import static info.rmapproject.indexing.solr.IndexUtils.assertNotNullOrEmpty;
import static info.rmapproject.indexing.solr.IndexUtils.dateToString;
import static info.rmapproject.indexing.solr.IndexUtils.irisEqual;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class CustomRepoImpl implements CustomRepo {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private DiscoRepository delegate;

    @Override
    public void index(IndexDTO toIndex) {
        assertNotNull(toIndex);

        RMapEvent event = toIndex.getEvent();
        RMapAgent agent = toIndex.getAgent();
        assertNotNull(event);
        assertNotNull(agent);

        if ( !(event.getAssociatedAgent().getStringValue().equals(agent.getId().getStringValue()))) {
            throw new RuntimeException("Missing agent '" + event.getAssociatedAgent().getStringValue() +
                    "' of event " + event);
        }

        RMapIri source;
        RMapIri target;

        switch (event.getEventType()) {
            case CREATION:
                source = null;
                target = ((RMapEventWithNewObjects) event).getCreatedObjectIds().get(0);
                break;
            case UPDATE:
                source = ((RMapEventUpdate) event).getInactivatedObjectId();
                target = ((RMapEventUpdate) event).getDerivedObjectId();
                break;
            case DERIVATION:
                source = ((RMapEventDerivation) event).getSourceObjectId();
                target = ((RMapEventDerivation) event).getDerivedObjectId();
                break;
            default:
                throw new RuntimeException("Unhandled event type " + event);
        }

        IndexableThing forSource = null;
        // The source IRI will be null in the case of a creation event
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

        // The target IRI should never be null
        IndexableThing forTarget = new IndexableThing();

        forTarget.eventSource = source;
        forTarget.eventTarget = target;
        forTarget.event = event;
        forTarget.agent = agent;
        forTarget.disco = toIndex.getTargetDisco();
        forTarget.status = inferDiscoStatus(forTarget.disco, forTarget.event, forTarget.agent)
                .orElseThrow(() -> new RuntimeException(
                        String.format("Unable to infer the status for RMap DiSCO: %s Event: %s: Agent: %s",
                                toIndex.getTargetDisco().getId(), toIndex.getEvent().getId(), toIndex.getAgent().getId())));

        if (forSource != null) {
            delegate.save(toDocument(forSource));

            Set<DiscoSolrDocument> docsToUpdate = delegate.findDiscoSolrDocumentsByDiscoStatusAndDiscoUri(
                    ACTIVE.toString(), forSource.disco.getId().getStringValue())
                    .stream()
                    .peek(doc -> doc.setDiscoStatus(INACTIVE.toString()))
                    .collect(Collectors.toSet());

            delegate.saveAll(docsToUpdate);
        }

        delegate.save(toDocument(forTarget));


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

    public DiscoRepository getDelegate() {
        return delegate;
    }

    public void setDelegate(DiscoRepository delegate) {
        this.delegate = delegate;
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

    Optional<RMapStatus> inferDiscoStatus(RMapDiSCO disco, RMapEvent event, RMapAgent agent) {
        log.debug("Inferring DiSCO status for DiSCO '{}', {} event '{}', agent '{}'",
                    disco.getId().getStringValue(), event.getEventType().toString(), event.getId().getStringValue(),
                    agent.getId().getStringValue());

        RMapStatus status = null;

        Optional<RMapIri> sourceIri = null;
        Optional<RMapIri> targetIri = null;
        try {
            sourceIri = findEventIri(event, EventDirection.SOURCE);
            targetIri = findEventIri(event, EventDirection.TARGET);
        } catch (RuntimeException e) {
            throw new RuntimeException(String.format(
                    "Error resolving an event IRI for DiSCO %s, Event %s, Agent %s: %s",
                    disco.getId().getStringValue(), event.getId().getStringValue(), agent.getId().getStringValue(),
                    e.getMessage()), e);
        }

        log.debug("Found source event IRI: '{}', target event IRI: '{}'",
                sourceIri.orElse(null), targetIri.orElse(null));

        switch (event.getEventType()) {
            case CREATION:
                if (!targetIri.isPresent()) {
                    throw new IllegalArgumentException("No CREATION event target IRI for event " +
                            event.getId().getStringValue());
                }

                if (irisEqual(targetIri, disco.getId())) {
                    status = ACTIVE;
                } else {
                    throw new IllegalArgumentException(String.format(
                            "Missing DiSCO %s for CREATION event target %s%n",
                            targetIri.get().getStringValue(), event.getId().getStringValue()));
                }

                break;

            case DERIVATION:
                if (targetIri.isPresent() && irisEqual(targetIri, disco.getId())) {
                    status = ACTIVE;
                } else if (sourceIri.isPresent() && irisEqual(sourceIri, disco.getId())) {
                    status = INACTIVE;
                } else {
                    throw new IllegalArgumentException("Missing DERIVATION event source and target IRI for event " +
                            event.getId().getStringValue());
                }
                break;

            case UPDATE:
                if (targetIri.isPresent() && irisEqual(targetIri, disco.getId())) {
                    status = ACTIVE;
                } else if (sourceIri.isPresent() && irisEqual(sourceIri, disco.getId())) {
                    status = INACTIVE;
                } else {
                    throw new IllegalArgumentException("Missing UPDATE event source and target IRI for event " +
                            event.getId().getStringValue());
                }
                break;

            case DELETION:
                if (targetIri.isPresent() || sourceIri.isPresent() &&
                        (irisEqual(targetIri, disco.getId()) || irisEqual(sourceIri, disco.getId()))) {
                    status = DELETED;
                } else {
                    throw new IllegalArgumentException("Missing DELETION event source or target IRI for event " +
                            event.getId().getStringValue());
                }
                break;

            case TOMBSTONE:
                if (targetIri.isPresent() || sourceIri.isPresent() &&
                        (irisEqual(targetIri, disco.getId()) || irisEqual(sourceIri, disco.getId()))) {
                    status = TOMBSTONED;
                } else {
                    throw new IllegalArgumentException("Missing TOMBSTONED event source or target IRI for event " +
                            event.getId().getStringValue());
                }
                break;

            case INACTIVATION:
                if (targetIri.isPresent() || sourceIri.isPresent() &&
                        (irisEqual(targetIri, disco.getId()) || irisEqual(sourceIri, disco.getId()))) {
                    status = INACTIVE;
                } else {
                    throw new IllegalArgumentException("Missing INACTIVATION event source or target IRI for event " +
                            event.getId().getStringValue());
                }
                break;

            case REPLACE:
                if (targetIri.isPresent() && irisEqual(targetIri, disco.getId())) {
                    status = ACTIVE;
                } else {
                    throw new IllegalArgumentException("Missing REPLACE event target IRI for event " +
                            event.getId().getStringValue());
                }
                break;

            default:
                throw new RuntimeException("Unknown RMap event type: " + event.getEventType());
        }

        log.debug("Inferred status: {}", status);
        return Optional.of(status);
    }

    Optional<RMapDiSCO> findDiscoForEvent(RMapEvent event, EventDirection sourceOrTarget, RMapDiSCO disco) {
        Map<RMapObjectType, Set<? extends RMapObject>> discoMap = new HashMap<>();
        discoMap.put(RMapObjectType.DISCO, Collections.singleton(disco));
        return findDiscoForEvent(event, sourceOrTarget, discoMap);
    }

    @SuppressWarnings("unchecked")
    Optional<RMapDiSCO> findDiscoForEvent(RMapEvent event, EventDirection sourceOrTarget,
                                          Map<RMapObjectType, Set<? extends RMapObject>> objects) {

        assertNotNullOrEmpty(objects);

        Optional<RMapIri> eventIri = findEventIri(event, sourceOrTarget);

        if (!eventIri.isPresent()) {
            return Optional.empty();
        }

        return (Optional<RMapDiSCO>) assertNotNullOrEmpty(objects.get(RMapObjectType.DISCO))
                .stream()
                .filter(disco -> disco.getId().getStringValue().equals(eventIri.get().getStringValue()))
                .findAny();
    }

    private Optional<RMapIri> findEventIri(RMapEvent event, EventDirection direction) {
        Optional<RMapIri> iri = Optional.empty();

        if (direction == EventDirection.TARGET) {
            switch (event.getEventType()) {
                case CREATION:
                    // TODO: handle multiple creation ids
                    iri = Optional.of(assertNotNullOrEmpty(((RMapEventCreation) event).getCreatedObjectIds()).get(0));
                    break;

                case DERIVATION:
                    iri = Optional.of(((RMapEventDerivation) event).getDerivedObjectId());
                    break;

                case UPDATE:
                    iri = Optional.of(((RMapEventUpdate) event).getDerivedObjectId());
                    break;

                case DELETION:
                    // TODO: handle multiple deletion ids
                    // TODO: decide what the source IRI and target IRI is for a deletion event
                    // Right now, the IRI of the deleted DiSCO is used for both
                    iri = Optional.of(assertNotNullOrEmpty(((RMapEventDeletion)event).getDeletedObjectIds()).get(0));
                    break;

                case TOMBSTONE:
                    // TODO: decide what the source IRI and target IRI is for a tombstone event
                    // Right now, the IRI of the tombstoned resource is used for both
                    iri = Optional.of(((RMapEventTombstone) event).getTombstonedResourceId());
                    break;

                case INACTIVATION:
                    // TODO: decide what the source IRI and target IRI is for an inactivation event
                    // Right now, the IRI of the inactivated disco is used for both
                    iri = Optional.of(((RMapEventInactivation) event).getInactivatedObjectId());
                    break;

                case REPLACE:
                    // TODO: missing the source object of a replacement?
                    iri = Optional.empty();

                default:
                    throw new IllegalArgumentException("Unknown RMap event type: " + event);

            }
        }

        if (direction == EventDirection.SOURCE) {
            switch (event.getEventType()) {
                case CREATION:
                    // TODO: handle multiple creation ids
                    iri = Optional.empty();
                    break;

                case DERIVATION:
                    iri = Optional.of(((RMapEventDerivation) event).getSourceObjectId());
                    break;

                case UPDATE:
                    iri = Optional.of(((RMapEventUpdate)event).getInactivatedObjectId());
                    break;

                case DELETION:
                    // TODO: handle multiple deletion ids
                    // TODO: decide what the source IRI and target IRI is for a deletion event
                    // Right now, the IRI of the deleted DiSCO is used for both
                    iri = Optional.of(assertNotNullOrEmpty(((RMapEventDeletion)event).getDeletedObjectIds()).get(0));
                    break;

                case TOMBSTONE:
                    // TODO: decide what the source IRI and target IRI is for a tombstone event
                    // Right now, the IRI of the tombstoned resource is used for both
                    iri = Optional.of(((RMapEventTombstone) event).getTombstonedResourceId());

                case INACTIVATION:
                    // TODO: decide what the source IRI and target IRI is for an inactivation event
                    // Right now, the IRI of the inactivated disco is used for both
                    iri = Optional.of(((RMapEventInactivation) event).getInactivatedObjectId());
                    break;

                case REPLACE:
                    iri = Optional.of(((RMapEventUpdateWithReplace) event).getUpdatedObjectId());

                default:
                    throw new IllegalArgumentException("Unknown RMap event type: " + event.getEventType());

            }
        }

        return iri;
    }

    private enum EventDirection { SOURCE, TARGET }
}
