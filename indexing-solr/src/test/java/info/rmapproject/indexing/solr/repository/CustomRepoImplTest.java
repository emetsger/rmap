package info.rmapproject.indexing.solr.repository;

import info.rmapproject.core.model.RMapIri;
import info.rmapproject.core.model.RMapStatus;
import info.rmapproject.core.model.disco.RMapDiSCO;
import info.rmapproject.core.rdfhandler.RDFHandler;
import info.rmapproject.indexing.solr.AbstractSpringIndexingTest;
import info.rmapproject.indexing.solr.IndexUtils;
import info.rmapproject.indexing.solr.TestResourceManager;
import info.rmapproject.indexing.solr.model.DiscoSolrDocument;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.openrdf.rio.RDFFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.PartialUpdate;
import org.springframework.data.solr.core.query.UpdateField;

import java.net.URI;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;

import static info.rmapproject.indexing.solr.IndexUtils.iae;
import static info.rmapproject.indexing.solr.model.DiscoSolrDocument.CORE_NAME;
import static info.rmapproject.indexing.solr.model.DiscoSolrDocument.DISCO_STATUS;
import static info.rmapproject.indexing.solr.model.DiscoSolrDocument.DOC_ID;
import static info.rmapproject.indexing.solr.model.DiscoSolrDocument.DOC_LAST_UPDATED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anySetOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class CustomRepoImplTest extends AbstractSpringIndexingTest {

    @Autowired
    private RDFHandler rdfHandler;

    @Autowired
    private IndexableThingMapper mapper;

    private CustomRepoImpl underTest = new CustomRepoImpl();

    private TestResourceManager rm;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        rm = TestResourceManager.load(
                "/data/discos/rmd18mddcw", RDFFormat.NQUADS, rdfHandler);
    }

    @Test
    public void testIndexCreate() throws Exception {
        
    }

    @Test
    public void testIndexUpdate() throws Exception {
        
    }

    @Test
    public void testIndexInactivate() throws Exception {

    }

    @Test
    public void testIndexTombstone() throws Exception {

    }

    @Test
    public void testIndexDelete() throws Exception {

    }

    @Test
    public void testIndexDerive() throws Exception {

    }

    @Test
    @SuppressWarnings("unchecked")
    public void updateDocumentStatusByDiscoIri() throws Exception {
        DiscoRepository mockRepository = mock(DiscoRepository.class);
        SolrTemplate mockTemplate = mock(SolrTemplate.class);
        underTest.setTemplate(mockTemplate);
        underTest.setDelegate(mockRepository);

        // We want to INACTIVATE all disco solr docs that contain the disco iri rmap:rmd18mddcw
        RMapDiSCO disco = rm.getDisco("rmap:rmd18mddcw");
        RMapStatus expectedStatus = RMapStatus.INACTIVE;

        // This is the response from the index when we search for solr documents with a uri of rmap:rmd18mddcw.
        // Note that the document returned from the index is ACTIVE.
        DiscoSolrDocument mockReponse = mockRepositoryResponse(RMapStatus.ACTIVE);
        when(mockRepository.findDiscoSolrDocumentsByDiscoUri(disco.getId().getStringValue()))
                .thenReturn(Collections.singleton(mockReponse));

        // Insure that the status is updated to INACTIVE
        when(mockTemplate.saveBean(eq(CORE_NAME), anySetOf(PartialUpdate.class)))
                .then((inv) -> {
                    // Insures that the PartialUpdate going to the index contains the correct value for the
                    // disco_status field, and that some value is present for doc_last_updated
                    Set<PartialUpdate> updates = inv.getArgumentAt(1, Set.class);
                    assertEquals(1, updates.size());
                    assertValueForUpdateField(
                            updates.stream().flatMap(update -> update.getUpdates().stream()),
                            DISCO_STATUS,
                            expectedStatus.toString());

                    assertValuePresenceForUpdateField(
                            updates.stream().flatMap(update -> update.getUpdates().stream()),
                            DOC_LAST_UPDATED);

                    return null;
                });

        underTest.updateDocumentStatusByDiscoIri(disco.getId(), expectedStatus, null);

        verify(mockRepository).findDiscoSolrDocumentsByDiscoUri(disco.getId().getStringValue());
        verify(mockTemplate).saveBeans(eq(CORE_NAME), anySetOf(PartialUpdate.class));
        verify(mockTemplate).commit(CORE_NAME);
    }

    private DiscoSolrDocument mockRepositoryResponse(RMapStatus status) {
        IndexableThing it = new IndexableThing();
        it.disco = rm.getDisco("rmap:rmd18mddcw");
        it.agent = rm.getAgent("rmap:rmd18m7mj4");
        it.event = rm.getEvent("rmap:rmd18m7msr");

        it.status = status;
        it.eventTarget = it.disco.getId();

        return mapper.apply(it);
    }

    private void assertValueForUpdateField(Stream<UpdateField> updateFields, String fieldName, String expectedValue) {
        String actualValue = (String)updateFields.filter(updateField -> updateField.getName().equals(fieldName))
                .findAny()
                .orElseThrow(iae("Did not find an update field for " + fieldName))
                .getValue();
        assertEquals(expectedValue, actualValue);
    }

    private void assertValuePresenceForUpdateField(Stream<UpdateField> updateFields, String fieldName) {
        String actualValue = (String)updateFields.filter(updateField -> updateField.getName().equals(fieldName))
                .findAny()
                .orElseThrow(iae("Did not find an update field for " + fieldName))
                .getValue();
        assertNotNull("Null value for update field " + fieldName, actualValue);
        assertTrue("Empty string for update field " + fieldName, actualValue.trim().length() > 0);
    }
}