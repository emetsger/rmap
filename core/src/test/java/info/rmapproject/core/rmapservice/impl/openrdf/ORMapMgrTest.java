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
package info.rmapproject.core.rmapservice.impl.openrdf;

import static java.lang.Integer.parseInt;
import static java.net.URI.create;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import info.rmapproject.core.model.event.RMapEvent;
import info.rmapproject.core.model.impl.openrdf.StatementsAdapter;
import info.rmapproject.kafka.shared.JustInTimeConfiguredConsumerFactory;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.common.TopicPartition;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.ExternalResource;
import org.junit.rules.TestRule;
import org.openrdf.model.IRI;
import org.openrdf.model.Literal;
import org.openrdf.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import info.rmapproject.core.CoreTestAbstract;
import info.rmapproject.core.exception.RMapDefectiveArgumentException;
import info.rmapproject.core.exception.RMapException;
import info.rmapproject.core.model.impl.openrdf.ORAdapter;
import info.rmapproject.core.model.impl.openrdf.ORMapAgent;
import info.rmapproject.core.model.impl.openrdf.ORMapDiSCO;
import info.rmapproject.core.model.request.RMapRequestAgent;
import info.rmapproject.core.rdfhandler.RDFType;
import info.rmapproject.core.rdfhandler.impl.openrdf.RioRDFHandler;
import info.rmapproject.core.rmapservice.RMapService;
import info.rmapproject.core.rmapservice.impl.openrdf.triplestore.SesameSailMemoryTriplestore;
import info.rmapproject.core.rmapservice.impl.openrdf.triplestore.SesameTriplestore;
import info.rmapproject.testdata.service.TestConstants;
import info.rmapproject.testdata.service.TestDataHandler;
import info.rmapproject.testdata.service.TestFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.test.rule.KafkaEmbedded;
import org.springframework.test.context.TestPropertySource;

/**
 * @author khanson
 *
 */
@TestPropertySource(locations = { "classpath:/rmapcore.properties" })
public abstract class ORMapMgrTest extends CoreTestAbstract {

	private static final AtomicInteger counter = new AtomicInteger();

	static Logger LOG = LoggerFactory.getLogger(ORMapMgrTest.class);

	static KafkaEmbedded kafkaBroker;

	@Value("${rmapcore.producer.topic}")
	String topic;

	@Autowired
	protected RMapService rmapService;
	
	@Autowired
	SesameTriplestore triplestore;

	@Autowired
	JustInTimeConfiguredConsumerFactory<String, RMapEvent> consumerFactory;

	/** General use sysagent for testing **/
	protected ORMapAgent sysagent = null;
	
	/** Second general use sysagent for testing that requires 2 users **/
	protected ORMapAgent sysagent2 = null;
	
	/** Request agent based on sysagent. Include key */
	protected RMapRequestAgent requestAgent = null;
	
	/** Request agent based on sysagent2. No Key */
	protected RMapRequestAgent requestAgent2 = null;

	@ClassRule
	public static KafkaEmbedded kafkaBroker() throws IOException {
		kafkaBroker = newKafkaBrokerRule(loadKafkaBrokerProperties());
		return kafkaBroker;
	}

	@ClassRule
	public static TestRule removeKafkaLogDir() {
		return new ExternalResource() {
			@Override
			protected void after() {
				try {
					String logDirectory = loadKafkaBrokerProperties().getProperty("kafka.broker.logs-dir");
					LOG.debug("Verifying Kafka broker log directory [{}] is removed", logDirectory);
					assertFalse(new File(logDirectory).exists());
				} catch (IOException e) {
					fail("Error loading Kafka broker properties: " + e.getMessage());
				}
			}
		};
	}

	@After
	public void consumeTopic() throws Exception {
		LOG.debug("Entering @After: consumeTopic(), using consumerFactory to createConsumer()");
		try (Consumer<String, RMapEvent> consumer = consumerFactory.createConsumer()) {
			LOG.debug("@After: invoking kafkaBroker.consumeFromAnEmbeddedTopic ...");
//			kafkaBroker.consumeFromAnEmbeddedTopic(consumer, "rmap-event-topic");
			final CountDownLatch consumerLatch = new CountDownLatch(1);
			consumer.subscribe(singletonList("rmap-event-topic"), new ConsumerRebalanceListener() {
				@Override
				public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
					LOG.debug("Revoked partitions: [{}]", partitions
							.stream()
							.map(part -> String.format("topic: %s, partition: %s", part.topic(), part.partition()))
							.collect(joining(", ")));
				}

				@Override
				public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
					LOG.debug("Assigned partitions: [{}]", partitions
							.stream()
							.map(part -> String.format("topic: %s, partition: %s", part.topic(), part.partition()))
							.collect(joining(", ")));
					consumerLatch.countDown();
				}
			});

			long timeout = 10000;
			LOG.debug("Kafka consumer polling with a timeout of [{}]", timeout);
			consumer.poll(timeout).forEach(record -> LOG.debug(
					"Consumed from topic {}, partition {}, offset {}: [{}] = [{}]",
					record.topic(), record.partition(), record.offset(), record.key(), record.value()));
			assertThat(consumerLatch.await(30, TimeUnit.SECONDS))
					.as("Failed to be assigned partitions from the embedded topics")
					.isTrue();

//			consumer.
//
//			consumer.poll(timeout).forEach(record -> LOG.debug("Consumed [{}] = [{}]", record.key(), record.value()));
		}
	}

	@Before
	public void setupAgents() throws Exception {
		//create 2 test agents and corresponding requestAgents
		createSystemAgent();
		createSystemAgent2();
	}

	/**
	 * Removes all statements from triplestore to avoid interference between tests
	 * @throws Exception
	 */
	@After
	public void clearTriplestore() throws Exception {
		if (triplestore instanceof SesameSailMemoryTriplestore) {
			triplestore.getConnection().clear();
		}
	}

