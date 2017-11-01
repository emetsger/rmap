package info.rmapproject.integration.util;

import org.springframework.kafka.test.rule.KafkaEmbedded;

import java.io.File;

public abstract class AbstractKafkaBrokerTask extends AbstractKafkaTask {

    static KafkaEmbedded BROKER;

    static boolean BROKER_IS_STARTED = false;

    String brokerHost = "localhost";

    int brokerPort;

    File brokerHome;

    File brokerPropertiesLocation;



    public File getBrokerPropertiesLocation() {
        return brokerPropertiesLocation;
    }

    public void setBrokerPropertiesLocation(File brokerPropertiesLocation) {
        if (brokerPropertiesLocation == null) {
            throw new IllegalArgumentException("Kafka broker properties location must not be null.");
        }
        this.brokerPropertiesLocation = brokerPropertiesLocation;
    }

    public String getBrokerHost() {
        return brokerHost;
    }

    public void setBrokerHost(String brokerHost) {
        if (brokerHost == null || brokerHost.trim().length() == 0) {
            throw new IllegalArgumentException("Kafka broker host must not be empty or null.");
        }
        this.brokerHost = brokerHost;
    }

    public int getBrokerPort() {
        return brokerPort;
    }

    public void setBrokerPort(int brokerPort) {
        if (brokerPort < 1) {
            throw new IllegalArgumentException("Kafka broker port must be a positive integer");
        }
        this.brokerPort = brokerPort;
    }

    public File getBrokerHome() {
        return brokerHome;
    }

    public void setBrokerHome(File brokerHome) {
        if (brokerHome == null) {
            throw new IllegalArgumentException("Kafka broker home must not be null.");
        }
        this.brokerHome = brokerHome;
    }
}
