package info.rmapproject.indexing.solr;

import info.rmapproject.core.model.RMapObjectType;
import info.rmapproject.core.model.agent.RMapAgent;
import info.rmapproject.core.model.disco.RMapDiSCO;
import info.rmapproject.core.model.event.RMapEvent;
import info.rmapproject.core.rdfhandler.RDFHandler;
import org.openrdf.rio.RDFFormat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Provides easy access for loading RDF from classpath resources, deserializing the RDF to RMap objects, and accessing
 * instances of specific RMap objects by URI.
 *
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class TestResourceManager {

    private RDFHandler rdfHandler;

    private Map<RMapObjectType, Set<TestUtils.RDFResource>> rmapObjects;

    /**
     * Constructs an instance of the manager with the supplied RDFHandler and RDFResources.
     *
     * @param rdfHandler
     * @param rmapObjects
     */
    private TestResourceManager(RDFHandler rdfHandler, Map<RMapObjectType, Set<TestUtils.RDFResource>> rmapObjects) {
        this.rdfHandler = rdfHandler;
        this.rmapObjects = rmapObjects;
    }

    /**
     * Obtain the agents that were loaded
     *
     * @return
     */
    public List<RMapAgent> getAgents() {
        return TestUtils.getRmapObjects(rmapObjects, RMapObjectType.AGENT, rdfHandler);
    }

    /**
     * Obtain the specified agent
     *
     * @param iri
     * @return
     * @throws RuntimeException if the event is not found
     */
    public RMapAgent getAgent(String iri) {
        return getAgents()
                .stream()
                .filter(a -> a.getId().getStringValue().equals(iri))
                .findAny()
                .orElseThrow(() -> new RuntimeException("Missing expected agent '" + iri + "'"));
    }

    /**
     * Obtain the events that were loaded
     *
     * @return
     */
    public List<RMapEvent> getEvents() {
        return TestUtils.getRmapObjects(rmapObjects, RMapObjectType.EVENT, rdfHandler);
    }

    /**
     * Obtain the specified event
     *
     * @param iri
     * @return
     * * @throws RuntimeException if the event is not found
     */
    public RMapEvent getEvent(String iri) {
        return getEvents()
                .stream()
                .filter(e -> e.getId().getStringValue().equals(iri))
                .findAny()
                .orElseThrow(() -> new RuntimeException("Missing expected event '" + iri + "'"));
    }

    /**
     * Obtain the discos that were loaded
     *
     * @return
     */
    public List<RMapDiSCO> getDiscos() {
        return TestUtils.getRmapObjects(rmapObjects, RMapObjectType.DISCO, rdfHandler);
    }

    /**
     * Obtain the specified disco
     *
     * @param iri
     * @return
     * @throws RuntimeException if the disco is not found
     */
    public RMapDiSCO getDisco(String iri) {
        return getDiscos()
                .stream()
                .filter(d -> d.getId().getStringValue().equals(iri))
                .findAny()
                .orElseThrow(() -> new RuntimeException("Missing expected disco '" + iri + "'"));
    }

    /**
     * Load RDF resources from the filesystem and returns an instance of this class which provides access to the
     * loaded resources.
     * <p>
     * Assumes {@code resourcePath} specifies a directory containing files that contain DiSCOs, Agents, and
     * Events.  Each file must contain a single DiSCO, or single Event, or single Agent as retrieved from the
     * RMap HTTP API in the specified {@code format}.
     * </p>
     *
     * @param resourcePath
     * @param format
     * @param rdfHandler
     * @return
     */
    public static TestResourceManager load(String resourcePath, RDFFormat format, RDFHandler rdfHandler) {
        Map<RMapObjectType, Set<TestUtils.RDFResource>> rmapObjects = new HashMap<>();
        TestUtils.getRmapResources(resourcePath, rdfHandler, format, rmapObjects);

        TestResourceManager instance = new TestResourceManager(rdfHandler, rmapObjects);

        return instance;
    }


}
