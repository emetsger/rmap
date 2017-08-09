package info.rmapproject.indexing.solr.repository;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public interface CustomRepo {

    void index(IndexDTO toIndex);

}
