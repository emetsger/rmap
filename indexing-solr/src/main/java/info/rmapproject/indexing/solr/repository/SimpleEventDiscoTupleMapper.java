package info.rmapproject.indexing.solr.repository;

import info.rmapproject.indexing.solr.model.DiscoSolrDocument;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;

import static info.rmapproject.indexing.solr.IndexUtils.assertNotNull;
import static info.rmapproject.indexing.solr.IndexUtils.ise;
import static info.rmapproject.indexing.solr.IndexUtils.notNull;

/**
 * Maps {@link EventDiscoTuple} instances to {@link info.rmapproject.indexing.solr.model.DiscoSolrDocument}s.
 *
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
class SimpleEventDiscoTupleMapper implements EventDiscoTupleMapper {

    @Autowired
    private DiscoMapper discoMapper;

    @Autowired
    private AgentMapper agentMapper;

    @Autowired
    private EventMapper eventMapper;

    /**
     * Constructs a {@code SimpleEventDiscoTupleMapper}.  The caller must set a
     * {@link #setDiscoMapper(DiscoMapper) DiscoMapper}, {@link #setAgentMapper(AgentMapper) AgentMapper}, and an
     * {@link #setEventMapper(EventMapper) EventMapper}.
     */
    public SimpleEventDiscoTupleMapper() {

    }

    /**
     * Constructs a {@code SimpleEventDiscoTupleManager} with the supplied mappers.
     *
     * @param discoMapper the disco mapper, must not be {@code null}
     * @param agentMapper the agent mapper, must not be {@code null}
     * @param eventMapper the event mapper, must not be {@code null}
     * @throws IllegalArgumentException if any mapper is {@code null}
     */
    public SimpleEventDiscoTupleMapper(DiscoMapper discoMapper, AgentMapper agentMapper, EventMapper eventMapper) {
        assertNotNull(discoMapper, "Disco Mapper must not be null.");
        assertNotNull(agentMapper, "Agent Mapper must not be null.");
        assertNotNull(eventMapper, "Event Mapper must not be null.");

        this.discoMapper = discoMapper;
        this.agentMapper = agentMapper;
        this.eventMapper = eventMapper;
    }

    /**
     * Maps a {@link EventDiscoTuple} to a {@link DiscoSolrDocument} field by field.  The mapping logic is tolerant with
     * respect to {@code null} values; no field validation is performed.  Any validation logic ought to exist outside of
     * this method.
     *
     * @param eventDiscoTuple the thing to be indexed
     * @return the {@code DiscoSolrDocument}
     * @throws IllegalArgumentException if the supplied {@code eventDiscoTuple} is {@code null}
     * @throws IllegalStateException if any of the mappers are null
     */
    @Override
    public DiscoSolrDocument apply(EventDiscoTuple eventDiscoTuple) {
        assertNotNull(eventDiscoTuple, "The supplied object to index must not be null.");
        assertNotNull(discoMapper, ise("The DiscoMapper must not be null."));
        assertNotNull(eventMapper, ise("The EventMapper must not be null."));
        assertNotNull(agentMapper, ise("The AgentMapper must not be null."));

        DiscoSolrDocument doc = new DiscoSolrDocument();

        // Fields mapped from EventDiscoTuple.disco

        if (notNull(eventDiscoTuple.disco)) {
            doc = discoMapper.apply(eventDiscoTuple.disco, doc);
        }

        // Fields mapped from EventDiscoTuple.status

        if (notNull(eventDiscoTuple.status)) {
            doc.setDiscoStatus(eventDiscoTuple.status.toString());
        }

        // Fields mapped from EventDiscoTuple.agent

        if (notNull(eventDiscoTuple.agent)) {
            doc = agentMapper.apply(eventDiscoTuple.agent, doc);
        }

        // Fields mapped from EventDiscoTuple.event

        if (notNull(eventDiscoTuple.event)) {

            doc = eventMapper.apply(eventDiscoTuple.event, doc);

            if (notNull(eventDiscoTuple.eventTarget)) {
                doc.setEventTargetObjectUris(Collections.singletonList(eventDiscoTuple.eventTarget.getStringValue()));
            }
            if (notNull(eventDiscoTuple.eventSource)) {
                doc.setEventSourceObjectUris(Collections.singletonList(eventDiscoTuple.eventSource.getStringValue()));
            }
        }

        return doc;
    }

    /**
     * Maps a DiSCO to Solr fields.
     *
     * @return
     */
    DiscoMapper getDiscoMapper() {
        return discoMapper;
    }

    /**
     * Maps a DiSCO to Solr fields.
     *
     * @param discoMapper
     */
    void setDiscoMapper(DiscoMapper discoMapper) {
        this.discoMapper = discoMapper;
    }

    /**
     * Maps an Agent to Solr fields.
     *
     * @return
     */
    AgentMapper getAgentMapper() {
        return agentMapper;
    }

    /**
     * Maps an Agent to Solr fields.
     *
     * @param agentMapper
     */
    void setAgentMapper(AgentMapper agentMapper) {
        this.agentMapper = agentMapper;
    }

    /**
     * Maps an Event to Solr fields
     *
     * @return
     */
    EventMapper getEventMapper() {
        return eventMapper;
    }

    /**
     * Maps an Event to Solr fields
     *
     * @param eventMapper
     */
    void setEventMapper(EventMapper eventMapper) {
        this.eventMapper = eventMapper;
    }
}
