package info.rmapproject.indexing.solr.repository;

import info.rmapproject.indexing.solr.IndexUtils;

import java.util.function.Function;
import java.util.stream.Stream;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public interface IndexDTOMapper extends Function<IndexDTO, Stream<IndexableThing>> {

    @Override
    Stream<IndexableThing> apply(IndexDTO indexDTO);

    default IndexableThing getSourceIndexableThing(IndexDTO indexDTO) {
        return apply(indexDTO)
                .filter(it -> it.eventSource != null && IndexUtils.irisEqual(it.eventSource, it.disco.getId()))
                .findAny()
                .orElseThrow(IndexUtils.ise("Missing source of event."));
    }

    default IndexableThing getTargetIndexableThing(IndexDTO indexDTO) {
        return apply(indexDTO)
                .filter(it -> it.eventTarget != null && IndexUtils.irisEqual(it.eventTarget, it.disco.getId()))
                .findAny()
                .orElseThrow(IndexUtils.ise("Missing target of event."));
    }


}
