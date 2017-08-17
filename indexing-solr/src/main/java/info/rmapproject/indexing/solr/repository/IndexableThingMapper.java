package info.rmapproject.indexing.solr.repository;

import info.rmapproject.indexing.solr.model.DiscoSolrDocument;

import java.util.function.Function;

/**
 * Maps {@link IndexableThing} to {@link info.rmapproject.indexing.solr.model.DiscoSolrDocument}
 *
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
interface IndexableThingMapper extends Function<IndexableThing, DiscoSolrDocument> {

    @Override
    DiscoSolrDocument apply(IndexableThing indexableThing);

}
