package info.rmapproject.indexing.solr;

import info.rmapproject.indexing.solr.model.DiscoSolrDocument;
import info.rmapproject.indexing.solr.repository.DiscoRepository;
import org.apache.solr.client.solrj.response.SolrPingResponse;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.net.URI;
import java.util.ArrayList;

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
    @Qualifier("solrTemplate")
    private SolrTemplate solrTemplateWithCore;

    @Autowired
    private DiscoRepository discoRepository;

    /**
     * Fails: can't specify a core name to ping
     */
    @Test
    @Ignore
    public void testPing() throws Exception {
        SolrPingResponse res = solrTemplateWithCore.ping();
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

        DiscoSolrDocument doc = new DiscoSolrDocument();
        doc.setDisco_id(5L);
        doc.setDisco_uri(URI.create("http://rmapproject.org/disco/1234"));
        doc.setDisco_creator_uri(URI.create("http://foaf.org/Elliot_Metsger"));
        doc.setDisco_aggregated_resource_uris(new ArrayList() {
            {
                add("http://doi.org/10.1109/disco.test");
                add("http://ieeexplore.ieee.org/example/000000-mm.zip");
            }
        });
        doc.setDisco_provenance_uri(URI.create("http://rmapproject.org/prov/1234"));
        doc.setDisco_related_statements(new ArrayList() {
            {
                add("TODO n3 triples");
            }
        });

        // Don't need to use the saveBean(core, doc) method, because the document has the core as an annotation
        UpdateResponse res = solrTemplateWithCore.saveBean(doc);
        assertNotNull(res);

        solrTemplateWithCore.commit("discos");
    }

    /**
     * Write a document using domain-specific DiscoRepository
     *
     * Fails on commit() with no core specified
     */
    @Test
    public void simpleWriteWithRepo() throws Exception {
        DiscoSolrDocument doc = new DiscoSolrDocument();
        doc.setDisco_id(10L);
        doc.setDisco_description("simpleWriteWithRepo");
        doc.setDisco_uri(URI.create("http://rmapproject.org/disco/5678f"));
        doc.setDisco_creator_uri(URI.create("http://foaf.org/Elliot_Metsger"));
        doc.setDisco_aggregated_resource_uris(new ArrayList() {
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

        DiscoSolrDocument saved = discoRepository.save(doc);
        assertNotNull(saved);
    }

    @Test
    public void simpleCountAndDelete() throws Exception {
        DiscoSolrDocument doc = new DiscoSolrDocument();
        doc.setDisco_id(20L);
        doc.setDisco_description("simpleCountAndDelete");
        doc.setDisco_uri(URI.create("http://rmapproject.org/disco/5678f"));
        doc.setDisco_creator_uri(URI.create("http://foaf.org/Elliot_Metsger"));
        doc.setDisco_aggregated_resource_uris(new ArrayList() {
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

        DiscoSolrDocument saved = discoRepository.save(doc);
        assertNotNull(saved);

        assertTrue(discoRepository.count() > 0);

        discoRepository.deleteAll();

        assertEquals(0, discoRepository.count());
    }
}
