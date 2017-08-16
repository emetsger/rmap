package info.rmapproject.indexing.solr.repository;

import info.rmapproject.core.model.RMapIri;
import info.rmapproject.core.model.RMapStatus;
import info.rmapproject.core.model.agent.RMapAgent;
import info.rmapproject.core.model.disco.RMapDiSCO;
import info.rmapproject.core.model.event.RMapEvent;

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
