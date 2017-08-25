package info.rmapproject.indexing.solr.repository;

import info.rmapproject.core.model.RMapIri;
import info.rmapproject.core.model.RMapObjectType;
import info.rmapproject.core.model.agent.RMapAgent;
import info.rmapproject.core.model.disco.RMapDiSCO;
import info.rmapproject.core.model.event.RMapEvent;
import info.rmapproject.core.model.event.RMapEventDerivation;
import info.rmapproject.core.model.event.RMapEventUpdate;
import info.rmapproject.core.model.event.RMapEventWithNewObjects;
import info.rmapproject.core.rdfhandler.RDFHandler;
import info.rmapproject.indexing.solr.AbstractSpringIndexingTest;
import info.rmapproject.indexing.solr.IndexUtils;
import info.rmapproject.indexing.solr.TestUtils;
import info.rmapproject.indexing.solr.model.DiscoSolrDocument;
import info.rmapproject.indexing.solr.model.DiscoVersionDocument;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.util.JavaBinCodec;
import org.apache.solr.common.util.NamedList;
import org.junit.Ignore;
import org.junit.Test;
import org.openrdf.rio.RDFFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.solr.core.DefaultQueryParser;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.PartialUpdate;
import org.springframework.data.solr.core.query.Query;
import org.springframework.lang.Nullable;

import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.lang.Long.parseLong;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
@Ignore("Move tests to ITs")
public class SimpleSolrTest extends AbstractSpringIndexingTest {

    @Autowired
    private SolrTemplate solrTemplate;

    @Autowired
    private DiscoRepository discoRepository;

    @Autowired
    private VersionRepository versionRepository;

    @Autowired
    RDFHandler rdfHandler;

//    @After
//    public void tearDown() throws Exception {
//        discoRepository.deleteAll();
//        assertEquals(0, discoRepository.count());
//    }

    /**
     * Fails: can't specify a core name to ping
     */
    @Test
    @Ignore("Fails: can't specify a core name to ping")
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
                .map(DiscoSolrDocument::getDocId)
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
                .filter(doc -> (parseLong(doc.getDocId()) > 199 && parseLong(doc.getDocId()) < 300))
                .collect(Collectors.toSet());

        assertEquals(3, filtered.size());
        assertTrue(filtered.stream().allMatch(doc -> parseLong(doc.getDocId()) >= 200 && parseLong(doc.getDocId()) < 203));
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
        assertEquals("300", found.iterator().next().getDocId());
        assertEquals("http://doi.org/10.1109/disco.test",
                found.iterator().next().getDiscoAggregatedResourceUris().iterator().next());

        discoRepository.deleteAll();

        // Store a disco document that has an upper-case resource url and try to find it with a lower case URL

        doc = new DiscoSolrDocument();
        doc.setDocId("301");
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
        assertEquals("301", found.iterator().next().getDocId());
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

        Map<RMapObjectType, Set<TestUtils.RDFResource>> rmapObjects = new HashMap<>();
        TestUtils.getRmapResources("/data/discos/rmd18mddcw", rdfHandler, RDFFormat.NQUADS, rmapObjects);

        List<RMapDiSCO> discos = TestUtils.getRmapObjects(rmapObjects, RMapObjectType.DISCO, rdfHandler);
        assertEquals(3, discos.size());

        List<RMapEvent> events = TestUtils.getRmapObjects(rmapObjects, RMapObjectType.EVENT, rdfHandler);
        assertEquals(3, events.size());

        List<RMapAgent> agents = TestUtils.getRmapObjects(rmapObjects, RMapObjectType.AGENT, rdfHandler);
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

                    doc.setDocId(String.valueOf(idCounter.getAndIncrement()));
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
    @SuppressWarnings("unchecked")
    public void javabinTest() throws Exception {
        try (InputStream in = this.getClass().getResourceAsStream("/javabin.out")) {
            NamedList<Object> response = (NamedList) new JavaBinCodec().unmarshal(in);
            assertNotNull(response);
        }
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
        doc.setDocId(id);
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

}
