package info.rmapproject.indexing.solr;

import info.rmapproject.core.model.RMapIri;
import info.rmapproject.core.model.RMapObject;
import info.rmapproject.core.model.RMapObjectType;
import info.rmapproject.core.rdfhandler.RDFHandler;
import info.rmapproject.core.rdfhandler.RDFType;
import info.rmapproject.core.rdfhandler.impl.openrdf.RioRDFHandler;
import org.openrdf.model.IRI;
import org.openrdf.model.Statement;
import org.openrdf.rio.RDFFormat;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.ReadableByteChannel;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class TestUtils {

    /**
     * Returns {@code true} if the supplied statement's predicate is
     * {@code http://www.w3.org/1999/02/22-rdf-syntax-ns#type}.
     *
     * @param s the statement
     * @return true if the statement represents an {@code rdf:type}
     */
    static boolean isRdfType(Statement s) {
        return s.getPredicate().stringValue().equals(RdfTypeIRI.INSTANCE.stringValue());
    }

    /**
     * Retrieve a listing of Spring {@code Resource} objects that contain RMap objects.
     * <p>
     * Assumes {@code resourcePath} specifies a directory containing files that contain DiSCOs, Agents, and
     * Events.  Each file must contain a single DiSCO, or single Event, or single Agent as retrieved from the
     * RMap HTTP API.  The supplied {@code Map} is populated by this method.
     * </p>
     * <p>
     * The returned map allows the caller to obtain resources that contain the RMap object type of interest.  For
     * example, the resources containing DiSCOs are stored under the {@code RMapObjectType#DISCO} key.
     * </p>
     *
     * @param resourcePath a classpath resource that is expected to resolve to a directory on the file system
     * @param format the RDF format of the resources in the directory
     * @param rmapObjects a {@code Map} of Spring resources keyed by the type of RMap objects they contain
     */
    static void getRmapResources(String resourcePath, RDFHandler rdfHandler, RDFFormat format, Map<RMapObjectType,
            Set<RDFResource>> rmapObjects) {

        URL base = SimpleSolrTest.class.getResource(resourcePath);
        assertNotNull("Base resource directory " + resourcePath + " does not exist, or cannot be resolved.",
                base);
        File baseDir = null;
        try {
            baseDir = new File(base.toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException("Could not convert " + base.toString() + " to a URI: " + e.getMessage(), e);
        }
        assertTrue("Directory " + baseDir + " does not exist.", baseDir.exists());
        assertTrue(baseDir + " must be a directory.", baseDir.isDirectory());

        RDFType type;

        if (format == RDFFormat.TURTLE) {
            type = RDFType.TURTLE;
        } else if (format == RDFFormat.NQUADS) {
            type = RDFType.NQUADS;
        } else {
            throw new RuntimeException("Unsupported RDFFormat: " + format);
        }

        //noinspection ConstantConditions
        Stream.of(baseDir.listFiles((dir, name) -> {
            if (format == RDFFormat.TURTLE) {
                return name.endsWith(".ttl");
            }

            if (format == RDFFormat.NQUADS) {
                return name.endsWith(".n4");
            }

            throw new RuntimeException("Unsupported RDFFormat: " + format);
        })).forEach(file -> {
            try (FileInputStream fin = new FileInputStream(file)) {

                // Extract all the RDF statements from the file
                Set<Statement> statements = ((RioRDFHandler) rdfHandler).convertRDFToStmtList(fin, type, "");

                // Filter the statements that have a predicate of rdf:type
                statements.stream().filter(TestUtils::isRdfType)

                        // Map the the object of the rdf:type statement to a URI
                        .map(s -> URI.create(s.getObject().stringValue()))

                        // Collect the following mapping to a Map<RMapObjectType, Resource>
                        // (i.e. RMap objects of type X are in file Y)
                        .collect(

                                Collectors.toMap(
                                        // Map the rdf:type URI to a RMapObjectType to use as a Map key
                                        uri -> RMapObjectType.getObjectType(new RMapIri(uri)),

                                        // Ignore the rdf:type URI, and simply create a new HashSet containing the file
                                        ignored -> {
                                            Set<RDFResource> resources = new HashSet<>(1);
                                            resources.add(new RDFResourceWrapper(new FileSystemResource(file), format));
                                            return resources;
                                        },

                                        // If there are any key conflicts, simply merge the sets
                                        (one, two) -> {
                                            one.addAll(two);
                                            return one;
                                        },

                                        // Put the resulting Map entry into the supplied Map
                                        () -> rmapObjects

                        ));

            } catch (IOException e) {
                fail("Failed reading turtle RDF from " + file + ": " + e.getMessage());
            }
        });
    }

    /**
     * Deserialize Turtle RDF from Spring Resources for the specified RMap object type.
     *
     * @param rmapObjects
     * @param desiredType
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    static <T extends RMapObject> List<T> getRmapObjects(Map<RMapObjectType, Set<RDFResource>> rmapObjects,
                                                         RMapObjectType desiredType, RDFHandler rdfHandler) {

        List<T> objects = (List<T>)rmapObjects.keySet().stream().filter(candidate -> candidate == desiredType)
                .flatMap(type -> rmapObjects.get(type).stream())
                .map(resource -> {
                    try {
                        switch (desiredType) {
                            case DISCO:
                                return rdfHandler.rdf2RMapDiSCO(resource.getInputStream(),
                                        resource.getRmapFormat(), "");
                            case AGENT:
                                return rdfHandler.rdf2RMapAgent(resource.getInputStream(),
                                        resource.getRmapFormat(), "");
                            case EVENT:
                                return rdfHandler.rdf2RMapEvent(resource.getInputStream(),
                                        resource.getRmapFormat(), "");
                            default:
                                throw new IllegalArgumentException("Unknown type " + desiredType);
                        }
                    } catch (IOException e) {
                        fail("Error opening RDF resource " + resource + ": " + e.getMessage());
                        return null;
                    }

                }).collect(Collectors.toList());

        return objects;
    }

    /**
     * A Spring Resource of RDF content. A RDFResource exposes the RDF serialization of the RDF.
     */
    interface RDFResource extends Resource {

        /**
         * The format of the resource, using the OpenRDF model.  Equivalent to {@link #getRmapFormat()}.
         *
         * @return the format of the resource in the OpenRDF model
         */
        RDFFormat getRdfFormat();

        /**
         * The format of the resource, using the RMap model.  Equivalent to {@link #getRdfFormat()}.
         *
         * @return the format of the resource in the RMap model
         */
        RDFType getRmapFormat();
    }

    private static class RdfTypeIRI implements IRI {

        static RdfTypeIRI INSTANCE = new RdfTypeIRI();

        private RdfTypeIRI() {

        }

        @Override
        public String getNamespace() {
            return "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
        }

        @Override
        public String getLocalName() {
            return "type";
        }

        @Override
        public String stringValue() {
            return "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";
        }
    }

    /**
     * Wraps a Spring {@code Resource}, retaining knowledge of the RDF serialization of the resource.
     */
    static class RDFResourceWrapper implements RDFResource {

        /**
         * The underlying Spring {@code Resource}
         */
        private Resource delegate;

        /**
         * The OpenRDF RDF format
         */
        private RDFFormat format;

        public RDFResourceWrapper(Resource delegate, RDFFormat format) {
            this.delegate = delegate;
            this.format = format;
        }

        @Override
        public RDFFormat getRdfFormat() {
            return format;
        }

        @Override
        public RDFType getRmapFormat() {
            if (format == RDFFormat.TURTLE) {
                return RDFType.TURTLE;
            }

            if (format == RDFFormat.NQUADS) {
                return RDFType.NQUADS;
            }

            throw new RuntimeException("Unsupported RDFFormat " + format);
        }

        @Override
        public boolean exists() {
            return delegate.exists();
        }

        @Override
        public boolean isReadable() {
            return delegate.isReadable();
        }

        @Override
        public boolean isOpen() {
            return delegate.isOpen();
        }

        @Override
        public boolean isFile() {
            return delegate.isFile();
        }

        @Override
        public URL getURL() throws IOException {
            return delegate.getURL();
        }

        @Override
        public URI getURI() throws IOException {
            return delegate.getURI();
        }

        @Override
        public File getFile() throws IOException {
            return delegate.getFile();
        }

        @Override
        public ReadableByteChannel readableChannel() throws IOException {
            return delegate.readableChannel();
        }

        @Override
        public long contentLength() throws IOException {
            return delegate.contentLength();
        }

        @Override
        public long lastModified() throws IOException {
            return delegate.lastModified();
        }

        @Override
        public Resource createRelative(String s) throws IOException {
            return delegate.createRelative(s);
        }

        @Override
        @Nullable
        public String getFilename() {
            return delegate.getFilename();
        }

        @Override
        public String getDescription() {
            return delegate.getDescription();
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return delegate.getInputStream();
        }
    }
}
