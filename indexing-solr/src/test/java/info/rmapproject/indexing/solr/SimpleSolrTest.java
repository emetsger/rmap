package info.rmapproject.indexing.solr;

import info.rmapproject.indexing.solr.model.DiscoSolrDocument;
import info.rmapproject.indexing.solr.model.DiscoVersionDocument;
import info.rmapproject.indexing.solr.repository.DiscoRepository;
import info.rmapproject.indexing.solr.repository.VersionRepository;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
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
import java.util.Calendar;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
                .map(DiscoSolrDocument::getDisco_id)
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
                .filter(doc -> (doc.getDisco_id() > 199 && doc.getDisco_id() < 300))
                .collect(Collectors.toSet());

        assertEquals(3, filtered.size());
        assertTrue(filtered.stream().allMatch(doc -> doc.getDisco_id() >= 200 && doc.getDisco_id() < 203));
    }

    /**
     * Create a DiscoVersionDocument, then update it.  Verify the updated document can be retrieved from the index.
     *
     * @throws Exception
     */
    @Test
    public void writeVersionUsingRepo() throws Exception {
//        registerUriConverter(solrTemplate);

        versionRepository.deleteAll();
        assertEquals(0, versionRepository.count());

        // Create a document and store it in the index.

        DiscoVersionDocument doc = new DiscoVersionDocument.Builder()
                .id(1L)
                .discoUri(URI.create("http://doi.org/10.1109/disco/2"))
                .addPastUri(URI.create("http://doi.org/10.1109/disco/1"))
                .status("ACTIVE")
                .build();

        SolrInputDocument inDoc = solrTemplate.convertBeanToSolrInputDocument(doc);

        DiscoVersionDocument saved = versionRepository.save(doc);
        assertNotNull(saved);
        assertEquals(doc, saved);

        assertEquals(1, versionRepository.count());

        // Update the document and save it in the index.

        DiscoVersionDocument docWithUpdate = new DiscoVersionDocument.Builder(doc)
                .activeUri(URI.create("http://doi.org/10.1109/disco/3"))
                .build();

        DiscoVersionDocument updateResponse = versionRepository.save(docWithUpdate);
        assertNotNull(updateResponse);

        // Verify that the updated document can be retrieved

        final DiscoVersionDocument actual = versionRepository.findById(docWithUpdate.getVersion_id())
                .orElseThrow(() -> new RuntimeException("DiscoVersionDocument " + docWithUpdate.getVersion_id() + " not found in index."));
//        assertEquals(updateResponse, actual);

        assertEquals(updateResponse.getDiscoUri(), actual.getDiscoUri());

        // Update using Solr Atomic Updates (requires <updateLog/>)
//        PartialUpdate update = new PartialUpdate("version_id", updated.getVersion_id());
//        update.setValueOfField("disco_uri", updated.getDiscoUri());
//        update.setValueOfField("past_uris", updated.getPastUris());
//        update.setValueOfField("last_updated", updated.getLastUpdated());
//        UpdateResponse res = solrTemplate.saveBean("versions", update);
//
//        assertNotNull(res);
//        assertEquals(0, res.getStatus());
//        assertEquals(1, versionRepository.count());
//        solrTemplate.commit("versions");
//
//        Thread.sleep(10000);


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
        doc.setDisco_description(testDescription);
        doc.setDisco_id(id);
        doc.setDisco_uri(URI.create("http://rmapproject.org/disco/5678f"));
        doc.setDisco_creator_uri(URI.create("http://foaf.org/Elliot_Metsger"));
        doc.setDiscoAggregatedResourceUris(new ArrayList() {
            {
                add("http://doi.org/10.1109/disco.test");
                add("http://ieeexplore.ieee.org/example/000000-mm.zip");
            }
        });
        doc.setDisco_provenance_uri(URI.create("http://rmapproject.org/prov/5678"));
        doc.setDisco_related_statements(new ArrayList() {
            {
                add("TODO n3 triples");
            }
        });
        return doc;
    }

}
