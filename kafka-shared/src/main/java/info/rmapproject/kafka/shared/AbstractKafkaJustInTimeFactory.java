package info.rmapproject.kafka.shared;

import org.apache.kafka.common.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;

public class AbstractKafkaJustInTimeFactory {

    private static final Logger LOG = LoggerFactory.getLogger(JustInTimeConfiguredProducerFactory.class);

    /**
     * Serializer for Kafka keys
     */
    private final Serializer<K> keySerializer;

    /**
     * Serializer for Kafka values
     */
    private final Serializer<V> valueSerializer;

    /**
     * Property keys with the supplied prefix will be used to configure the Kafka producer.  For example, if the
     * prefix is {@code rmapcore.producer.}, then any property key enumerated from {@link #sources} that begins with
     * {@code rmapcore.producer.} will be added to the {@link #getConfigurationProperties() producer configuration
     * properties}.  If the prefix is {@code null} (the default), <em>all</em> property keys from all property sources
     * will be considered a producer configuration property.
     */
    private String prefix;

    /**
     * If a {@link #prefix prefix} is specified, this flag determines whether or not the prefix is stripped from the
     * {@link #getConfigurationProperties() configuration property keys}.
     * <p>
     * Given a {@code prefix} of {@code rmapcore.producer.}, any property key enumerated from {@link #sources} that
     * begins with {@code rmapcore.producer.} will be added to the {@link #getConfigurationProperties() producer
     * configuration properties}.  For example, if the property key {@code rmapcore.producer.bootstrap.servers} is
     * present in {@link #sources the property sources}, and {@code strip} is {@code true}, the property key will be
     * added to the producer configuration properties as {@code bootstrap.servers} (the prefix is stripped off).  If
     * {@code strip} is {@code false}, the property key will be added to the producer configuration properties as
     * {@code rmapcore.producer.bootstrap.servers} (the prefix is preserved).
     * </p>
     */
    private boolean strip = true;

    /**
     * A mutable listing of {@link EnumerablePropertySource}s and subclasses thereof.  Property sources of any other
     * type are not considered by the {@code JustInTimeConfiguredProducerFactory}.
     */
    private MutablePropertySources sources = new MutablePropertySources();

    /**
     * The environment used to resolve {@code null} property values when {@link #getConfigurationProperties()} is called
     * (i.e. "just in time")
     */
    private Environment env;
}
