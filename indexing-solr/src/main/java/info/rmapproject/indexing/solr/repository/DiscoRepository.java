package info.rmapproject.indexing.solr.repository;

import info.rmapproject.indexing.solr.model.DiscoSolrDocument;
import org.springframework.data.solr.repository.SolrCrudRepository;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public interface DiscoRepository extends SolrCrudRepository<DiscoSolrDocument, Long> {

}
