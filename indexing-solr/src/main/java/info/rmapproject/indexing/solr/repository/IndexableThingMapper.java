package info.rmapproject.indexing.solr.repository;

import info.rmapproject.indexing.solr.model.DiscoSolrDocument;

/**
 * Maps {@link IndexableThing} to {@link info.rmapproject.indexing.solr.model.DiscoSolrDocument}
 *
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
@FunctionalInterface
interface IndexableThingMapper {

    DiscoSolrDocument map(IndexableThing indexableThing);

}
