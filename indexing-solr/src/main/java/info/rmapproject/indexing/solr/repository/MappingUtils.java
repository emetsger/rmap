package info.rmapproject.indexing.solr.repository;

import info.rmapproject.core.model.RMapTriple;

import java.util.stream.Stream;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
class MappingUtils {

    static Stream<String> tripleToString(Stream<RMapTriple> triples) {
        return triples
                .map(t -> String.format(
                        "%s %s %s",
                        t.getSubject().getStringValue(),
                        t.getPredicate().getStringValue(),
                        t.getObject().getStringValue()));
    }
}
