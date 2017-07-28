package org.springframework.data.solr.core;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.springframework.data.solr.core.convert.SolrConverter;
import org.springframework.data.solr.server.SolrClientFactory;

import java.util.Collections;
import java.util.List;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class Solr6xxTemplate extends SolrTemplate {

    public Solr6xxTemplate(SolrClient solrClient) {
        super(solrClient);
    }

    /**
     * Use a naive factory instead of {@link org.springframework.data.solr.server.support.HttpSolrClientFactory}.
     * Prevents munging of the base Solr URL by appending a core when it isn't needed.
     *
     * @param solrClient
     * @param core
     */
    public Solr6xxTemplate(SolrClient solrClient, String core) {
        this(new SolrClientFactory() {
            @Override
            public SolrClient getSolrClient() {
                return solrClient;
            }

            @Override
            public SolrClient getSolrClient(String core) {
                return solrClient;
            }

            @Override
            public List<String> getCores() {
                return Collections.singletonList(core);
            }
        });

        // set the core as the super constructor does
        setSolrCore(core);
    }

    /**
     * Use a naive factory instead of {@link org.springframework.data.solr.server.support.HttpSolrClientFactory}.
     * Prevents munging of the base Solr URL by appending a core when it isn't needed.
     *
     * @param solrClient
     * @param core
     * @param requestMethod
     */
    public Solr6xxTemplate(SolrClient solrClient, String core, RequestMethod requestMethod) {
        this(new SolrClientFactory() {
            @Override
            public SolrClient getSolrClient() {
                return solrClient;
            }

            @Override
            public SolrClient getSolrClient(String core) {
                return solrClient;
            }

            @Override
            public List<String> getCores() {
                return Collections.singletonList(core);
            }
        }, requestMethod);

        // set the core as the super constructor does
        setSolrCore(core);
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
