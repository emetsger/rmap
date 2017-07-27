package org.springframework.data.solr.core;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.springframework.data.solr.core.convert.SolrConverter;
import org.springframework.data.solr.server.SolrClientFactory;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class Solr6xxTemplate extends SolrTemplate {

    public Solr6xxTemplate(SolrClient solrClient) {
        super(solrClient);
    }

    public Solr6xxTemplate(SolrClient solrClient, String core) {
        super(solrClient, core);
    }

    public Solr6xxTemplate(SolrClient solrClient, String core, RequestMethod requestMethod) {
        super(solrClient, core, requestMethod);
    }

    public Solr6xxTemplate(SolrClientFactory solrClientFactory) {
        super(solrClientFactory);
    }

    public Solr6xxTemplate(SolrClientFactory solrClientFactory, String defaultCore) {
        super(solrClientFactory, defaultCore);
    }

    public Solr6xxTemplate(SolrClientFactory solrClientFactory, RequestMethod requestMethod) {
        super(solrClientFactory, requestMethod);
    }

    public Solr6xxTemplate(SolrClientFactory solrClientFactory, SolrConverter solrConverter) {
        super(solrClientFactory, solrConverter);
    }

    public Solr6xxTemplate(SolrClientFactory solrClientFactory, SolrConverter solrConverter, RequestMethod defaultRequestMethod) {
        super(solrClientFactory, solrConverter, defaultRequestMethod);
    }

    @Override
    public SolrInputDocument convertBeanToSolrInputDocument(Object bean) {
        if (bean instanceof SolrInputDocument) {
            return (SolrInputDocument) bean;
        }

        SolrInputDocument document = new SolrInputDocument(new String[]{});
        getConverter().write(bean, document);
        return document;
    }
}
