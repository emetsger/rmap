package info.rmapproject.indexing.solr.repository;

import info.rmapproject.indexing.solr.model.DiscoSolrDocument;
import org.springframework.data.solr.repository.SolrCrudRepository;

import java.net.URI;
import java.util.Set;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public interface DiscoRepository extends SolrCrudRepository<DiscoSolrDocument, Long> {

    Set<DiscoSolrDocument> findDiscoSolrDocumentsByDiscoAggregatedResourceUris(URI discoAggregatedResourceUri);

    Set<DiscoSolrDocument> findDiscoSolrDocumentsByDiscoAggregatedResourceUrisContains(String uriSubstring);

}
