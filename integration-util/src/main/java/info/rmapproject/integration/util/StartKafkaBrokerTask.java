package info.rmapproject.integration.util;

import org.apache.tools.ant.BuildException;
import org.springframework.kafka.test.rule.KafkaEmbedded;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

public class StartKafkaBrokerTask extends AbstractKafkaBrokerTask {

    @Override
    public void init() throws BuildException {
        super.init();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void execute() throws BuildException {
//        if (getBrokerHome() == null) {
//            throw new IllegalStateException("Kakfa broker home must not be null.");
//        }
//
//        if (getBrokerPort() < 1) {
//            throw new IllegalStateException("Kafka broker port must be a positive integer.");
//        }
//
//        if (getBrokerHost() == null || getBrokerHost().trim().length() == 0) {
//            throw new IllegalStateException("Kafka broker host must not be empty or null.");
//        }
//
//        if (getBrokerPropertiesLocation() == null) {
//            throw new IllegalStateException("Kafka broker properties location must not be null.");
//        }

        Properties brokerProperties = new Properties();

        try (InputStream in = new FileInputStream(getBrokerPropertiesLocation())) {
            brokerProperties.load(in);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Kafka broker properties file not found '" + getBrokerPropertiesLocation() +
                    "': " + e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException("Error loading Kafka broker properties file '" + getBrokerPropertiesLocation() +
                    "': " + e.getMessage(), e);
        } catch (NullPointerException e) {
            throw new RuntimeException("The Kafka broker properties file produced a null input stream '" +
                    getBrokerPropertiesLocation() + "': " + e.getMessage(), e);
        }

        KafkaEmbedded broker = new KafkaEmbedded(1, true, 2, "topic");
        broker.brokerProperties((Map<String,String>) (Map<?,?>) brokerProperties);
        broker.setKafkaPorts(Arrays.stream(brokerProperties.getProperty("listeners").split(","))
                .map(URI::create)
                .mapToInt(URI::getPort)
                .toArray());

        try {
            broker.before();
            BROKER = broker;
            BROKER_IS_STARTED = true;
        } catch (Exception e) {
            throw new RuntimeException("Error starting Kafka broker: " + e.getMessage(), e);
        }
    }
}
