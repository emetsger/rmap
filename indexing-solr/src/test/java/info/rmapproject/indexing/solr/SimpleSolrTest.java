package info.rmapproject.indexing.solr;

import info.rmapproject.core.model.RMapIri;
import info.rmapproject.core.model.RMapObject;
import info.rmapproject.core.model.RMapObjectType;
import info.rmapproject.core.model.agent.RMapAgent;
import info.rmapproject.core.model.disco.RMapDiSCO;
import info.rmapproject.core.model.event.RMapEvent;
import info.rmapproject.core.model.event.RMapEventDerivation;
import info.rmapproject.core.model.event.RMapEventUpdate;
import info.rmapproject.core.model.event.RMapEventWithNewObjects;
import info.rmapproject.core.rdfhandler.RDFHandler;
import info.rmapproject.core.rdfhandler.RDFType;
import info.rmapproject.core.rdfhandler.impl.openrdf.RioRDFHandler;
import info.rmapproject.indexing.solr.model.DiscoSolrDocument;
import info.rmapproject.indexing.solr.model.DiscoVersionDocument;
import info.rmapproject.indexing.solr.repository.DiscoRepository;
import info.rmapproject.indexing.solr.repository.IndexDTO;
import info.rmapproject.indexing.solr.repository.VersionRepository;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openrdf.model.IRI;
import org.openrdf.model.Statement;
import org.openrdf.rio.RDFFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.solr.core.DefaultQueryParser;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.PartialUpdate;
import org.springframework.data.solr.core.query.Query;
import org.springframework.lang.Nullable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static info.rmapproject.core.model.RMapStatus.ACTIVE;
import static info.rmapproject.core.model.RMapStatus.INACTIVE;
import static java.lang.Long.parseLong;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ActiveProfiles({"default", "inmemory-triplestore"})
@ContextConfiguration({"classpath:/rmap-indexing-solr.xml", "classpath:/spring-rmapcore-context.xml"})
public class SimpleSolrTest {

    @Autowired
    private SolrTemplate solrTemplate;

    @Autowired
    private DiscoRepository discoRepository;

    @Autowired
    private VersionRepository versionRepository;

    @Autowired
    RDFHandler rdfHandler;

    @Before
    public void setUp() throws Exception {
        System.setProperty("spring.profiles.active", "default, inmemory-triplestore, inmemory-idservice");
    }

//    @After
//    public void tearDown() throws Exception {
//        discoRepository.deleteAll();
//        assertEquals(0, discoRepository.count());
//    }

    /**
     * Fails: can't specify a core name to ping
     */
    @Test
    public void testPing() throws Exception {
        SolrPingResponse res = solrTemplate.ping();
        assertNotNull(res);
        assertTrue(res.getElapsedTime() > 0);
        assertEquals(0, res.getStatus());
    }

    /**
     * Write a document using the SolrTemplate
     */
    @Test
    @SuppressWarnings("unchecked")
    public void simpleWrite() throws Exception {
        DiscoSolrDocument doc = discoDocument("1", "simpleWriteWithTemplate");

        // NOT TRUE WITH spring-data-solr 3: Don't need to use the saveBean(core, doc) method, because the document has the core as an annotation
        UpdateResponse res = solrTemplate.saveBean("discos", doc);
        assertNotNull(res);

        solrTemplate.commit("discos");
    }

    /**
     * Write a document using domain-specific DiscoRepository
     *
     * Fails on commit() with no core specified
     */
    @Test
    public void simpleWriteWithRepo() throws Exception {
        DiscoSolrDocument doc = discoDocument("10", "simpleWriteWithRepo");

        DiscoSolrDocument saved = discoRepository.save(doc);
        assertNotNull(saved);
    }

    @Test
    public void simpleCountAndDelete() throws Exception {
        DiscoSolrDocument doc = discoDocument("20", "simpleCountAndDelete");

        DiscoSolrDocument saved = discoRepository.save(doc);
        assertNotNull(saved);

        assertTrue(discoRepository.count() > 0);

        discoRepository.deleteAll();

        assertEquals(0, discoRepository.count());
    }

