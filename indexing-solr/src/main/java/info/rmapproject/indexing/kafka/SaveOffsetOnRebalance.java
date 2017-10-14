package info.rmapproject.indexing.kafka;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class SaveOffsetOnRebalance<K, V> implements ConsumerAwareRebalanceListener<K, V> {

    private static final Logger LOG = LoggerFactory.getLogger(SaveOffsetOnRebalance.class);

    private Consumer<K, V> consumer;

    @Override
    public void setConsumer(Consumer<K, V> consumer) {
        if (consumer == null) {
            throw new IllegalArgumentException("Consumer must not be null.");
        }
        this.consumer = consumer;
    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        LOG.debug("Received {} event", "onPartitionsRevoked");
        // commit offsets
        // close any resources
    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        LOG.debug("Received {} event", "onPartitionsAssigned");
        // determine latest offset
        // seek to offset
    }

}
