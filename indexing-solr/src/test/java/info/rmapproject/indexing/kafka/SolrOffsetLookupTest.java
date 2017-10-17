package info.rmapproject.indexing.kafka;

import info.rmapproject.indexing.solr.model.DiscoSolrDocument;
import info.rmapproject.indexing.solr.repository.KafkaMetadataRepository;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;

import static info.rmapproject.indexing.solr.repository.KafkaMetadataRepository.SORT_ASC_BY_KAFKA_OFFSET;
import static info.rmapproject.indexing.solr.repository.KafkaMetadataRepository.SORT_DESC_BY_KAFKA_OFFSET;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SolrOffsetLookupTest {

    @Test(expected = IllegalArgumentException.class)
    public void testNullMapConstructor() throws Exception {
        new SolrOffsetLookup<DiscoSolrDocument>(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEmptyMapConstructor() throws Exception {
        new SolrOffsetLookup<DiscoSolrDocument>(Collections.emptyMap());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testLookupMissingMap() throws Exception {
        SolrOffsetLookup underTest = new SolrOffsetLookup(
                new HashMap<String, KafkaMetadataRepository>() {
                    {
                        put("foo", mock(KafkaMetadataRepository.class));
                    }
                });

        assertEquals(-1, underTest.lookupOffset("bar", 0, Seek.EARLIEST));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testNullResults() throws Exception {
        final KafkaMetadataRepository repo = mock(KafkaMetadataRepository.class);
        SolrOffsetLookup underTest = new SolrOffsetLookup(
                new HashMap<String, KafkaMetadataRepository>() {
                    {
                        put("foo", repo);
                    }

                });

        when(repo.findTopDiscoSolrDocumentByKafkaTopicAndKafkaPartition(
                "foo", 0, SORT_DESC_BY_KAFKA_OFFSET)).thenReturn(null);

        assertEquals(-1, underTest.lookupOffset("foo", 0, Seek.LATEST));

        verify(repo).findTopDiscoSolrDocumentByKafkaTopicAndKafkaPartition(
                "foo", 0, SORT_DESC_BY_KAFKA_OFFSET);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testEmptyResults() throws Exception {
        final KafkaMetadataRepository repo = mock(KafkaMetadataRepository.class);
        SolrOffsetLookup underTest = new SolrOffsetLookup(
                new HashMap<String, KafkaMetadataRepository>() {
                    {
                        put("foo", repo);
                    }

                });

        when(repo.findTopDiscoSolrDocumentByKafkaTopicAndKafkaPartition(
                "foo", 0, SORT_DESC_BY_KAFKA_OFFSET)).thenReturn(Collections.emptyList());

        assertEquals(-1, underTest.lookupOffset("foo", 0, Seek.LATEST));

        verify(repo).findTopDiscoSolrDocumentByKafkaTopicAndKafkaPartition(
                "foo", 0, SORT_DESC_BY_KAFKA_OFFSET);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testValidResult() throws Exception {
        final KafkaMetadataRepository repo = mock(KafkaMetadataRepository.class);
        SolrOffsetLookup underTest = new SolrOffsetLookup(
                new HashMap<String, KafkaMetadataRepository>() {
                    {
                        put("foo", repo);
                    }

                });
        DiscoSolrDocument doc = new DiscoSolrDocument.Builder().kafkaOffset(20).build();

        when(repo.findTopDiscoSolrDocumentByKafkaTopicAndKafkaPartition(
                "foo", 0, SORT_DESC_BY_KAFKA_OFFSET)).thenReturn(Collections.singletonList(doc));

        assertEquals(20, underTest.lookupOffset("foo", 0, Seek.LATEST));

        verify(repo).findTopDiscoSolrDocumentByKafkaTopicAndKafkaPartition(
                "foo", 0, SORT_DESC_BY_KAFKA_OFFSET);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSeek() throws Exception {
        final KafkaMetadataRepository repo = mock(KafkaMetadataRepository.class);
        SolrOffsetLookup underTest = new SolrOffsetLookup(
                new HashMap<String, KafkaMetadataRepository>() {
                    {
                        put("foo", repo);
                    }

                });

        underTest.lookupOffset("foo", 0, Seek.LATEST);

        verify(repo).findTopDiscoSolrDocumentByKafkaTopicAndKafkaPartition(
                "foo", 0, SORT_DESC_BY_KAFKA_OFFSET);

        underTest.lookupOffset("foo", 0, Seek.EARLIEST);

        verify(repo).findTopDiscoSolrDocumentByKafkaTopicAndKafkaPartition(
                "foo", 0, SORT_ASC_BY_KAFKA_OFFSET);

    }
}
