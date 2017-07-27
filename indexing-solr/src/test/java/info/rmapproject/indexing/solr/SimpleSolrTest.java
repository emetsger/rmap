package info.rmapproject.indexing.solr;

import info.rmapproject.indexing.solr.model.DiscoSolrDocument;
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
    @Qualifier("discos")
    private SolrTemplate solrTemplate;

    @Test
    public void testPing() throws Exception {
        SolrPingResponse res = solrTemplate.ping();
        assertNotNull(res);
        assertTrue(res.getElapsedTime() > 0);
        assertEquals(0, res.getStatus());
    }

    @Test
    @Ignore
    public void testMod() throws Exception {
        System.err.println(2 % 2);
        System.err.println(0 % 2);
    }

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

        UpdateResponse res = solrTemplate.saveBean(doc);
        assertNotNull(res);

        solrTemplate.commit("discos");
    }
}
