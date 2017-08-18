package info.rmapproject.indexing.solr.repository;

import info.rmapproject.indexing.solr.IndexUtils;
import info.rmapproject.indexing.solr.model.DiscoSolrDocument;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URI;
import java.util.Collections;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import static info.rmapproject.indexing.solr.IndexUtils.assertNotNull;
import static info.rmapproject.indexing.solr.IndexUtils.dateToString;
import static info.rmapproject.indexing.solr.IndexUtils.notNull;
import static info.rmapproject.indexing.solr.repository.MappingUtils.tripleToString;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
class SimpleIndexableThingMapper implements IndexableThingMapper {

    @Autowired
    private DiscoMapper discoMapper;

    @Autowired
    private AgentMapper agentMapper;

    @Autowired
    private EventMapper eventMapper;

    public SimpleIndexableThingMapper() {

    }

    public SimpleIndexableThingMapper(DiscoMapper discoMapper, AgentMapper agentMapper, EventMapper eventMapper) {
        assertNotNull(discoMapper, "Disco Mapper must not be null.");
        assertNotNull(agentMapper, "Agent Mapper must not be null.");
        assertNotNull(eventMapper, "Event Mapper must not be null.");

        this.discoMapper = discoMapper;
        this.agentMapper = agentMapper;
        this.eventMapper = eventMapper;
    }

    /**
     * Maps a {@link IndexableThing} to a {@link DiscoSolrDocument} field by field.  The mapping logic is tolerant with
     * respect to {@code null} values; no field validation is performed.  Any validation logic ought to exist outside of
     * this method.
     *
     * @param indexableThing the thing to be indexed
     * @return the {@code DiscoSolrDocument}
     */
    @Override
    public DiscoSolrDocument apply(IndexableThing indexableThing) {
        assertNotNull(indexableThing, "The supplied object to index must not be null.");

        DiscoSolrDocument doc = new DiscoSolrDocument();

        // Fields mapped from indexableThing.disco

        if (notNull(indexableThing.disco)) {
            doc = discoMapper.apply(indexableThing.disco, doc);
        }

        // Fields mapped from indexableThing.status

        if (notNull(indexableThing.status)) {
            doc.setDiscoStatus(indexableThing.status.toString());
        }

        // Fields mapped from indexableThing.agent

        if (notNull(indexableThing.agent)) {
            doc = agentMapper.apply(indexableThing.agent, doc);
        }

        // Fields mapped from indexableThing.event

        if (notNull(indexableThing.event)) {

            doc = eventMapper.apply(indexableThing.event, doc);

            if (notNull(indexableThing.eventTarget)) {
                doc.setEventTargetObjectUris(Collections.singletonList(indexableThing.eventTarget.getStringValue()));
            }
            if (notNull(indexableThing.eventSource)) {
                doc.setEventSourceObjectUris(Collections.singletonList(indexableThing.eventSource.getStringValue()));
            }
        }

        return doc;
    }

    DiscoMapper getDiscoMapper() {
        return discoMapper;
    }

    void setDiscoMapper(DiscoMapper discoMapper) {
        this.discoMapper = discoMapper;
    }

    AgentMapper getAgentMapper() {
        return agentMapper;
    }

    void setAgentMapper(AgentMapper agentMapper) {
        this.agentMapper = agentMapper;
    }

    EventMapper getEventMapper() {
        return eventMapper;
    }

    void setEventMapper(EventMapper eventMapper) {
        this.eventMapper = eventMapper;
    }
}
