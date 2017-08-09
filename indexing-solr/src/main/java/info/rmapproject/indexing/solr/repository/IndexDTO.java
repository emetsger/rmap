package info.rmapproject.indexing.solr.repository;

import info.rmapproject.core.model.agent.RMapAgent;
import info.rmapproject.core.model.disco.RMapDiSCO;
import info.rmapproject.core.model.event.RMapEvent;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class IndexDTO {

    private RMapEvent event;
    private RMapAgent agent;
    private RMapDiSCO sourceDisco;
    private RMapDiSCO targetDisco;

    public RMapEvent getEvent() {
        return event;
    }

    public void setEvent(RMapEvent event) {
        this.event = event;
    }

    public RMapAgent getAgent() {
        return agent;
    }

    public void setAgent(RMapAgent agent) {
        this.agent = agent;
    }

    public RMapDiSCO getSourceDisco() {
        return sourceDisco;
    }

    public void setSourceDisco(RMapDiSCO sourceDisco) {
        this.sourceDisco = sourceDisco;
    }

    public RMapDiSCO getTargetDisco() {
        return targetDisco;
    }

    public void setTargetDisco(RMapDiSCO targetDisco) {
        this.targetDisco = targetDisco;
    }
}
