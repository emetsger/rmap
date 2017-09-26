package info.rmapproject.spring.kafka;

import org.springframework.core.annotation.AliasFor;
import org.springframework.kafka.test.context.EmbeddedKafka;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface PropertyResolvingEmbeddedKafka {

    /**
     * @return the number of brokers
     */
    @AliasFor("count")
    int value() default 1;

    /**
     * @return the number of brokers
     */
    @AliasFor("value")
    int count() default 1;

    /**
     * @return passed into {@code kafka.utils.TestUtils.createBrokerConfig()}.
     */
    boolean controlledShutdown() default false;

    /**
     * @return partitions per topic
     */
    int partitions() default 2;

    /**
     * @return the topics to create
     */
    String[] topics() default { };

    /**
     * Properties in form {@literal key=value} that should be added
     * to the broker config before runs.
     * @return the properties to add
     * @see KafkaEmbedded#brokerProperties(java.util.Map)
     */
    String[] brokerProperties() default { };

}
