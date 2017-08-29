package info.rmapproject.indexing.solr.repository;

import info.rmapproject.core.rdfhandler.RDFHandler;
import info.rmapproject.indexing.solr.AbstractSpringIndexingTest;
import info.rmapproject.indexing.solr.model.DiscoVersionDocument;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.PartialUpdate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


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

}
