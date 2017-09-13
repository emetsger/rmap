package info.rmapproject.indexing.solr.repository;

import info.rmapproject.indexing.solr.model.DiscoSolrDocument;

import java.util.function.Function;

/**
 * Maps {@link EventDiscoTuple} instances to {@link info.rmapproject.indexing.solr.model.DiscoSolrDocument}s.  Unlike
 * the {@link IndexDTOMapper}, a single {@code EventDiscoTuple} will map to a single {@code DiscoSolrDocument}.
 *
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
interface EventDiscoTupleMapper extends Function<EventDiscoTuple, DiscoSolrDocument> {

    /**
     * Converts a {@code EventDiscoTuple} to a {@code DiscoSolrDocument} for indexing.
     *
     * @param eventDiscoTuple the event disco tuple
     * @return the solr document
     */
    @Override
    DiscoSolrDocument apply(EventDiscoTuple eventDiscoTuple);

}