//	@ClassRule
//	@SuppressWarnings("serial")
//	public static KafkaEmbedded kafkaBroker() {
//		String topic = "rmap-event-topic";
//		LOG.debug("JUnit @Rule instantiating embedded Kafka broker [{}] on topic [{}]",
//				KafkaEmbedded.class.getName(), topic);
//		KafkaEmbedded embedded = new KafkaEmbedded(1, false, 2, topic);
//		LOG.debug("JUnit @Rule setting embedded Kafka broker property log.dirs: [{}]", System.getProperty("logs.dir"));
//		embedded.brokerProperties(new HashMap<String, String>() {
//			{
//				put("logs.dir", System.getProperty("logs.dir"));
//			}
//		});
//
//		LOG.debug("JUnit @Rule returning embedded Kafka broker instance [{}]", embedded);
//		kafkaBroker = embedded;
//		return embedded;
//	}
	
	
	/**
	 * Create generic sysagent and RequestAgent for general use using TestConstants. 
	 * @throws FileNotFoundException
	 * @throws RMapException
	 * @throws RMapDefectiveArgumentException
	 * @throws URISyntaxException
	 */
	protected void createSystemAgent() throws FileNotFoundException, RMapException, RMapDefectiveArgumentException, URISyntaxException{
		if (sysagent == null) {
			IRI AGENT_IRI = ORAdapter.getValueFactory().createIRI(TestConstants.SYSAGENT_ID);
			IRI ID_PROVIDER_IRI = ORAdapter.getValueFactory().createIRI(TestConstants.SYSAGENT_ID_PROVIDER);
			IRI AUTH_ID_IRI = ORAdapter.getValueFactory().createIRI(TestConstants.SYSAGENT_AUTH_ID);
			Literal NAME = ORAdapter.getValueFactory().createLiteral(TestConstants.SYSAGENT_NAME);	
			sysagent = new ORMapAgent(AGENT_IRI, ID_PROVIDER_IRI, AUTH_ID_IRI, NAME);
			
			if (requestAgent==null){
				requestAgent = new RMapRequestAgent(new URI(TestConstants.SYSAGENT_ID),new URI(TestConstants.SYSAGENT_KEY));
			}
			
			//create new test agent
			URI agentId=sysagent.getId().getIri();
			if (!rmapService.isAgentId(agentId)) {
				rmapService.createAgent(sysagent,requestAgent);
			}

			// Check the agent was created
			assertTrue(rmapService.isAgentId(agentId));		
		}
	}	

	/**
	 * Create second generic sysagent and RequestAgent for general use using TestConstants. 
	 * @throws RMapException
	 * @throws RMapDefectiveArgumentException
	 * @throws FileNotFoundException
	 * @throws URISyntaxException
	 */
	protected void createSystemAgent2() throws RMapException, RMapDefectiveArgumentException, FileNotFoundException, URISyntaxException{
		if (sysagent2 == null){
			//create new test agent #2
			IRI AGENT_IRI = ORAdapter.getValueFactory().createIRI(TestConstants.SYSAGENT2_ID);
			IRI ID_PROVIDER_IRI = ORAdapter.getValueFactory().createIRI(TestConstants.SYSAGENT_ID_PROVIDER);
			IRI AUTH_ID_IRI = ORAdapter.getValueFactory().createIRI(TestConstants.SYSAGENT2_AUTH_ID);
			Literal NAME = ORAdapter.getValueFactory().createLiteral(TestConstants.SYSAGENT2_NAME);	
			sysagent2 = new ORMapAgent(AGENT_IRI, ID_PROVIDER_IRI, AUTH_ID_IRI, NAME);
			
			if (requestAgent2==null){
				requestAgent2 = new RMapRequestAgent(new URI(TestConstants.SYSAGENT2_ID));
			}
			
			URI agentId=sysagent2.getId().getIri();
			if (!rmapService.isAgentId(agentId)) {
				rmapService.createAgent(sysagent2,requestAgent);
			}

			// Check the agent was created
			assertTrue(rmapService.isAgentId(agentId));		
		}
	}


	/**
	 * Retrieves a test DiSCO object
	 * @param testobj
	 * @return
	 * @throws FileNotFoundException
	 * @throws RMapException
	 * @throws RMapDefectiveArgumentException
	 */
	public static ORMapDiSCO getRMapDiSCO(TestFile testobj) throws FileNotFoundException, RMapException, RMapDefectiveArgumentException {
		InputStream stream = TestDataHandler.getTestData(testobj);
		RioRDFHandler handler = new RioRDFHandler();	
		Set<Statement>stmts = handler.convertRDFToStmtList(stream, RDFType.get(testobj.getType()), "");
		ORMapDiSCO disco = StatementsAdapter.asDisco(stmts,
				() -> create("http://example.org/disco/" + counter.getAndIncrement()));
		return disco;		
	}

	/**
	 * Retrieves a test Agent object
	 * @param testobj
	 * @return
	 * @throws FileNotFoundException
	 * @throws RMapException
	 * @throws RMapDefectiveArgumentException
	 */
	public static ORMapAgent getAgent(TestFile testobj) throws FileNotFoundException, RMapException, RMapDefectiveArgumentException {
		InputStream stream = TestDataHandler.getTestData(testobj);
		RioRDFHandler handler = new RioRDFHandler();	
		Set<Statement>stmts = handler.convertRDFToStmtList(stream, RDFType.get(testobj.getType()), "");
		ORMapAgent agent = StatementsAdapter.asAgent(stmts,
				() -> create("http://example.org/agent/" + counter.getAndIncrement()));
		return agent;		
	}
	
	
}
