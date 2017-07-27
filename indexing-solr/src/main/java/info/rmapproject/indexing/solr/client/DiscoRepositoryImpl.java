package info.rmapproject.indexing.solr.client;

import info.rmapproject.indexing.solr.model.DiscoSolrDocument;
import org.springframework.data.solr.core.SolrOperations;
import org.springframework.data.solr.repository.support.SimpleSolrRepository;

import java.net.URI;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class DiscoRepositoryImpl extends SimpleSolrRepository<DiscoSolrDocument, URI> {

//    public DiscoRepositoryImpl() {
//        super();
//    }
//
//    public DiscoRepositoryImpl(SolrOperations solrOperations) {
//        super(solrOperations);
//    }
//
//    public DiscoRepositoryImpl(SolrEntityInformation<DiscoSolrDocument, ?> metadata, SolrOperations solrOperations) {
//        super(metadata, solrOperations);
//    }

    public DiscoRepositoryImpl(SolrOperations solrOperations, Class<DiscoSolrDocument> entityClass) {
        super(solrOperations, entityClass);
    }

}
