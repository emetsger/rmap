package info.rmapproject.indexing.kafka;

import info.rmapproject.core.rdfhandler.RDFHandler;
import info.rmapproject.indexing.solr.AbstractSpringIndexingTest;
import info.rmapproject.indexing.solr.repository.IndexDTO;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.listener.config.ContainerProperties;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.rule.KafkaEmbedded;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static info.rmapproject.indexing.solr.TestUtils.prepareIndexableDtos;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
@DirtiesContext
@EmbeddedKafka(partitions = 1, topics = {"topic1", "topic2"})
@Ignore("To allow ITS to run")
public class SimpleKafkaTest extends AbstractSpringIndexingTest {

    private static final Logger LOG = LoggerFactory.getLogger(SimpleKafkaTest.class);

    @Autowired
    private KafkaEmbedded kafkaEmbedded;

    @Autowired
    private RDFHandler rdfHandler;

    @Test
    public void testSendIndexDTO() throws Exception {
        LOG.info("Start auto");
        ContainerProperties containerProps = new ContainerProperties("topic1", "topic2");
        List<IndexDTO> dtos = prepareIndexableDtos(rdfHandler, "/data/discos/rmd18mddcw", null)
                .collect(Collectors.toList());
        Queue<IndexDTO> expectedDtos = new ArrayDeque<>(dtos);
        final CountDownLatch latch = new CountDownLatch(3);
        containerProps.setMessageListener((MessageListener<Integer, IndexDTO>) message -> {
            LOG.info("received: " + message);
            IndexDTO expected = expectedDtos.remove();
            LOG.debug("expected: " + expected);
            IndexDTO actual = message.value();
            LOG.debug("actual: " + actual);
            assertEquals(expected, actual);
            LOG.debug("Decrementing latch.");
            latch.countDown();
        });
        KafkaMessageListenerContainer<Integer, String> container = createContainer(containerProps, IntegerDeserializer.class, ObjectDeserializer.class);
        container.setBeanName("testAuto");
        container.start();
        Thread.sleep(5000); // wait a bit for the container to start
        KafkaTemplate<Integer, IndexDTO> template = createTemplate(IntegerSerializer.class, ObjectSerializer.class);
        template.setDefaultTopic("topic1");

        prepareIndexableDtos(rdfHandler, "/data/discos/rmd18mddcw", null)
                .peek(dto -> LOG.debug("Prepared DTO {}", dto))
                .forEach(template::sendDefault);

        // do anything with the completablefuture returned by the template?

        template.flush();

        assertTrue(latch.await(120, TimeUnit.SECONDS));
        container.stop();

        LOG.info("Stop auto");
    }

    @Test
    public void testAutoCommit() throws Exception {
        LOG.info("Start auto");
        ContainerProperties containerProps = new ContainerProperties("topic1", "topic2");
        final CountDownLatch latch = new CountDownLatch(4);
        containerProps.setMessageListener((MessageListener<Integer, String>) message -> {
            LOG.info("received: " + message);
            latch.countDown();
        });
        KafkaMessageListenerContainer<Integer, String> container = createContainer(containerProps,
                IntegerDeserializer.class, StringDeserializer.class);
        container.setBeanName("testAuto");
        container.start();
        Thread.sleep(5000); // wait a bit for the container to start
        KafkaTemplate<Integer, String> template = createTemplate(IntegerSerializer.class, StringSerializer.class);
        template.setDefaultTopic("topic1");
        template.sendDefault(0, "foo");
        template.sendDefault(2, "bar");
        template.sendDefault(0, "baz");
        template.sendDefault(2, "qux");
        template.flush();
        assertTrue(latch.await(60, TimeUnit.SECONDS));
        container.stop();
        LOG.info("Stop auto");
    }

    private KafkaMessageListenerContainer<Integer, String> createContainer(
            ContainerProperties containerProps, Object keyDeser, Object valDeser) {
        Map<String, Object> props = consumerProps(keyDeser, valDeser);
        DefaultKafkaConsumerFactory<Integer, String> cf =
                new DefaultKafkaConsumerFactory<>(props);
        KafkaMessageListenerContainer<Integer, String> container =
                new KafkaMessageListenerContainer<>(cf, containerProps);
        return container;
    }

    private <T> KafkaTemplate<Integer, T> createTemplate(Object keySer, Object valSer) {
        Map<String, Object> senderProps = senderProps(keySer, valSer);
        ProducerFactory<Integer, T> pf =
                new DefaultKafkaProducerFactory<>(senderProps);
        KafkaTemplate<Integer, T> template = new KafkaTemplate<>(pf);
        return template;
    }

    private Map<String, Object> consumerProps(Object keyDeserializer, Object valueDeserializer) {
        Map<String, Object> props = KafkaTestUtils.consumerProps("group", "true", kafkaEmbedded);
//        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
//        props.put(ConsumerConfig.GROUP_ID_CONFIG, "group");
//        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "100");
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, "15000");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, keyDeserializer);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, valueDeserializer);
        return props;
    }

    private Map<String, Object> senderProps(Object keySerializer, Object valueSerializer) {
        Map<String, Object> props = KafkaTestUtils.senderProps(kafkaEmbedded.getBrokersAsString());
//        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ProducerConfig.RETRIES_CONFIG, 0);
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 1);
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, keySerializer);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, valueSerializer);
        return props;
    }

    public static class ObjectDeserializer implements Deserializer<IndexDTO> {

        public ObjectDeserializer() {
        }

        @Override
        public void configure(Map<String, ?> configs, boolean isKey) {
            // no-op
        }

        @Override
        public IndexDTO deserialize(String topic, byte[] data) {
            try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
                     ObjectInputStream ois = new ObjectInputStream(bais)) {
                return (IndexDTO) ois.readObject();
            } catch (IOException|ClassNotFoundException e) {
                throw new RuntimeException("Error deserializing an IndexDTO object: " + e.getMessage(), e);
            }
        }

        @Override
        public void close() {
            // no-op
        }
    }

    public static class ObjectSerializer implements Serializer<IndexDTO> {

        public ObjectSerializer() {
        }

        @Override
        public void configure(Map<String, ?> configs, boolean isKey) {
            // no-op
        }

        @Override
        public byte[] serialize(String topic, IndexDTO data) {
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                try(ObjectOutputStream oos = new ObjectOutputStream(baos)) {
                    oos.writeObject(data);
                }
                return baos.toByteArray();
            } catch (IOException e) {
                throw new RuntimeException("Error serializing an IndexDTO object: " + e.getMessage(), e);
            }
        }

        @Override
        public void close() {
            // no-op
        }
    }

}
