package info.rmapproject.kafka.shared;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.test.rule.KafkaEmbedded;

import java.util.HashMap;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Stream.of;


public class KafkaJunit4Bootstrapper {

    private static Logger LOG = LoggerFactory.getLogger(KafkaJunit4Bootstrapper.class);

    @SuppressWarnings("serial")
    public static KafkaEmbedded kafkaBroker(String topic) {
        return kafkaBroker(1, 2, false, topic);
    }

    public static KafkaEmbedded kafkaBroker(String topic, boolean controlledShutdown) {
        return kafkaBroker(1, 2, controlledShutdown, topic);
    }

    @SuppressWarnings("serial")
    public static KafkaEmbedded kafkaBroker(
            int brokerCount, int partitionCount, boolean controlledShutdown, String... topics) {

        LOG.debug("JUnit @Rule instantiating embedded Kafka broker [{}] for topic(s) [{}], brokerCount [{}], partitionCount [{}]",
                KafkaEmbedded.class.getName(), of(topics).collect(joining(", ")), brokerCount, partitionCount);
        KafkaEmbedded embedded = new KafkaEmbedded(1, false, 2, topics);
        LOG.debug("JUnit @Rule setting embedded Kafka broker property log.dirs: [{}]", System.getProperty("logs.dir"));
        embedded.brokerProperties(new HashMap<String, String>() {
            {
                put("logs.dir", System.getProperty("logs.dir"));
            }
        });

        LOG.debug("JUnit @Rule returning embedded Kafka broker instance [{}]", embedded);
        return embedded;

    }

}
