package info.rmapproject.indexing.solr.repository;

import info.rmapproject.indexing.solr.model.DiscoSolrDocument;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.solr.core.SolrOperations;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.repository.SolrCrudRepository;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
@NoRepositoryBean // TODO autowiring deps
public interface DiscoRepository extends SolrCrudRepository<DiscoSolrDocument, Long> {

}
