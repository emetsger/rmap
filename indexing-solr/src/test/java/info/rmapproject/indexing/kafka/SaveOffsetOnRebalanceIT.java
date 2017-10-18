package info.rmapproject.indexing.kafka;

import info.rmapproject.core.model.event.RMapEvent;
import info.rmapproject.indexing.IndexingInterruptedException;
import info.rmapproject.indexing.solr.AbstractSpringIndexingTest;
import info.rmapproject.kafka.shared.SpringKafkaConsumerFactory;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.TopicPartition;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class SaveOffsetOnRebalanceIT extends AbstractSpringIndexingTest {

    @Autowired
    private IndexingConsumer indexer;

    @Value("${rmapcore.producer.topic}")
    private String topic;

    /**
     * Arguably a consumer test.  Insures that the rebalancer methods are invoked when a consumer joins.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testPartitionsRevokedAndAssignedInvokedOnConsumerJoin() throws UnknownOffsetException, InterruptedException {
        CountDownLatch initialLatch2 = new CountDownLatch(2);
        CountDownLatch initialLatch4 = new CountDownLatch(4);

        indexer.setRebalanceListener(new ConsumerAwareRebalanceListener<String, RMapEvent>() {
            @Override
            public void setConsumer(Consumer<String, RMapEvent> consumer) {
                // no-op
            }

            @Override
            public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
                LOG.debug("Initial Consumer: Partitions Revoked {}", KafkaUtils.topicPartitionsAsString(partitions));
                initialLatch2.countDown();
                initialLatch4.countDown();
            }

            @Override
            public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
                LOG.debug("Initial Consumer: Partitions Assigned {}", KafkaUtils.topicPartitionsAsString(partitions));
                initialLatch2.countDown();
                initialLatch4.countDown();
            }
        });

        Thread t = new Thread(newConsumerRunnable(), "testPartitionsRevokedAndAssignedOnConsumerJoin-consumer");
        t.start();

        // rebalancer should be called when the consumer starts.
        assertTrue(initialLatch2.await(60000, TimeUnit.MILLISECONDS));

        CountDownLatch secondaryLatch2 = new CountDownLatch(2);
        Consumer secondaryConsumer = SpringKafkaConsumerFactory.newConsumer("-02");
        secondaryConsumer.subscribe(Collections.singleton(topic), new ConsumerAwareRebalanceListener<String, RMapEvent>() {
            @Override
            public void setConsumer(Consumer<String, RMapEvent> consumer) {
                // no-op
            }

            @Override
            public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
                LOG.debug("Secondary Consumer: Partitions Revoked {}", KafkaUtils.topicPartitionsAsString(partitions));
                secondaryLatch2.countDown();
            }

            @Override
            public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
                LOG.debug("Secondary Consumer: Partitions Assigned {}", KafkaUtils.topicPartitionsAsString(partitions));
                secondaryLatch2.countDown();
            }
        });

        // Fire up a second consumer, and invoke poll so it gets its partitions assigned.  the rebalancer should be
        // invoked for both consumers
        secondaryConsumer.poll(0);
        t.interrupt();  // short-circuit any polling in the initial consumer, speed things up.

        // the initial rebalancer should have its methods invoked a total of four times
        assertTrue(initialLatch4.await(60000, TimeUnit.MILLISECONDS));

        // the second rebalancer should have its methods invoked a total of two times
        assertTrue(secondaryLatch2.await(60000, TimeUnit.MILLISECONDS));

        // cleanup.  wakeup causes the initial indexer close its consumer and exit
        LOG.debug("Waking up initial consumer.");
        indexer.getConsumer().wakeup();
        LOG.debug("Thread joining.");
        t.join();
        LOG.debug("Closing secondary consumer.");
        secondaryConsumer.close();
    }

    /**
     * Arguably a consumer test.  Insures that the rebalancer methods are invoked when a consumer joins.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testPartitionsRevokedAndAssignedInvokedOnStart() throws UnknownOffsetException, InterruptedException {
        ConsumerAwareRebalanceListener underTest = mock(ConsumerAwareRebalanceListener.class);
        indexer.setRebalanceListener(underTest);

        Thread t = new Thread(newConsumerRunnable(), "testPartitionsRevokedAndAssignedOnStart-consumer");
        t.start();

        // allow thread to run a bit
        Thread.sleep(5000);

        LOG.debug("Waking up consumer.");
        indexer.getConsumer().wakeup();

        LOG.debug("Thread joining.");
        t.join();

        verify(underTest).onPartitionsRevoked(Collections.emptySet());
        verify(underTest).onPartitionsAssigned(new HashSet(){
            {
                add(new TopicPartition(topic, 0));
                add(new TopicPartition(topic, 1));
            }
        });


    }

    private Runnable newConsumerRunnable() {
        return () -> {
            try {
                indexer.consumeEarliest(topic);
            } catch (UnknownOffsetException e) {
                throw new RuntimeException(e.getMessage(), e);
            } catch (RuntimeException e) {
                if (e.getCause() instanceof IndexingInterruptedException) {
                    LOG.info("Caught {}", e.getMessage(), e);
                }
            }
        };
    }

}
