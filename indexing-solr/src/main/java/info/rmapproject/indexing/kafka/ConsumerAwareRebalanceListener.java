package info.rmapproject.indexing.kafka;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public interface ConsumerAwareRebalanceListener<K, V> extends ConsumerRebalanceListener {

    void setConsumer(Consumer<K, V> consumer);

}
