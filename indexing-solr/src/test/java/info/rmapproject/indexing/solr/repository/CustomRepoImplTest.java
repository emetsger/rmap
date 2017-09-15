package info.rmapproject.indexing.solr.repository;

import info.rmapproject.core.model.RMapStatus;
import info.rmapproject.core.model.disco.RMapDiSCO;
import info.rmapproject.indexing.solr.AbstractSpringIndexingTest;
import info.rmapproject.indexing.solr.IndexUtils;
import info.rmapproject.indexing.solr.TestResourceManager;
import info.rmapproject.indexing.solr.model.DiscoSolrDocument;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openrdf.rio.RDFFormat;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.PartialUpdate;
import org.springframework.data.solr.core.query.UpdateField;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
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
    
    private Mocks mocks;
    
    private DiscoRepository mockRepository;
    
    private SolrTemplate mockTemplate;

    @Before
    public void setUpMocks() throws Exception {
        this.mocks = new Mocks().build();
        this.mockRepository = mocks.getMockRepository();
        this.mockTemplate = mocks.getMockTemplate();
    }

    @Test
    public void testIndexCreate() throws Exception {
        rm = TestResourceManager.load(
                "/data/discos/rmd18mddcw", RDFFormat.NQUADS, rdfHandler);

        String discoIri = "rmap:rmd18m7mr7";
        IndexDTO dto = new IndexDTO(
                rm.getEvent("rmap:rmd18m7msr"),
                rm.getAgent("rmap:rmd18m7mj4"),
                null,
                rm.getDisco(discoIri));

        when(mockRepository.save(any(DiscoSolrDocument.class))).then(
                (invocation) -> {
                    DiscoSolrDocument doc = invocation.getArgumentAt(0, DiscoSolrDocument.class);
                    assertEquals(discoIri, doc.getDiscoUri());
                    return null;
                });

        underTest.index(dto);

        verify(mockRepository).save(any(DiscoSolrDocument.class));
        verifyZeroInteractions(mockTemplate);
    }

    @Test
    public void testIndexUpdate() throws Exception {
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

        when(mockRepository.save(any(DiscoSolrDocument.class))).then(
                (invocation) -> {
                    DiscoSolrDocument doc = invocation.getArgumentAt(0, DiscoSolrDocument.class);
                    saved.add(doc.getDiscoUri());
                    return null;
                });

        // This is the response from the index when we search for solr documents with a uri of rmap:rmd18m7mr7.
        // Note that the document returned from the index is ACTIVE.
        RepositoryDocuments repodocs = new RepositoryDocuments().build(mocks);
        repodocs.addDTOSource(dto, RMapStatus.ACTIVE);
        when(mockRepository.findDiscoSolrDocumentsByDiscoUri(sourceDiscoIri))
                .thenReturn(Collections.singleton(repodocs.getDocumentForIri(sourceDiscoIri)));

        // Insure that the status is updated to INACTIVE
        whenSolrTemplateSavesBeansAssertStatus(mockTemplate, sourceDiscoIri, RMapStatus.INACTIVE);

        underTest.index(dto);

        verify(mockRepository, times(2)).save(any(DiscoSolrDocument.class));
        assertEquals(2, saved.size());
        assertTrue(saved.contains(sourceDiscoIri));
        assertTrue(saved.contains(targetDiscoIri));

        verify(mockTemplate).saveBeans(eq(CORE_NAME), anySetOf(DiscoPartialUpdate.class));
        verify(mockTemplate).commit(CORE_NAME);
    }

    @Test
    public void testIndexInactivate() throws Exception {
        rm = TestResourceManager.load(
                "/data/discos/931zcrjgzb", RDFFormat.NQUADS, rdfHandler);

        String sourceDiscoIri = "rmap:rmp1835x5x";
        IndexDTO dto = new IndexDTO(
                rm.getEvent("rmap:931zcrjgzb"),
                rm.getAgent("rmap:rmp2543q5"),
                rm.getDisco(sourceDiscoIri),
                null);

        Set<String> saved = new HashSet<>();

        when(mockRepository.save(any(DiscoSolrDocument.class))).then(
                (invocation) -> {
                    DiscoSolrDocument doc = invocation.getArgumentAt(0, DiscoSolrDocument.class);
                    saved.add(doc.getDiscoUri());
                    return null;
                });

        // This is the response from the index when we search for solr documents with a uri of rmap:rmp1835x5x.
        // Note that the document returned from the index is ACTIVE.
        RepositoryDocuments repodocs = new RepositoryDocuments().build(mocks);
        repodocs.addDTOSource(dto, RMapStatus.ACTIVE);
        when(mockRepository.findDiscoSolrDocumentsByDiscoUri(sourceDiscoIri))
                .thenReturn(Collections.singleton(repodocs.getDocumentForIri(sourceDiscoIri)));

        // Insure that the status is updated to INACTIVE
        whenSolrTemplateSavesBeansAssertStatus(mockTemplate, sourceDiscoIri, RMapStatus.INACTIVE);

        underTest.index(dto);

        verify(mockRepository).save(any(DiscoSolrDocument.class));
        assertEquals(1, saved.size());
        assertTrue(saved.contains(sourceDiscoIri));

        verify(mockTemplate).saveBeans(eq(CORE_NAME), anySetOf(DiscoPartialUpdate.class));
        verify(mockTemplate).commit(CORE_NAME);
    }

    @Test
    public void testIndexTombstone() throws Exception {
        rm = TestResourceManager.load(
                "/data/discos/rmp1892gbc", RDFFormat.NQUADS, rdfHandler);

        String sourceDiscoIri = "rmap:rmp1892gbc";
        IndexDTO dto = new IndexDTO(
                rm.getEvent("rmap:rmp1892fdx"),
                rm.getAgent("rmap:rmd18m7mj4"),
                rm.getDisco(sourceDiscoIri),
                null);

        Set<String> saved = new HashSet<>();

        when(mockRepository.save(any(DiscoSolrDocument.class))).then(
                (invocation) -> {
                    DiscoSolrDocument doc = invocation.getArgumentAt(0, DiscoSolrDocument.class);
                    saved.add(doc.getDiscoUri());
                    return null;
                });

        // This is the response from the index when we search for solr documents with a uri of rmap:rmp1892gbc.
        // Note that the document returned from the index is ACTIVE.
        RepositoryDocuments repodocs = new RepositoryDocuments().build(mocks);
        repodocs.addDTOSource(dto, RMapStatus.ACTIVE);
        when(mockRepository.findDiscoSolrDocumentsByDiscoUri(sourceDiscoIri))
                .thenReturn(Collections.singleton(repodocs.getDocumentForIri(sourceDiscoIri)));

        // Insure that the status is updated to TOMBSTONED
        whenSolrTemplateSavesBeansAssertStatus(mockTemplate, sourceDiscoIri, RMapStatus.TOMBSTONED);

        underTest.index(dto);

        verify(mockRepository).save(any(DiscoSolrDocument.class));
        assertEquals(1, saved.size());
        assertTrue(saved.contains(sourceDiscoIri));

        verify(mockTemplate).saveBeans(eq(CORE_NAME), anySetOf(DiscoPartialUpdate.class));
        verify(mockTemplate).commit(CORE_NAME);
    }

    @Test
    @Ignore("DELETE isn't implemented yet in RMAP core")
    public void testIndexDelete() throws Exception {

    }

    @Test
    public void testIndexDerive() throws Exception {
        rm = TestResourceManager.load(
                "/data/discos/jwstqjq4mq", RDFFormat.NQUADS, rdfHandler);

        String sourceDiscoIri = "rmap:rmd18mddgf";
        String targetDiscoIri = "rmap:9cnp5hqdb7";
        IndexDTO dto = new IndexDTO(
                rm.getEvent("rmap:jwstqjq4mq"),
                rm.getAgent("rmap:gtht76hgmh"),
                rm.getDisco(sourceDiscoIri),
                rm.getDisco(targetDiscoIri));

        Set<String> saved = new HashSet<>();

        when(mockRepository.save(any(DiscoSolrDocument.class))).then(
                (invocation) -> {
                    DiscoSolrDocument doc = invocation.getArgumentAt(0, DiscoSolrDocument.class);
                    saved.add(doc.getDiscoUri());
                    return null;
                });

        underTest.index(dto);

        verify(mockRepository).save(any(DiscoSolrDocument.class));
        assertEquals(1, saved.size());
        assertTrue(saved.contains(targetDiscoIri));

        verifyZeroInteractions(mockTemplate);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void updateDocumentStatusByDiscoIri() throws Exception {
        rm = TestResourceManager.load(
                "/data/discos/rmd18mddcw", RDFFormat.NQUADS, rdfHandler);

        // We want to INACTIVATE all disco solr docs that contain the disco iri rmap:rmd18mddcw
        RMapDiSCO disco = rm.getDisco("rmap:rmd18mddcw");
        RMapStatus expectedStatus = RMapStatus.INACTIVE;

        // This is the response from the index when we search for solr documents with a uri of rmap:rmd18mddcw.
        // Note that the document returned from the index is ACTIVE.
        DiscoSolrDocument mockReponse = mockRepositoryResponse(mocks, RMapStatus.ACTIVE);
        when(mockRepository.findDiscoSolrDocumentsByDiscoUri(disco.getId().getStringValue()))
                .thenReturn(Collections.singleton(mockReponse));

        // Insure that the partial update to the index for the disco is updated to INACTIVE
        whenSolrTemplateSavesBeansAssertStatus(mockTemplate, disco.getId().getStringValue(), expectedStatus);

        underTest.updateDocumentStatusByDiscoIri(disco.getId(), expectedStatus, null);

        verify(mockRepository).findDiscoSolrDocumentsByDiscoUri(disco.getId().getStringValue());
        verify(mockTemplate).saveBeans(eq(CORE_NAME), anySetOf(PartialUpdate.class));
        verify(mockTemplate).commit(CORE_NAME);
    }

    @SuppressWarnings("unchecked")
    private void whenSolrTemplateSavesBeansAssertStatus(SolrTemplate mockTemplate, String discoIri, RMapStatus expectedStatus) {
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
                            update,
                            DISCO_STATUS,
                            expectedStatus.toString());

                    assertValuePresenceForUpdateField(
                            update,
                            DOC_LAST_UPDATED);

                    return null;
                });
    }

    private DiscoSolrDocument mockRepositoryResponse(Mocks mocks, RMapStatus status) {
        EventDiscoTuple it = new EventDiscoTuple();
        it.disco = rm.getDisco("rmap:rmd18mddcw");
        it.agent = rm.getAgent("rmap:rmd18m7mj4");
        it.event = rm.getEvent("rmap:rmd18m7msr");

        it.status = status;
        it.eventTarget = it.disco.getId();

        return mocks.getEventDiscoTupleMapper().apply(it);
    }

    private DiscoSolrDocument mockRepositoryResponse2(EventDiscoTuple it, RMapStatus status, Mocks mocks) {
        DiscoSolrDocument doc = mocks.getEventDiscoTupleMapper().apply(it);
        doc.setDiscoStatus(status.toString());
        return doc;
    }

    private void assertValueForUpdateField(DiscoPartialUpdate update, String fieldName, String expectedValue) {
        Stream<UpdateField> updateFields = update.getUpdates().stream();
        String actualValue = (String)updateFields.filter(updateField -> updateField.getName().equals(fieldName))
                .findAny()
                .orElseThrow(iae("Did not find an update field for " + fieldName))
                .getValue();
        assertEquals(expectedValue, actualValue);
    }

    private void assertValuePresenceForUpdateField(DiscoPartialUpdate update, String fieldName) {
        Stream<UpdateField> updateFields = update.getUpdates().stream();
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
        private EventDiscoTupleMapper eventDiscoTupleMapper;
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
         * A concrete mapper which maps instances of {@link EventDiscoTuple} to {@link DiscoSolrDocument} instances.
         *
         * @return
         */
        public EventDiscoTupleMapper getEventDiscoTupleMapper() {
            return eventDiscoTupleMapper;
        }

        /**
         * A concrete mapper which maps instances of {@link IndexDTO} to {@link EventDiscoTuple} instances.
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
            eventDiscoTupleMapper = assembleConcreteITMapper();
            indexDTOMapper = assembleConcreteDTOMapper();

            underTest.setDelegate(mockRepository);
            underTest.setTemplate(mockTemplate);
            underTest.setEventDiscoTupleMapper(eventDiscoTupleMapper);
            underTest.setDtoMapper(indexDTOMapper);
            return this;
        }

        private EventDiscoTupleMapper assembleConcreteITMapper() {
            AgentMapper agentMapper = new SimpleAgentMapper();
            EventMapper eventMapper = new SimpleEventMapper();
            DiscoMapper discoMapper = new SimpleDiscoMapper();

            return new SimpleEventDiscoTupleMapper(discoMapper, agentMapper, eventMapper);
        }

        private IndexDTOMapper assembleConcreteDTOMapper() {
            return new SimpleIndexDTOMapper(new StandAloneStatusInferencer());
        }
    }

    private class RepositoryDocuments {
        Set<DiscoSolrDocument> docs;
        Mocks mocks;

        RepositoryDocuments build(Mocks mocks) {
            docs = new HashSet<>();
            this.mocks = mocks;
            return this;
        }

        Set<DiscoSolrDocument> getDocumentsForIri(String discoIri) {
            Set<DiscoSolrDocument> result = docs
                    .stream()
                    .filter(doc -> doc.getDiscoUri().equals(discoIri))
                    .collect(Collectors.toSet());

            if (result.size() == 0) {
                throw new IllegalStateException("No document found for disco iri " + discoIri);
            }

            return result;
        }

        DiscoSolrDocument getDocumentForIri(String discoIri) {
            Set<DiscoSolrDocument> result = docs
                    .stream()
                    .filter(doc -> doc.getDiscoUri().equals(discoIri))
                    .collect(Collectors.toSet());

            if (result.size() == 0) {
                throw new IllegalStateException("No document found for disco iri " + discoIri);
            }

            if (result.size() > 1) {
                throw new IllegalStateException("Multiple documents (" + result.size() + ") found for disco iri " + discoIri);
            }

            return result.iterator().next();
        }

        void addDTOTarget(IndexDTO dto, RMapStatus status) {
            addWithDirectionAndStatus(dto, IndexUtils.EventDirection.TARGET, status);
        }

        void addDTOTarget(IndexDTO dto, String status) {
            addWithDirectionAndStatus(dto, IndexUtils.EventDirection.TARGET, status);
        }


        void addDTOSource(IndexDTO dto, RMapStatus status) {
            addWithDirectionAndStatus(dto, IndexUtils.EventDirection.SOURCE, status);
        }

        void addDTOSource(IndexDTO dto, String status) {
            addWithDirectionAndStatus(dto, IndexUtils.EventDirection.SOURCE, status);
        }

        void addWithDirectionAndStatus(IndexDTO dto, IndexUtils.EventDirection direction, RMapStatus status) {
            addWithDirectionAndStatus(dto, direction, status.toString());
        }

        void addWithDirectionAndStatus(IndexDTO dto, IndexUtils.EventDirection direction, String status) {
            switch (direction) {
                case SOURCE:
                    addWithStatus(mocks.getIndexDTOMapper().getSourceIndexableThing(dto), status);
                    break;
                case TARGET:
                    addWithStatus(mocks.getIndexDTOMapper().getTargetIndexableThing(dto), status);
                    break;
                case EITHER:
                    addWithStatus(mocks.getIndexDTOMapper().getSourceIndexableThing(dto), status);
                    addWithStatus(mocks.getIndexDTOMapper().getTargetIndexableThing(dto), status);
                    break;
                default:
                    throw new RuntimeException("Unknown EventDirection " + direction);
            }
        }

        void add(EventDiscoTuple eventDiscoTuple) {
            docs.add(mocks.getEventDiscoTupleMapper().apply(eventDiscoTuple));
        }

        void addWithStatus(EventDiscoTuple eventDiscoTuple, RMapStatus status) {
            addWithStatus(eventDiscoTuple, status.toString());
        }

        void addWithStatus(EventDiscoTuple eventDiscoTuple, String status) {
            DiscoSolrDocument doc = mocks.getEventDiscoTupleMapper().apply(eventDiscoTuple);
            doc.setDiscoStatus(status);
            docs.add(doc);
        }
    }
}