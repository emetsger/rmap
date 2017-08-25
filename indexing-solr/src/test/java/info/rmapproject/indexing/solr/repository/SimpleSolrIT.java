package info.rmapproject.indexing.solr.repository;

import info.rmapproject.core.model.RMapObjectType;
import info.rmapproject.core.model.agent.RMapAgent;
import info.rmapproject.core.model.disco.RMapDiSCO;
import info.rmapproject.core.model.event.RMapEvent;
import info.rmapproject.core.rdfhandler.RDFHandler;
import info.rmapproject.indexing.solr.AbstractSpringIndexingTest;
import info.rmapproject.indexing.solr.TestUtils;
import info.rmapproject.indexing.solr.model.DiscoSolrDocument;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static info.rmapproject.core.model.RMapStatus.ACTIVE;
import static info.rmapproject.core.model.RMapStatus.INACTIVE;
import static info.rmapproject.indexing.solr.TestUtils.prepareIndexableDtos;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class SimpleSolrIT extends AbstractSpringIndexingTest {

    @Autowired
    private RDFHandler rdfHandler;

    @Autowired
    private DiscoRepository discoRepository;

    /**
     * Tests the {@link DiscoRepository#index(IndexDTO)} method by supplying three {@code IndexDTO}s for indexing from
     * the {@code /data/discos/rmd18mddcw} directory:
     * <ul>
     *     <li>a creation event</li>
     *     <li>followed by an update event</li>
     *     <li>followed by another update event</li>
     * </ul>
     * This test insures that the {@link DiscoSolrDocument}s in the index have the correct
     * {@link DiscoSolrDocument#DISCO_STATUS} after the three events have been processed.
     *
     * @see <a href="src/test/resources/data/discos/rmd18mddcw/README.md">README.MD</a>
     */
    @Test
    public void testIndexingDiscoStatusCreateAndUpdateAndUpdate() {
        LOG.debug("Deleting everything in the index.");
        discoRepository.deleteAll();
        assertEquals(0, discoRepository.count());

        Consumer<Map<RMapObjectType, Set<TestUtils.RDFResource>>> assertions = (resources) -> {
            List<RMapDiSCO> discos = TestUtils.getRmapObjects(resources, RMapObjectType.DISCO, rdfHandler);
            assertEquals(3, discos.size());

            List<RMapEvent> events = TestUtils.getRmapObjects(resources, RMapObjectType.EVENT, rdfHandler);
            assertEquals(3, events.size());

            List<RMapAgent> agents = TestUtils.getRmapObjects(resources, RMapObjectType.AGENT, rdfHandler);
            assertEquals(1, agents.size());
        };

        LOG.debug("Preparing indexable objects.");
        Stream<IndexDTO> dtos = prepareIndexableDtos(rdfHandler,"/data/discos/rmd18mddcw", assertions);

        dtos.peek(dto -> LOG.debug("Indexing {}", dto)).forEach(dto -> discoRepository.index(dto));

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
        assertEquals(4, docs.size());

        // assert they have the uris we expect
        assertEquals(2, docs.stream().filter(doc -> doc.getDiscoUri().endsWith("rmd18mdd8b")).count());
        assertEquals(2, docs.stream().filter(doc -> doc.getDiscoUri().endsWith("rmd18m7mr7")).count());
    }


}
