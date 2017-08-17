package info.rmapproject.indexing.solr.repository;

import info.rmapproject.core.model.agent.RMapAgent;
import info.rmapproject.indexing.solr.IndexUtils;
import info.rmapproject.indexing.solr.model.DiscoSolrDocument;
import org.springframework.stereotype.Component;

import static info.rmapproject.indexing.solr.IndexUtils.notNull;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
@Component
class SimpleAgentMapper implements AgentMapper {

    @Override
    public DiscoSolrDocument apply(RMapAgent agent, DiscoSolrDocument doc) {
        IndexUtils.assertNotNull(agent, "Agent must not be null.");

        if (doc == null) {
            doc = new DiscoSolrDocument();
        }

        if (notNull(agent.getId())) {
            doc.setAgentUri(agent.getId().getStringValue());
        }
        if (notNull(agent.getName())) {
            doc.setAgentDescription(agent.getName().getStringValue());
        }
        if (notNull(agent.getIdProvider())) {
            doc.setAgentProviderUri(agent.getIdProvider().getStringValue());
        }
        // TODO? toIndex.agent.getAuthId()

        return doc;
    }

}
