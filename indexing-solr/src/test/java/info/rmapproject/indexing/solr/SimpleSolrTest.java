package info.rmapproject.indexing.solr;

import info.rmapproject.indexing.solr.model.DiscoSolrDocument;
import info.rmapproject.indexing.solr.model.DiscoVersionDocument;
import info.rmapproject.indexing.solr.repository.DiscoRepository;
import info.rmapproject.indexing.solr.repository.VersionRepository;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.solr.core.DefaultQueryParser;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.PartialUpdate;
import org.springframework.data.solr.core.query.Query;
import org.springframework.lang.Nullable;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:/rmap-indexing-solr.xml")
public class SimpleSolrTest {

    @Autowired
    private SolrTemplate solrTemplate;

    @Autowired
    private DiscoRepository discoRepository;

    @Autowired
    private VersionRepository versionRepository;

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
        DiscoSolrDocument doc = discoDocument(1, "simpleWriteWithTemplate");

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
        DiscoSolrDocument doc = discoDocument(10, "simpleWriteWithRepo");

        DiscoSolrDocument saved = discoRepository.save(doc);
        assertNotNull(saved);
    }

    @Test
    public void simpleCountAndDelete() throws Exception {
        DiscoSolrDocument doc = discoDocument(20, "simpleCountAndDelete");

        DiscoSolrDocument saved = discoRepository.save(doc);
        assertNotNull(saved);

        assertTrue(discoRepository.count() > 0);

        discoRepository.deleteAll();

        assertEquals(0, discoRepository.count());
    }

    @Test
    public void testSimpleFindUsingDocumentId() throws Exception {
        List<Long> ids = Arrays.asList(100L, 101L, 102L);
        ids.stream().map(id -> discoDocument(id, "testSimpleSearchUsingDocumentId"))
                .forEach(doc -> discoRepository.save(doc));

        Set<Long> found = StreamSupport.stream(
                discoRepository.findAllById(ids).spliterator(), false)
                .map(DiscoSolrDocument::getDiscoId)
                .collect(Collectors.toSet());

        ids.forEach(expectedId -> assertTrue(found.stream().anyMatch(expectedId::equals)));
        assertTrue(found.stream().noneMatch(id -> (id < 100 || id > 102)));
    }

    @Test
    public void testSimpleFindUsingCriteria() throws Exception {
        registerUriConverter(solrTemplate);
        List<Long> ids = Arrays.asList(200L, 201L, 202L);
        ids.stream().map(id -> discoDocument(id, "testSimpleFindUsingCriteria"))
                .forEach(doc -> discoRepository.save(doc));

        Set<DiscoSolrDocument> found = discoRepository
                .findDiscoSolrDocumentsByDiscoAggregatedResourceUris(URI.create("http://doi.org/10.1109/disco.test"));

        assertNotNull(found);

        Set<DiscoSolrDocument> filtered = found.stream()
                .filter(doc -> (doc.getDiscoId() > 199 && doc.getDiscoId() < 300))
                .collect(Collectors.toSet());

        assertEquals(3, filtered.size());
        assertTrue(filtered.stream().allMatch(doc -> doc.getDiscoId() >= 200 && doc.getDiscoId() < 203));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCaseInsensitiveUriSearch() throws Exception {
        registerUriConverter(solrTemplate);
        discoRepository.deleteAll();

        // Store a disco document that has a lower-case resource url and try to find it with an upper case URL

        DiscoSolrDocument doc = discoDocument(300L, "testCaseInsensitiveUriSearch");
        assertEquals("http://doi.org/10.1109/disco.test",
                doc.getDiscoAggregatedResourceUris().iterator().next());
        discoRepository.save(doc);

        Set<DiscoSolrDocument> found = discoRepository
                .findDiscoSolrDocumentsByDiscoAggregatedResourceUris(URI.create("http://DOI.ORG/10.1109/disco.test"));

        assertNotNull(found);

        assertEquals(1, found.size());
        assertEquals((Long)300L, found.iterator().next().getDiscoId());
        assertEquals("http://doi.org/10.1109/disco.test",
                found.iterator().next().getDiscoAggregatedResourceUris().iterator().next());

        discoRepository.deleteAll();

        // Store a disco document that has an upper-case resource url and try to find it with a lower case URL

        doc = new DiscoSolrDocument();
        doc.setDiscoId(301L);
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
        assertEquals((Long)301L, found.iterator().next().getDiscoId());
        assertEquals("http://DOI.ORG/10.1109/disco.test",
                found.iterator().next().getDiscoAggregatedResourceUris().iterator().next());

    }

    @Test
    public void testWildcardUriSearch() throws Exception {
        discoRepository.deleteAll();

        // Store a disco document that has a field containing a URL, and see if we can retrieve that document using
        // a wildcard search

        DiscoSolrDocument doc = discoDocument(400L, "testWildcardUriSearch");
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
    private static DiscoSolrDocument discoDocument(long id, String testDescription) {
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

}
