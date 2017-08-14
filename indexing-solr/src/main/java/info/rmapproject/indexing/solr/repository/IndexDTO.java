package info.rmapproject.indexing.solr.repository;

import info.rmapproject.core.model.RMapIri;
import info.rmapproject.core.model.agent.RMapAgent;
import info.rmapproject.core.model.disco.RMapDiSCO;
import info.rmapproject.core.model.event.RMapEvent;
import info.rmapproject.indexing.solr.IndexUtils;

import java.util.Optional;

import static info.rmapproject.indexing.solr.IndexUtils.findEventIri;
import static info.rmapproject.indexing.solr.IndexUtils.irisEqual;

/**
 * Encapsulates the unit of information sent to the indexer for indexing.  An {@code IndexDTO} forms a connected graph
 * between an event, the DiSCOs referenced by the event, and the agent responsible for the event.
 * <p>
 * Indexing is event-driven.  Events are considered to have a source and target.  The objects referenced by the source
 * and target can be considered as input and output, respectively, to the event.  For example, the source of an
 * {@link info.rmapproject.core.model.event.RMapEventType#UPDATE UPDATE event} is the DiSCO that was operated on by
 * the event, and the target is the DiSCO that was produced by the event.  Agents are associated with the {@code event},
 * and are either directly or indirectly responsible for the event's occurrence.
 * </p>
 * <p>
 * Non-null IRIs present in the {@link #getEvent() RMapEvent} <em>must</em> reference DiSCOs in this object.  For
 * example, if the {@code event}'s source IRI is {@code <http://example.com/disco/1>}, then the {@link
 * #getSourceDisco() source DiSCO} must be present, and have the same IRI.
 * </p>
 *
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class IndexDTO {

    private final RMapEvent event;
    private final RMapAgent agent;
    private final RMapDiSCO sourceDisco;
    private final RMapDiSCO targetDisco;
    private final RMapIri eventSourceIri;
    private final RMapIri eventTargetIri;

    /**
     * Constructs a connected graph of objects to be indexed.
     * <p>
     * The {@code event} and {@code agent} must not be {@code null}.  If the {@code event} references a DiSCO, then the
     * supplied DiSCO must not be {@code null}, and the IRI of the reference must be equal to the IRI of the DiSCO.
     * </p>
     * <p>
     * For example, if the {@code event} had a target IRI of {@code <http://example.org/disco/1>}, then
     * {@code targetDisco} <em>must not</em> be {@code null} and <em>must</em> have an IRI equal to
     * {@code <http://example.org/disco/1>}.  If the {@code event} has <em>no</em> source IRI, then the {@code
     * sourceDisco} may be {@code null}.
     * </p>
     * @param event the RMapEvent being indexed
     * @param agent the RMapAgent responsible for the event
     * @param sourceDisco the DiSCO referenced by the event's source IRI
     * @param targetDisco the DiSCO referenced by the event's target IRI
     * @throws IllegalArgumentException if {@code event} or {@code agent} are {@code null}, or if a reference is missing
     */
    public IndexDTO(RMapEvent event, RMapAgent agent, RMapDiSCO sourceDisco, RMapDiSCO targetDisco) {
        this.event = IndexUtils.assertNotNull(event);
        this.agent = IndexUtils.assertNotNull(agent);

        if (!irisEqual(event.getAssociatedAgent(), agent.getId())) {
            throw new IllegalArgumentException("Incorrect agent IRI: expected " + event.getAssociatedAgent() + ", but" +
                    " found " + agent.getId());
        }

        Optional<RMapIri> iri;

        if ((iri = findEventIri(event, IndexUtils.EventDirection.SOURCE)).isPresent()) {
            if (sourceDisco == null) {
                throw new IllegalArgumentException("Expected to find a source DiSCO with iri " +
                        iri.get().getStringValue() + ", but the source DiSCO was null.");
            }

            this.sourceDisco = sourceDisco;
            this.eventSourceIri = iri.get();

            if (!irisEqual(eventSourceIri, sourceDisco.getId())) {
                throw new IllegalStateException("Expected DiSCO IRI " + sourceDisco.getId().getStringValue() + " to " +
                        "be equal to event source IRI " + eventSourceIri.getStringValue());
            }
        } else {
            this.sourceDisco = null;
            this.eventSourceIri = null;
        }

        if ((iri = findEventIri(event, IndexUtils.EventDirection.TARGET)).isPresent()) {
            if (targetDisco == null) {
                throw new IllegalArgumentException("Expected to find a target DiSCO with iri " +
                        iri.get().getStringValue() + ", but the target DiSCO was null.");
            }

            this.targetDisco = targetDisco;
            this.eventTargetIri = iri.get();

            if (!irisEqual(eventTargetIri, targetDisco.getId())) {
                throw new IllegalStateException("Expected DiSCO IRI " + targetDisco.getId().getStringValue() + " to " +
                        "be equal to event target IRI " + eventTargetIri.getStringValue());
            }
        } else {
            this.targetDisco = null;
            this.eventTargetIri = null;
        }
    }

    public RMapEvent getEvent() {
        return event;
    }

    public RMapAgent getAgent() {
        return agent;
    }

    public RMapDiSCO getSourceDisco() {
        return sourceDisco;
    }

    public RMapDiSCO getTargetDisco() {
        return targetDisco;
    }

    public RMapIri getEventSourceIri() {
        return eventSourceIri;
    }

    public RMapIri getEventTargetIri() {
        return eventTargetIri;
    }

    @Override
    public String toString() {
        return "IndexDTO{" +
                "event=" + ((event != null) ? event.getId() : "null") +
                ", agent=" + ((agent != null) ? agent.getId() : "null") +
                ", sourceDisco=" + ((sourceDisco != null) ? sourceDisco.getId() : "null") +
                ", targetDisco=" + ((targetDisco != null) ? targetDisco.getId() : "null") +
                ", eventSourceIri=" + ((eventSourceIri != null) ? eventSourceIri.getIri() : "null") +
                ", eventTargetIri=" + ((eventTargetIri != null) ? eventTargetIri.getIri() : "null") +
                '}';
    }
}
