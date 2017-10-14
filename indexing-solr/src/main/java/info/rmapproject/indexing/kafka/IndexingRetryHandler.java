package info.rmapproject.indexing.kafka;

import info.rmapproject.indexing.IndexingTimeoutException;
import info.rmapproject.indexing.solr.repository.IndexDTO;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
@FunctionalInterface
public interface IndexingRetryHandler {

    void retry(IndexDTO dto) throws IndexingTimeoutException;

}
