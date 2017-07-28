package info.rmapproject.indexing.solr.client;

import info.rmapproject.indexing.solr.model.DiscoSolrDocument;
import info.rmapproject.indexing.solr.repository.DiscoRepository;
import org.springframework.data.solr.core.SolrOperations;
import org.springframework.data.solr.repository.support.SimpleSolrRepository;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class DiscoRepositoryImpl extends SimpleSolrRepository<DiscoSolrDocument, Long>
        implements DiscoRepository {

    public DiscoRepositoryImpl(SolrOperations solrOperations) {
        super(solrOperations);
    }

}
