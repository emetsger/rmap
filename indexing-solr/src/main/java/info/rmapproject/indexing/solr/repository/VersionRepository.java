package info.rmapproject.indexing.solr.repository;

import info.rmapproject.indexing.solr.model.DiscoVersionDocument;
import org.springframework.data.solr.repository.SolrCrudRepository;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public interface VersionRepository extends SolrCrudRepository<DiscoVersionDocument, Long> {

}
