package info.rmapproject.kafka.shared;

import org.apache.kafka.common.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySources;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class JustInTimeConfiguredProducerFactory<K, V> extends DefaultKafkaProducerFactory<K, V>
        implements EnvironmentAware {

    private static final Logger LOG = LoggerFactory.getLogger(JustInTimeConfiguredProducerFactory.class);

    private static final Object NULL_VALUE = new Object();

    private String prefix;

    private boolean strip = true;

    private MutablePropertySources sources = new MutablePropertySources();

    private Environment env;


    public JustInTimeConfiguredProducerFactory(Map<String, Object> configs, Serializer<K> keySerializer,
                                               Serializer<V> valueSerializer) {
        super(configs, keySerializer, valueSerializer);
        sources.addFirst(new MapPropertySource("producer-construction-config", mapNullValues(configs)));
    }


    @Override
    public Map<String, Object> getConfigurationProperties() {
        List<EnumerablePropertySource<?>> enumerablePropertySources = new ArrayList<>();

        sources.forEach(source -> {
                    if (source instanceof EnumerablePropertySource) {
                        enumerablePropertySources.add((EnumerablePropertySource) source);
                    }
                });

        Map<String, Object> props = new HashMap<>();

        enumerablePropertySources.forEach(source -> {
            Stream.of(source.getPropertyNames())
                    .filter(propName -> prefix == null || propName.startsWith(prefix))
                    .peek(propName -> LOG.debug("Prefix: [{}], property name: [{}]", prefix, propName))
                    .collect(Collectors.toMap(
                            propName -> (prefix == null || !strip || !propName.startsWith(prefix)) ? propName : propName.substring(prefix.length()),
                            source::getProperty,
                            (val1, val2) -> {
                                LOG.debug("Merging [{}], [{}]", val1, val2);
                                return val2;
                            },
                            () -> props
                    ))
                    .forEach((key, value) -> LOG.debug("Resolved property key [{}] to [{}]", key, (isNullValue(value)) ? "null" : value));
        });


        props.entrySet().stream()
                .filter(entry -> isNullValue(entry.getValue()))
                .forEach(entry -> {
                    final String resolvedValue = env.getProperty(entry.getKey());
                    LOG.debug("Resolving null value for property key [{}] from the environment: [{}]", entry.getKey(),
                            resolvedValue);
                    props.put(entry.getKey(), resolvedValue);
                });

        return props;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.env = environment;
    }

    public void setSources(PropertySources sources) {
        sources.forEach(source -> this.sources.addLast(source));
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public void setStrip(boolean strip) {
        this.strip = strip;
    }

    private static Stream<PropertySource<?>> asStream(PropertySources sources) {
        Iterable<PropertySource<?>> iterable = sources::iterator;
        return StreamSupport.stream(iterable.spliterator(), false);
    }

    private static Map<String, Object> mapNullValues(Map<String, Object> map) {
        Map<String, Object> copy = new HashMap<>();
        map.forEach((key, value) -> {
            if (value == null) {
                copy.put(key, NULL_VALUE);
            } else {
                copy.put(key, value);
            }
        });

        return copy;
    }

    private static boolean isNullValue(Object value) {
        return value == NULL_VALUE;
    }


}
