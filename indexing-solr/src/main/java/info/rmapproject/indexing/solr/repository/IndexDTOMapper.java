package info.rmapproject.indexing.solr.repository;

import java.util.function.Function;
import java.util.stream.Stream;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public interface IndexDTOMapper extends Function<IndexDTO, Stream<IndexableThing>> {

    @Override
    Stream<IndexableThing> apply(IndexDTO indexDTO);

}
