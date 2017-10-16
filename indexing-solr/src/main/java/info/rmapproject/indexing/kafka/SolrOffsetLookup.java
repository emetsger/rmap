package info.rmapproject.indexing.kafka;

import info.rmapproject.indexing.IndexUtils;
import info.rmapproject.indexing.solr.model.KafkaMetadata;
import info.rmapproject.indexing.solr.repository.KafkaMetadataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static info.rmapproject.indexing.solr.repository.KafkaMetadataRepository.SORT_ASC_BY_KAFKA_OFFSET;
import static info.rmapproject.indexing.solr.repository.KafkaMetadataRepository.SORT_DESC_BY_KAFKA_OFFSET;

/**
 * Determines the earliest or latest offset for a topic and partition by consulting a Solr repository.
 * <p>
 * Kafka-related metadata is stored with Solr documents that implement {@link KafkaMetadata}.  This implementation is
 * able to look up Kafka offsets for a given topic and partition by querying a Solr repository containing such
 * documents.
 * </p>
 *
 * @param <T> the solr document type that carries Kafka-related metadata
 */
public class SolrOffsetLookup<T extends KafkaMetadata> implements OffsetLookup {

    private static Logger LOG = LoggerFactory.getLogger(SolrOffsetLookup.class);

    private Map<String, KafkaMetadataRepository<T>> repositories;

    /**
     * Support offset lookups for any topic in the supplied Map.  The expectation is that Kafka metadata for a topic
     * is kept in exactly one Solr repository.
     *
     * @param repositories mapping of Kafka topics to Solr repositories
     */
    public SolrOffsetLookup(Map<String, KafkaMetadataRepository<T>> repositories) {
        this.repositories = IndexUtils.assertNotNull(repositories,
                "Topic to Repository map must not be null.");
        if (repositories.isEmpty()) {
            throw IndexUtils.iae("Topic to Repository map must not be empty.").get();
        }
    }

    @Override
    public long lookupOffset(String topic, int partition, Seek seek) {
        KafkaMetadataRepository<T> repo = repositories.get(topic);

        if (repo == null) {
            LOG.warn("Unable to determine latest offset for topic {}, mapping to KafkaMetadataRepository is missing." +
                    "  (Hint: look at the wiring for {}" + this.getClass().getSimpleName());
            return -1;
        }

        List<T> results = repo.
                findTopDiscoSolrDocumentByKafkaTopicAndKafkaPartition(topic, partition,
                        (seek == Seek.LATEST) ? SORT_DESC_BY_KAFKA_OFFSET : SORT_ASC_BY_KAFKA_OFFSET);

        if (results == null || results.isEmpty()) {
            return -1;
        }

        return results.get(0).getKafkaOffset();
    }
}
