package info.rmapproject.indexing.solr.repository;

import info.rmapproject.core.model.RMapTriple;
import info.rmapproject.core.rdfhandler.RDFHandler;
import info.rmapproject.core.rdfhandler.RDFType;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.List;
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

    static String triplesToRDF(List<RMapTriple> triples, RDFHandler rdfHandler, RDFType rdfType) {
        OutputStream out = rdfHandler.triples2Rdf(triples, rdfType);
        if (!(out instanceof ByteArrayOutputStream)) {
            throw new RuntimeException("Unexpected OutputStream sub-type.  Wanted " +
                    ByteArrayOutputStream.class.getName() + " but was: " + out.getClass().getName());
        }

        return new String(((ByteArrayOutputStream) out).toByteArray());
    }
}
