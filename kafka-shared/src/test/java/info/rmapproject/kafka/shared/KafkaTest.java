package info.rmapproject.kafka.shared;

import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.TestPropertySource;

@ContextHierarchy(@ContextConfiguration(locations = {"classpath:/spring-rmapcore-test-context.xml"}))
@TestPropertySource(locations = { "classpath:/kafka-broker.properties" })
@EmbeddedKafka(topics = { "rmap-event-topic" },
        brokerProperties = { "log.dir=${kafka.broker.logs-dir}", "listeners=PLAINTEXT://localhost:${kafka.broker.port}", "auto.create.topics.enable=true" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public interface KafkaTest {

}
