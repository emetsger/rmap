/*******************************************************************************
 * Copyright 2017 Johns Hopkins University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This software was produced as part of the RMap Project (http://rmap-project.info),
 * The RMap Project was funded by the Alfred P. Sloan Foundation and is a 
 * collaboration between Data Conservancy, Portico, and IEEE.
 *******************************************************************************/
package info.rmapproject.core;

import info.rmapproject.kafka.shared.KafkaJunit4Bootstrapper;
import info.rmapproject.kafka.shared.KafkaTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.rule.KafkaEmbedded;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import static java.lang.Integer.parseInt;
import static org.junit.Assert.assertNotNull;

/**
 * Class for other test classes to inherit from. There are several annotations and settings required 
 * for most of the test classes, this sets them.  Note that the default class annotations can be 
 * overridden by defining them in the concrete class
 * @author khanson
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ComponentScan("info.rmapproject.core")
@ComponentScan("info.rmapproject.kafka")
@ContextConfiguration({ "classpath:/spring-rmapcore-context.xml", "classpath:/spring-rmapcore-test-context.xml"})
@TestPropertySource(locations = { "classpath:/rmapcore.properties", "classpath:/kafka-broker.properties" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@EmbeddedKafka(topics = { "rmap-event-topic" },
		brokerProperties = { "log.dir=${kafka.broker.logs-dir}", "listeners=PLAINTEXT://localhost:${kafka.broker.port}", "auto.create.topics.enable=true" })
public abstract class CoreTestAbstract {

	private static final String KAFKA_BROKER_PROPERTIES_RESOURCE = "/kafka-broker.properties";

	private static final String SPRING_ACTIVE_PROFILE_PROP = "spring.profiles.active";

	private static boolean activeProfilesPreSet = System.getProperties().containsKey(SPRING_ACTIVE_PROFILE_PROP);
	
	@BeforeClass
	public static void setUpSpringProfiles() {
		if (!activeProfilesPreSet) {
			System.setProperty("spring.profiles.active", "default, inmemory-triplestore, inmemory-idservice");
		}
	}
	
	@AfterClass
	public static void resetSpringProfiles() throws Exception {
		if (!activeProfilesPreSet) {
			System.getProperties().remove(SPRING_ACTIVE_PROFILE_PROP);
		}
	}

    protected static KafkaEmbedded newKafkaBrokerRule(Properties props) {
		final String PORT = "kafka.broker.port";
		final String PART = "kafka.broker.partition-count";
		final String LOGS = "kafka.broker.logs-dir";
		final String TOPICS = "kafka.broker.topics";

		requiredProperty(props, PORT);
		requiredProperty(props, PART);
		requiredProperty(props, LOGS);
		requiredProperty(props, TOPICS);

		requiredIntProperty(props, PORT);
		requiredIntProperty(props, PART);

		return KafkaJunit4Bootstrapper.kafkaBroker(
					parseInt(props.getProperty(PORT)),
					parseInt(props.getProperty(PART)),
					props.getProperty(LOGS),
					props.getProperty(TOPICS).split(","));
		}

	private static void requiredIntProperty(Properties props, String key) {
		try {
			parseInt(props.getProperty(key));
		} catch (NumberFormatException e) {
			throw new IllegalStateException(key + " must be an integer." , e);
		}
	}

	private static void requiredProperty(Properties props, String key) {
		if (props.getProperty(key) == null) {
			throw new IllegalStateException("Missing required property " + key);
		}
	}

	protected static Properties loadKafkaBrokerProperties() throws IOException {
		URL resource = CoreTestAbstract.class.getResource(KAFKA_BROKER_PROPERTIES_RESOURCE);
		assertNotNull("Missing required classpath resource: " + KAFKA_BROKER_PROPERTIES_RESOURCE, resource);

		Properties props = new Properties();

		try (InputStream stream = resource.openStream()) {
			props.load(stream);
		}

		return props;
	}
}
