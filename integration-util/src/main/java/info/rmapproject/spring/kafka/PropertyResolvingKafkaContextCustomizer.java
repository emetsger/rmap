package info.rmapproject.spring.kafka;

import info.rmapproject.kafka.shared.KafkaPropertyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultSingletonBeanRegistry;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.rule.KafkaEmbedded;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.util.Assert;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PropertyResolvingKafkaContextCustomizer implements ContextCustomizer, EnvironmentAware {

    private static final Logger LOG = LoggerFactory.getLogger(PropertyResolvingKafkaContextCustomizer.class);

    private final PropertyResolvingEmbeddedKafka embeddedKafka;

    private Environment env;

    public PropertyResolvingKafkaContextCustomizer(PropertyResolvingEmbeddedKafka embeddedKafka) {
        if (embeddedKafka == null) {
            throw new IllegalArgumentException("EmbeddedKafka annotation must not be null.");
        }

        this.embeddedKafka = embeddedKafka;
    }

//    PropertyResolvingKafkaContextCustomizer(PropertyResolvingEmbeddedKafka embeddedKafka, Environment env) {
//        if (embeddedKafka == null) {
//            throw new IllegalArgumentException("EmbeddedKafka annotation must not be null.");
//        }
//
//        if (env == null) {
//            throw new IllegalArgumentException("Environment must not be null.");
//        }
//
//        this.embeddedKafka = embeddedKafka;
//        this.env = env;
//    }

    @Override
    @SuppressWarnings("unchecked")
    public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
        ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
        Assert.isInstanceOf(DefaultSingletonBeanRegistry.class, beanFactory);

        KafkaEmbedded kafkaEmbedded = new KafkaEmbedded(embeddedKafka.count(),
                embeddedKafka.controlledShutdown(),
                embeddedKafka.partitions(),
                embeddedKafka.topics());

        MutablePropertySources sources = new MutablePropertySources();

        try (StringReader reader = new StringReader(Stream.of(embeddedKafka.brokerProperties())
                .collect(Collectors.joining("\n")))) {
            Properties brokerProperties = new Properties();
            brokerProperties.load(reader);
            sources.addLast(new PropertiesPropertySource("embedded-kafka-broker-properties", brokerProperties));
            LOG.debug("Loaded [{}] Kafka broker properties from [{}] annotation",
                    brokerProperties.size(), embeddedKafka);
            StringBuilder props = brokerProperties.stringPropertyNames().stream().collect(StringBuilder::new, (builder, s) -> builder.append(s).append("=").append(brokerProperties.getProperty(s)), (builder1, builder2) -> builder1.append(builder2.toString()));
            LOG.debug("[{}]", props);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load broker properties: " + e.getMessage(), e);
        }

        Map<String, Object> brokerProperties = KafkaPropertyUtils.asMap(sources, null, false);
        LOG.debug("Resolving Kafka broker properties with [{}] environment and [{}] property sources", env, sources);
        KafkaPropertyUtils.resolveProperties(brokerProperties, new PropertySourcesPropertyResolver(sources), env);

        kafkaEmbedded.brokerProperties((Map<String, String>) (Map<?, ?>)brokerProperties);

        beanFactory.initializeBean(kafkaEmbedded, KafkaEmbedded.BEAN_NAME);
        beanFactory.registerSingleton(KafkaEmbedded.BEAN_NAME, kafkaEmbedded);
        ((DefaultSingletonBeanRegistry) beanFactory).registerDisposableBean(KafkaEmbedded.BEAN_NAME, kafkaEmbedded);
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.env = environment;
    }
}
