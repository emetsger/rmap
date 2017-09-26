package info.rmapproject.spring.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.env.Environment;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;

import java.util.List;

/**
 * The {@link ContextCustomizerFactory} implementation to produce a
 * {@link PropertyResolvingKafkaContextCustomizer} if a {@link EmbeddedKafka} annotation
 * is present on the test class.
 *
 * @author Artem Bilan
 * @since 1.3
 */
public class PropertyResolvingKafkaContextCustomizerFactory implements ContextCustomizerFactory {

    private static final Logger LOG = LoggerFactory.getLogger(PropertyResolvingKafkaContextCustomizerFactory.class);

//    private Environment env;

    @Override
    public ContextCustomizer createContextCustomizer(Class<?> testClass,
                                                     List<ContextConfigurationAttributes> configAttributes) {
        PropertyResolvingEmbeddedKafka embeddedKafka =
                AnnotatedElementUtils.findMergedAnnotation(testClass, PropertyResolvingEmbeddedKafka.class);

        if (embeddedKafka != null) {
            LOG.debug(
                    "Creating new PropertyResolvingKafkaContextCustomizer for test class [{}]",
                    testClass);
            return new PropertyResolvingKafkaContextCustomizer(embeddedKafka);
        }

        return null;
    }
//
//    @Override
//    public void setEnvironment(Environment environment) {
//        this.env = environment;
//    }
}
