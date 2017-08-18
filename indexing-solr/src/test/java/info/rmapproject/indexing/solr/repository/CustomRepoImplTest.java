package info.rmapproject.indexing.solr.repository;

import info.rmapproject.core.model.RMapStatus;
import info.rmapproject.core.model.disco.RMapDiSCO;
import info.rmapproject.indexing.solr.AbstractSpringIndexingTest;
import info.rmapproject.indexing.solr.TestResourceManager;
import info.rmapproject.indexing.solr.model.DiscoSolrDocument;
import org.junit.Before;
import org.junit.Test;
import org.openrdf.rio.RDFFormat;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.PartialUpdate;
import org.springframework.data.solr.core.query.UpdateField;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static info.rmapproject.indexing.solr.IndexUtils.iae;
import static info.rmapproject.indexing.solr.IndexUtils.ise;
import static info.rmapproject.indexing.solr.model.DiscoSolrDocument.CORE_NAME;
import static info.rmapproject.indexing.solr.model.DiscoSolrDocument.DISCO_STATUS;
import static info.rmapproject.indexing.solr.model.DiscoSolrDocument.DOC_LAST_UPDATED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anySetOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class CustomRepoImplTest extends AbstractSpringIndexingTest {

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
        Mocks mocks = new Mocks().build();

        rm = TestResourceManager.load(
                "/data/discos/rmd18mddcw", RDFFormat.NQUADS, rdfHandler);

        String discoIri = "rmap:rmd18m7mr7";
        IndexDTO dto = new IndexDTO(
                rm.getEvent("rmap:rmd18m7msr"),
                rm.getAgent("rmap:rmd18m7mj4"),
                null,
                rm.getDisco(discoIri));

        when(mocks.getMockRepository().save(any(DiscoSolrDocument.class))).then(
                (invocation) -> {
                    DiscoSolrDocument doc = invocation.getArgumentAt(0, DiscoSolrDocument.class);
                    assertEquals(discoIri, doc.getDiscoUri());
                    return null;
                });

        underTest.index(dto);

        verify(mocks.getMockRepository()).save(any(DiscoSolrDocument.class));
        verifyZeroInteractions(mocks.getMockTemplate());
    }

    @Test
    public void testIndexUpdate() throws Exception {
        Mocks mocks = new Mocks().build();

        rm = TestResourceManager.load(
                "/data/discos/rmd18mddcw", RDFFormat.NQUADS, rdfHandler);

        String sourceDiscoIri = "rmap:rmd18m7mr7";
        String targetDiscoIri = "rmap:rmd18mdd8b";
        IndexDTO dto = new IndexDTO(
                rm.getEvent("rmap:rmd18mdd9v"),
                rm.getAgent("rmap:rmd18m7mj4"),
                rm.getDisco(sourceDiscoIri),
                rm.getDisco(targetDiscoIri));

        Set<String> saved = new HashSet<>();

        when(mocks.getMockRepository().save(any(DiscoSolrDocument.class))).then(
                (invocation) -> {
                    DiscoSolrDocument doc = invocation.getArgumentAt(0, DiscoSolrDocument.class);
                    saved.add(doc.getDiscoUri());
                    return null;
                });

        DiscoSolrDocument mockResponse = mockRepositoryResponse2(mocks.getIndexDTOMapper().getSourceIndexableThing(dto), RMapStatus.ACTIVE, mocks);

        // This is the response from the index when we search for solr documents with a uri of rmap:rmd18m7mr7.
        // Note that the document returned from the index is ACTIVE.
        when(mocks.getMockRepository().findDiscoSolrDocumentsByDiscoUri(sourceDiscoIri))
                .thenReturn(Collections.singleton(mockResponse));

        // Insure that the status is updated to INACTIVE
        assertTemplateUpdatesStatus(mocks.getMockTemplate(), sourceDiscoIri, RMapStatus.INACTIVE);

        underTest.index(dto);

        verify(mocks.getMockRepository(), times(2)).save(any(DiscoSolrDocument.class));
        assertEquals(2, saved.size());
        assertTrue(saved.contains(sourceDiscoIri));
        assertTrue(saved.contains(targetDiscoIri));

        verify(mocks.getMockTemplate()).saveBeans(eq(CORE_NAME), anySetOf(DiscoPartialUpdate.class));
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
        Mocks mocks = new Mocks().build();
        DiscoRepository mockRepository = mocks.getMockRepository();
        SolrTemplate mockTemplate = mocks.getMockTemplate();

        // We want to INACTIVATE all disco solr docs that contain the disco iri rmap:rmd18mddcw
        RMapDiSCO disco = rm.getDisco("rmap:rmd18mddcw");
        RMapStatus expectedStatus = RMapStatus.INACTIVE;

        // This is the response from the index when we search for solr documents with a uri of rmap:rmd18mddcw.
        // Note that the document returned from the index is ACTIVE.
        DiscoSolrDocument mockReponse = mockRepositoryResponse(mocks, RMapStatus.ACTIVE);
        when(mockRepository.findDiscoSolrDocumentsByDiscoUri(disco.getId().getStringValue()))
                .thenReturn(Collections.singleton(mockReponse));

        // Insure that the status is updated to INACTIVE
        assertTemplateUpdatesStatus(mockTemplate, disco.getId().getStringValue(), expectedStatus);

        underTest.updateDocumentStatusByDiscoIri(disco.getId(), expectedStatus, null);

        verify(mockRepository).findDiscoSolrDocumentsByDiscoUri(disco.getId().getStringValue());
        verify(mockTemplate).saveBeans(eq(CORE_NAME), anySetOf(PartialUpdate.class));
        verify(mockTemplate).commit(CORE_NAME);
    }

    private void assertTemplateUpdatesStatus(SolrTemplate mockTemplate, String discoIri, RMapStatus expectedStatus) {
        when(mockTemplate.saveBeans(eq(CORE_NAME), anySetOf(DiscoPartialUpdate.class)))
                .then((inv) -> {
                    // Insures that the PartialUpdate going to the index contains the correct value for the
                    // disco_status field, and that some value is present for doc_last_updated
                    Set<DiscoPartialUpdate> updates = inv.getArgumentAt(1, Set.class);

                    DiscoPartialUpdate update = updates
                            .stream()
                            .filter(candidate -> candidate.getDiscoIri().equals(discoIri))
                            .findAny()
                            .orElseThrow(ise("Missing expected DiscoPartialUpdate for disco with iri " + discoIri));

                    assertValueForUpdateField(
                            update.getUpdates().stream(),
                            DISCO_STATUS,
                            expectedStatus.toString());

                    assertValuePresenceForUpdateField(
                            update.getUpdates().stream(),
                            DOC_LAST_UPDATED);

                    return null;
                });
    }

    private DiscoSolrDocument mockRepositoryResponse(Mocks mocks, RMapStatus status) {
        IndexableThing it = new IndexableThing();
        it.disco = rm.getDisco("rmap:rmd18mddcw");
        it.agent = rm.getAgent("rmap:rmd18m7mj4");
        it.event = rm.getEvent("rmap:rmd18m7msr");

        it.status = status;
        it.eventTarget = it.disco.getId();

        return mocks.getIndexableThingMapper().apply(it);
    }

    private DiscoSolrDocument mockRepositoryResponse2(IndexableThing it, RMapStatus status, Mocks mocks) {
        DiscoSolrDocument doc = mocks.getIndexableThingMapper().apply(it);
        doc.setDiscoStatus(status.toString());
        return doc;
    }

    private void assertValueForUpdateField(Stream<UpdateField> updateFields, String fieldName, String expectedValue) {
        String actualValue = (String)updateFields.filter(updateField -> updateField.getName().equals(fieldName))
                .findAny()
                .orElseThrow(iae("Did not find an update field for " + fieldName))
                .getValue();
        assertEquals(expectedValue, actualValue);
    }

    private void assertValuePresenceForUpdateField(Stream<UpdateField> updateFields, String fieldName) {
        Object actualValue = updateFields.filter(updateField -> updateField.getName().equals(fieldName))
                .findAny()
                .orElseThrow(iae("Did not find an update field for " + fieldName))
                .getValue();
        assertNotNull("Null value for update field " + fieldName, actualValue);
        if (actualValue instanceof String)
            assertTrue("Empty string for update field " + fieldName, ((String)actualValue).trim().length() > 0);
        if (actualValue instanceof Long)
            assertTrue("Uninitialized long for update field " + fieldName, ((Long) actualValue) > 0);
    }


    /**
     * A container for mock objects; invoking {@link #build()} will assemble the mocks and apply them on the
     * {@link #underTest CustomRepoImpl} that is under test.
     */
    private class Mocks {
        private DiscoRepository mockRepository;
        private SolrTemplate mockTemplate;
        private IndexableThingMapper indexableThingMapper;
        private IndexDTOMapper indexDTOMapper;

        /**
         * A mock instance of {@link DiscoRepository}, the high-level interface to the Solr index.
         *
         * @return
         */
        public DiscoRepository getMockRepository() {
            return mockRepository;
        }

        /**
         * A mock instance of {@link SolrTemplate}, the low-level interface to the Solr index.
         *
         * @return
         */
        public SolrTemplate getMockTemplate() {
            return mockTemplate;
        }

        /**
         * A concrete mapper which maps instances of {@link IndexableThing} to {@link DiscoSolrDocument} instances.
         *
         * @return
         */
        public IndexableThingMapper getIndexableThingMapper() {
            return indexableThingMapper;
        }

        /**
         * A concrete mapper which maps instances of {@link IndexDTO} to {@link IndexableThing} instances.
         *
         * @return
         */
        public IndexDTOMapper getIndexDTOMapper() {
            return indexDTOMapper;
        }

        /**
         * Instantiates concrete mappers and mock instances, and assembles them onto the {@link #underTest
         * CustomRepoImpl under test}.
         *
         * @return
         */
        public Mocks build() {
            mockRepository = mock(DiscoRepository.class);
            mockTemplate = mock(SolrTemplate.class);
            indexableThingMapper = assembleConcreteITMapper();
            indexDTOMapper = assembleConcreteDTOMapper();

            underTest.setDelegate(mockRepository);
            underTest.setTemplate(mockTemplate);
            underTest.setIndexableThingMapper(indexableThingMapper);
            underTest.setDtoMapper(indexDTOMapper);
            return this;
        }

        private IndexableThingMapper assembleConcreteITMapper() {
            AgentMapper agentMapper = new SimpleAgentMapper();
            EventMapper eventMapper = new SimpleEventMapper();
            DiscoMapper discoMapper = new SimpleDiscoMapper();

            return new SimpleIndexableThingMapper(discoMapper, agentMapper, eventMapper);
        }

        private IndexDTOMapper assembleConcreteDTOMapper() {
            return new SimpleIndexDTOMapper(new StandAloneStatusInferencer());
        }
    }
}