    @Test
    public void testSimpleFindUsingDocumentId() throws Exception {
        List<Long> ids = Arrays.asList(100L, 101L, 102L);
        ids.stream().map(String::valueOf).map(id -> discoDocument(id, "testSimpleSearchUsingDocumentId"))
                .forEach(doc -> discoRepository.save(doc));

        Set<String> found = StreamSupport.stream(
                discoRepository.findAllById(ids).spliterator(), false)
                .map(DiscoSolrDocument::getDiscoId)
                .collect(Collectors.toSet());

        ids.forEach(expectedId -> assertTrue(found.stream().map(Long::parseLong).anyMatch(expectedId::equals)));
    }

    @Test
    public void testSimpleFindUsingCriteria() throws Exception {
        discoRepository.deleteAll();
        registerUriConverter(solrTemplate);
        List<Long> ids = Arrays.asList(200L, 201L, 202L);
        ids.stream().map(String::valueOf).map(id -> discoDocument(id, "testSimpleFindUsingCriteria"))
                .forEach(doc -> discoRepository.save(doc));

        Set<DiscoSolrDocument> found = discoRepository
                .findDiscoSolrDocumentsByDiscoAggregatedResourceUris(URI.create("http://doi.org/10.1109/disco.test"));

        assertNotNull(found);

        Set<DiscoSolrDocument> filtered = found.stream()
                .filter(doc -> (parseLong(doc.getDiscoId()) > 199 && parseLong(doc.getDiscoId()) < 300))
                .collect(Collectors.toSet());

        assertEquals(3, filtered.size());
        assertTrue(filtered.stream().allMatch(doc -> parseLong(doc.getDiscoId()) >= 200 && parseLong(doc.getDiscoId()) < 203));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCaseInsensitiveUriSearch() throws Exception {
        registerUriConverter(solrTemplate);
        discoRepository.deleteAll();

        // Store a disco document that has a lower-case resource url and try to find it with an upper case URL

        DiscoSolrDocument doc = discoDocument("300", "testCaseInsensitiveUriSearch");
        assertEquals("http://doi.org/10.1109/disco.test",
                doc.getDiscoAggregatedResourceUris().iterator().next());
        discoRepository.save(doc);

        Set<DiscoSolrDocument> found = discoRepository
                .findDiscoSolrDocumentsByDiscoAggregatedResourceUris(URI.create("http://DOI.ORG/10.1109/disco.test"));

        assertNotNull(found);

        assertEquals(1, found.size());
        assertEquals("300", found.iterator().next().getDiscoId());
        assertEquals("http://doi.org/10.1109/disco.test",
                found.iterator().next().getDiscoAggregatedResourceUris().iterator().next());

        discoRepository.deleteAll();

        // Store a disco document that has an upper-case resource url and try to find it with a lower case URL

        doc = new DiscoSolrDocument();
        doc.setDiscoId("301");
        doc.setDiscoAggregatedResourceUris(new ArrayList() {
            {
                add("http://DOI.ORG/10.1109/disco.test");
            }
        });
        discoRepository.save(doc);



        found = discoRepository
                .findDiscoSolrDocumentsByDiscoAggregatedResourceUris(URI.create("http://doi.org/10.1109/disco.test"));

        assertNotNull(found);

        assertEquals(1, found.size());
        assertEquals("301", found.iterator().next().getDiscoId());
        assertEquals("http://DOI.ORG/10.1109/disco.test",
                found.iterator().next().getDiscoAggregatedResourceUris().iterator().next());

    }

    @Test
    public void testWildcardUriSearch() throws Exception {
        discoRepository.deleteAll();

        // Store a disco document that has a field containing a URL, and see if we can retrieve that document using
        // a wildcard search

        DiscoSolrDocument doc = discoDocument("400", "testWildcardUriSearch");
        discoRepository.save(doc);

        Set<DiscoSolrDocument> found = discoRepository.findDiscoSolrDocumentsByDiscoAggregatedResourceUrisContains("http://doi.org/10.1109/");
        assertEquals(1, found.size());

        found = discoRepository.findDiscoSolrDocumentsByDiscoAggregatedResourceUrisContains("10.1109");
        assertEquals(1, found.size());
    }

    /**
     * Create a DiscoVersionDocument, then update it.  Verify the updated document can be retrieved from the index.
     *
     * @throws Exception
     */
    @Test
    public void writeAndUpdateVersionUsingRepo() throws Exception {
        versionRepository.deleteAll();
        assertEquals(0, versionRepository.count());

        // Create a document and store it in the index.

        DiscoVersionDocument doc = new DiscoVersionDocument.Builder()
                .id(1L)
                .discoUri("http://doi.org/10.1109/disco/2")
                .addPastUri("http://doi.org/10.1109/disco/1")
                .status("ACTIVE")
                .build();

        DiscoVersionDocument saved = versionRepository.save(doc);
        assertNotNull(saved);
        assertEquals(doc, saved);

        assertEquals(1, versionRepository.count());

        // Update the document and save it in the index.

        DiscoVersionDocument docWithUpdate = new DiscoVersionDocument.Builder(doc)
                .activeUri("http://doi.org/10.1109/disco/3")
                .build();

        DiscoVersionDocument updateResponse = versionRepository.save(docWithUpdate);
        assertNotNull(updateResponse);

        // Verify that the updated document can be retrieved

        final DiscoVersionDocument actual = versionRepository.findById(docWithUpdate.getVersionId())
                .orElseThrow(() -> new RuntimeException("DiscoVersionDocument " + docWithUpdate.getVersionId() + " not found in index."));
        assertEquals(updateResponse, actual);
    }

    @Test
    public void writeAndUpdateVersionUsingPartialUpdate() throws Exception {

        DiscoVersionDocument doc = new DiscoVersionDocument.Builder()
                .id(200L)
                .discoUri("http://doi.org/10.1109/disco/2")
                .addPastUri("http://doi.org/10.1109/disco/1")
                .status("ACTIVE")
                .build();

        UpdateResponse res = solrTemplate.saveBean("versions", doc);
        assertNotNull(res);
        assertEquals(0, res.getStatus());
        solrTemplate.commit("versions");

        assertEquals(doc, solrTemplate.getById("versions", 200L, DiscoVersionDocument.class)
                .orElseThrow(() -> new RuntimeException(
                        "Expected to find a DiscoVersionDocument with ID " + doc.getVersionId() + " in the index.")));

        // Update using Solr Atomic Updates (requires <updateLog/>)
        PartialUpdate update = new PartialUpdate("version_id", doc.getVersionId());
        update.setValueOfField("disco_status", "INACTIVE");
        res = solrTemplate.saveBean("versions", update);
        assertNotNull(res);
        assertEquals(0, res.getStatus());
        solrTemplate.commit("versions");

        assertEquals("INACTIVE", solrTemplate.getById("versions", 200L, DiscoVersionDocument.class)
                .orElseThrow(() -> new RuntimeException(
                        "Expected to find a DiscoVersionDocument with ID " + doc.getVersionId() + " in the index."))
                .getDiscoStatus());
    }

    @Test
    public void indexDiscoRdf() throws Exception {
        discoRepository.deleteAll();
        assertEquals(0, discoRepository.count());

        AtomicInteger idCounter = new AtomicInteger(1);

        Map<RMapObjectType, Set<RDFResource>> rmapObjects = new HashMap<>();
        getRmapResources("/data/discos/rmd18mddcw", RDFFormat.NQUADS, rmapObjects);

        List<RMapDiSCO> discos = getRmapObjects(rmapObjects, RMapObjectType.DISCO);
        assertEquals(3, discos.size());

        List<RMapEvent> events = getRmapObjects(rmapObjects, RMapObjectType.EVENT);
        assertEquals(3, events.size());

        List<RMapAgent> agents = getRmapObjects(rmapObjects, RMapObjectType.AGENT);
        assertEquals(1, agents.size());

        events.stream()
                .flatMap(event -> {

                    final RMapIri agentIri = event.getAssociatedAgent();
                    final RMapAgent agent = agents.stream()
                            .filter(a -> a.getId().getStringValue().equals(agentIri.getStringValue()))
                            .findAny()
                            .orElseThrow(() ->
                                    new RuntimeException("Missing agent '" + agentIri + "' of event " + event));

                    RMapIri source;
                    RMapIri target;

                    switch (event.getEventType()) {
                        case CREATION:
                            source = null;
                            target = ((RMapEventWithNewObjects) event).getCreatedObjectIds().get(0);
                            break;
                        case UPDATE:
                            source = ((RMapEventUpdate) event).getInactivatedObjectId();
                            target = ((RMapEventUpdate) event).getDerivedObjectId();
                            break;
                        case DERIVATION:
                            source = ((RMapEventDerivation) event).getSourceObjectId();
                            target = ((RMapEventDerivation) event).getDerivedObjectId();
                            break;
                        default:
                            throw new RuntimeException("Unhandled event type " + event);
                    }

                    IndexableThing forSource = null;
                    // The source IRI will be null in the case of a creation event
                    if (source != null) {
                        forSource = new IndexableThing();

                        forSource.eventSource = source;
                        forSource.eventTarget = target;
                        forSource.event = event;
                        forSource.agent = agent;
                        forSource.disco = discos.stream()
                                .filter(d -> d.getId().getStringValue().equals(source.getStringValue()))
                                .findAny()
                                .orElseThrow(() ->
                                        new RuntimeException("Missing source '" + source + "' of event " + event));
                    }

                    // The target IRI should never be null
                    IndexableThing forTarget = new IndexableThing();

                    forTarget.eventSource = source;
                    forTarget.eventTarget = target;
                    forTarget.event = event;
                    forTarget.agent = agent;
                    forTarget.disco = discos.stream()
                            .filter(d -> d.getId().getStringValue().equals(target.getStringValue()))
                            .findAny()
                            .orElseThrow(() ->
                                    new RuntimeException("Missing target '" + target + "' of event " + event));


                    return Stream.of(Optional.ofNullable(forSource), Optional.of(forTarget));
                })

                .filter(Optional::isPresent)

                .map(Optional::get)

                .map(toIndex -> {

                    DiscoSolrDocument doc = new DiscoSolrDocument();

                    doc.setDiscoId(String.valueOf(idCounter.getAndIncrement()));
                    doc.setDiscoRelatedStatements(toIndex.disco.getRelatedStatements().stream().map(t -> t.getSubject().getStringValue() + " " + t.getPredicate().getStringValue() + " " + t.getObject().getStringValue()).collect(Collectors.toList()));
                    doc.setDiscoUri(toIndex.disco.getId().getStringValue());
                    doc.setDiscoCreatorUri(toIndex.disco.getCreator().getStringValue());               // TODO: Resolve creator and index creator properties?
                    doc.setDiscoAggregatedResourceUris(toIndex.disco.getAggregatedResources()
                            .stream().map(URI::toString).collect(Collectors.toList()));
                    doc.setDiscoDescription(toIndex.disco.getDescription().getStringValue());
                    doc.setDiscoProvenanceUri(toIndex.disco.getProvGeneratedBy() != null ? toIndex.disco.getProvGeneratedBy().getStringValue() : null);

                    doc.setAgentUri(toIndex.agent.getId().getStringValue());
                    doc.setAgentDescription(toIndex.agent.getName().getStringValue());
                    doc.setAgentProviderUri(toIndex.agent.getIdProvider().getStringValue());
                    // TODO? toIndex.agent.getAuthId()

                    doc.setEventUri(toIndex.event.getId().getStringValue());
                    doc.setEventAgentUri(toIndex.event.getAssociatedAgent().getStringValue());
                    doc.setEventDescription(toIndex.event.getDescription() != null ? toIndex.event.getDescription().getStringValue() : null);
                    doc.setEventStartTime(IndexUtils.dateToString(toIndex.event.getStartTime()));
                    doc.setEventEndTime(IndexUtils.dateToString(toIndex.event.getEndTime()));
                    doc.setEventType(toIndex.event.getEventType().name());
                    doc.setEventTargetObjectUris(Collections.singletonList(toIndex.eventTarget.getStringValue()));
                    if (toIndex.eventSource != null) {
                        doc.setEventSourceObjectUris(Collections.singletonList(toIndex.eventSource.getStringValue()));
                    }

                    return doc;
                })

                .forEach(doc -> discoRepository.save(doc));
    }

    @Test
    public void writableDiscoRepository() throws Exception {
        discoRepository.deleteAll();
        assertEquals(0, discoRepository.count());
        Stream<IndexDTO> dtos = prepareIndexableDtos("/data/discos/rmd18mddcw");

        dtos.forEach(dto -> discoRepository.index(dto));

        // 5 documents should have been added
        // - one document per DiSCO, Event tuple
        assertEquals(5, discoRepository.count());

        // 1 document should be active
        Set<DiscoSolrDocument> docs = discoRepository.findDiscoSolrDocumentsByDiscoStatus(ACTIVE.toString());
        assertEquals(1, docs.size());

        // assert it is the uri we expect
        DiscoSolrDocument active = docs.iterator().next();
        assertTrue(active.getDiscoUri().endsWith("rmd18mddcw"));

        // the other four should be inactive
        docs = discoRepository.findDiscoSolrDocumentsByDiscoStatus(INACTIVE.toString());
        assertEquals(4, discoRepository.count());

        // assert they have the uris we expect
        assertEquals(2, docs.stream().filter(doc -> doc.getDiscoUri().endsWith("rmd18mdd8b")).count());
        assertEquals(2, docs.stream().filter(doc -> doc.getDiscoUri().endsWith("rmd18m7mr7")).count());
    }

    private Stream<IndexDTO> prepareIndexableDtos(String resourcePath) {
        Map<RMapObjectType, Set<RDFResource>> rmapObjects = new HashMap<>();
        getRmapResources(resourcePath, RDFFormat.NQUADS, rmapObjects);

        List<RMapDiSCO> discos = getRmapObjects(rmapObjects, RMapObjectType.DISCO);
        assertEquals(3, discos.size());

        List<RMapEvent> events = getRmapObjects(rmapObjects, RMapObjectType.EVENT);
        assertEquals(3, events.size());

        List<RMapAgent> agents = getRmapObjects(rmapObjects, RMapObjectType.AGENT);
        assertEquals(1, agents.size());

        return events.stream()
                .sorted(Comparator.comparing(RMapEvent::getStartTime))
                .map(event -> {
            RMapAgent agent = agents.stream()
                    .filter(a -> a.getId().getStringValue().equals(event.getAssociatedAgent().getStringValue()))
                    .findAny()
                    .orElseThrow(() ->
                            new RuntimeException("Missing expected agent " + event.getAssociatedAgent().getStringValue()));

            RMapIri source;
            RMapIri target;

            switch (event.getEventType()) {
                case CREATION:
                    source = null;
                    target = ((RMapEventWithNewObjects) event).getCreatedObjectIds().get(0);
                    break;
                case UPDATE:
                    source = ((RMapEventUpdate) event).getInactivatedObjectId();
                    target = ((RMapEventUpdate) event).getDerivedObjectId();
                    break;
                case DERIVATION:
                    source = ((RMapEventDerivation) event).getSourceObjectId();
                    target = ((RMapEventDerivation) event).getDerivedObjectId();
                    break;
                default:
                    throw new RuntimeException("Unhandled event type " + event);
            }

            IndexDTO indexDto = new IndexDTO();
            indexDto.setAgent(agent);
            indexDto.setEvent(event);

            if (source != null) {
                indexDto.setSourceDisco(discos.stream()
                        .filter(disco -> disco.getId().getStringValue().equals(source.getStringValue()))
                        .findAny()
                        .orElseThrow(() ->
                                new RuntimeException("Missing expected source DiSCO: " + source.getStringValue())));
            }

            IndexUtils.assertNotNull(target);

            indexDto.setTargetDisco(discos.stream()
                    .filter(disco -> disco.getId().getStringValue().equals(target.getStringValue()))
                    .findAny()
                    .orElseThrow(() ->
                            new RuntimeException("Missing expected target DiSCO: " + target.getStringValue())));

            return indexDto;
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
    private <T extends RMapObject> List<T> getRmapObjects(Map<RMapObjectType, Set<RDFResource>> rmapObjects,
                                                          RMapObjectType desiredType) {

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
    private void getRmapResources(String resourcePath, RDFFormat format, Map<RMapObjectType,
            Set<RDFResource>> rmapObjects) {

        URL base = this.getClass().getResource(resourcePath);
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
                statements.stream().filter(SimpleSolrTest::isRdfType)

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
     * Returns {@code true} if the supplied statement's predicate is
     * {@code http://www.w3.org/1999/02/22-rdf-syntax-ns#type}.
     *
     * @param s the statement
     * @return true if the statement represents an {@code rdf:type}
     */
    private static boolean isRdfType(Statement s) {
        return s.getPredicate().stringValue().equals(RdfTypeIRI.INSTANCE.stringValue());
    }

    private static void registerUriConverter(SolrTemplate solrTemplate) {
        DefaultQueryParser queryParser = new DefaultQueryParser();
        queryParser.registerConverter(new Converter<URI, String>() {
            @Nullable
            @Override
            public String convert(URI uri) {
                if (uri == null) {
                    return null;
                }
                String converted = uri.toString().replaceAll(":", "\\\\:");
                System.err.println("Converted '" + uri + "' to '" + converted + "'");
                return converted;
            }
        });
        solrTemplate.registerQueryParser(Query.class, queryParser);
    }

    /**
     * Domain instance document
     * @param id
     * @param testDescription
     * @return
     */
    private static DiscoSolrDocument discoDocument(String id, String testDescription) {
        DiscoSolrDocument doc = new DiscoSolrDocument();
        doc.setDiscoDescription(testDescription);
        doc.setDiscoId(id);
        doc.setDiscoUri("http://rmapproject.org/disco/5678f");
        doc.setDiscoCreatorUri("http://foaf.org/Elliot_Metsger");
        doc.setDiscoAggregatedResourceUris(new ArrayList() {
            {
                add("http://doi.org/10.1109/disco.test");
                add("http://ieeexplore.ieee.org/example/000000-mm.zip");
            }
        });
        doc.setDiscoProvenanceUri("http://rmapproject.org/prov/5678");
        doc.setDiscoRelatedStatements(new ArrayList() {
            {
                add("TODO n3 triples");
            }
        });
        return doc;
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
     * Encapsulates an indexable unit.
     */
    private class IndexableThing {
        private RMapEvent event;
        private RMapDiSCO disco;
        private RMapAgent agent;
        private RMapIri eventSource;
        private RMapIri eventTarget;
    }

    /**
     * A Spring Resource of RDF content. A RDFResource exposes the RDF serialization of the RDF.
     */
    private interface RDFResource extends Resource {

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

    /**
     * Wraps a Spring {@code Resource}, retaining knowledge of the RDF serialization of the resource.
     */
    private class RDFResourceWrapper implements RDFResource {

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
