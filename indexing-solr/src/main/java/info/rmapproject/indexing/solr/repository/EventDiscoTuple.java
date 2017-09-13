package info.rmapproject.indexing.solr.repository;

import info.rmapproject.core.model.RMapIri;
import info.rmapproject.core.model.RMapStatus;
import info.rmapproject.core.model.agent.RMapAgent;
import info.rmapproject.core.model.disco.RMapDiSCO;
import info.rmapproject.core.model.event.RMapEvent;

/**
 * Decomposes an instance of {@link IndexDTO} into tuples keyed by the event and disco.
 * <p>
 * A {@code IndexDTO} instance will decompose to at least one (at most two) {@code EventDiscoTuple} instance; one
 * instance for the {@code (Event, Event Source)} and one instance for the {@code (Event, Event Target)}.  Event source
 * and targets are typically DiSCOs.  Practically speaking, each instance of a {@code EventDiscoTuple} is
 * represented by a Solr document in the index.
 */
class EventDiscoTuple {
    RMapEvent event;
    RMapDiSCO disco;
    RMapAgent agent;
    RMapStatus status;
    RMapIri eventSource;
    RMapIri eventTarget;
}
