package info.rmapproject.indexing.kafka;

import info.rmapproject.auth.service.RMapAuthService;
import info.rmapproject.core.exception.RMapDefectiveArgumentException;
import info.rmapproject.core.exception.RMapException;
import info.rmapproject.core.model.RMapObjectType;
import info.rmapproject.core.model.agent.RMapAgent;
import info.rmapproject.core.model.event.RMapEvent;
import info.rmapproject.core.model.impl.openrdf.ORAdapter;
import info.rmapproject.core.model.impl.openrdf.ORMapAgent;
import info.rmapproject.core.model.request.RequestEventDetails;
import info.rmapproject.core.rmapservice.RMapService;
import info.rmapproject.core.rmapservice.impl.openrdf.triplestore.SesameTriplestore;
import info.rmapproject.indexing.solr.AbstractSpringIndexingTest;
import info.rmapproject.indexing.solr.TestUtils;
import info.rmapproject.indexing.solr.repository.DiscoRepository;
import info.rmapproject.kafka.shared.SpringKafkaConsumerFactory;
import info.rmapproject.testdata.service.TestConstants;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.TopicPartition;
import org.junit.Assert;
import org.junit.Test;
import org.openrdf.model.IRI;
import org.openrdf.model.Literal;
import org.openrdf.model.Statement;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.rio.RDFFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static info.rmapproject.indexing.solr.TestUtils.getRmapObjects;
import static info.rmapproject.indexing.solr.TestUtils.getRmapResources;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ContextConfiguration("classpath:/spring-rmapauth-context.xml")
@ActiveProfiles(value = {"default", "inmemory-triplestore", "inmemory-idservice", "inmemory-db", "http-solr", "prod-kafka"}, inheritProfiles = false)
public class SaveOffsetOnRebalanceIT extends AbstractSpringIndexingTest {

    @Autowired
    private ApplicationContext ctx;

    @Autowired
    private IndexingConsumer indexer;

    @Autowired
    private OffsetLookup lookup;

    @Autowired
    private ConsumerAwareRebalanceListener<String, RMapEvent> underTest;

    @Autowired
    private KafkaTemplate<String, RMapEvent> producer;

    @Autowired
    private DiscoRepository discoRepository;

    @Autowired
    private RMapService rMapService;

    @Autowired
    private RMapAuthService authService;

    @Autowired
    private SesameTriplestore triplestore;

    @Value("${rmapcore.producer.topic}")
    private String topic;

    /**
     * Arguably a consumer test.  Insures that the rebalancer methods are invoked when a consumer joins.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testPartitionsRevokedAndAssignedInvokedOnConsumerJoin() throws UnknownOffsetException, InterruptedException {
        CountDownLatch initialLatch2 = new CountDownLatch(2);
        CountDownLatch initialLatch4 = new CountDownLatch(4);

        indexer.setRebalanceListener(new ConsumerAwareRebalanceListener<String, RMapEvent>() {
            @Override
            public void setConsumer(Consumer<String, RMapEvent> consumer) {
                // no-op
            }

            @Override
            public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
                LOG.debug("Initial Consumer: Partitions Revoked {}", KafkaUtils.topicPartitionsAsString(partitions));
                initialLatch2.countDown();
                initialLatch4.countDown();
            }

            @Override
            public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
                LOG.debug("Initial Consumer: Partitions Assigned {}", KafkaUtils.topicPartitionsAsString(partitions));
                initialLatch2.countDown();
                initialLatch4.countDown();
            }
        });

        AtomicReference<Exception> exceptionHolder = new AtomicReference<>();
        Thread t = new Thread(newConsumerRunnable(exceptionHolder), "testPartitionsRevokedAndAssignedOnConsumerJoin-consumer");

        t.start();

        // rebalancer should be called when the consumer starts.
        assertTrue(initialLatch2.await(60000, TimeUnit.MILLISECONDS));

        CountDownLatch secondaryLatch2 = new CountDownLatch(2);
        Consumer secondaryConsumer = SpringKafkaConsumerFactory.newConsumer("-02");
        secondaryConsumer.subscribe(Collections.singleton(topic), new ConsumerAwareRebalanceListener<String, RMapEvent>() {
            @Override
            public void setConsumer(Consumer<String, RMapEvent> consumer) {
                // no-op
            }

            @Override
            public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
                LOG.debug("Secondary Consumer: Partitions Revoked {}", KafkaUtils.topicPartitionsAsString(partitions));
                secondaryLatch2.countDown();
            }

            @Override
            public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
                LOG.debug("Secondary Consumer: Partitions Assigned {}", KafkaUtils.topicPartitionsAsString(partitions));
                secondaryLatch2.countDown();
            }
        });

        // Fire up a second consumer, and invoke poll so it gets its partitions assigned.  the rebalancer should be
        // invoked for both consumers
        secondaryConsumer.poll(0);
        t.interrupt();  // short-circuit any polling in the initial consumer, speed things up.

        // the initial rebalancer should have its methods invoked a total of four times
        assertTrue(initialLatch4.await(60000, TimeUnit.MILLISECONDS));

        // the second rebalancer should have its methods invoked a total of two times
        assertTrue(secondaryLatch2.await(60000, TimeUnit.MILLISECONDS));

        // cleanup.  wakeup causes the initial indexer close its consumer and exit
        LOG.debug("Waking up initial consumer.");
        indexer.getConsumer().wakeup();
        LOG.debug("Thread joining.");
        t.join();
        LOG.debug("Closing secondary consumer.");
        secondaryConsumer.close();
        assertNull("Consumer threw an unexpected exception: " + exceptionHolder.get().getMessage(),
                exceptionHolder.get());
    }

    /**
     * Arguably a consumer test.  Insures that the rebalancer methods are invoked when a consumer joins.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void testPartitionsRevokedAndAssignedInvokedOnStart() throws UnknownOffsetException, InterruptedException {
        ConsumerAwareRebalanceListener underTest = mock(ConsumerAwareRebalanceListener.class);
        indexer.setRebalanceListener(underTest);

        AtomicReference<Exception> exceptionHolder = new AtomicReference<>();
        Thread t = new Thread(newConsumerRunnable(exceptionHolder), "testPartitionsRevokedAndAssignedOnStart-consumer");
        t.start();

        // allow thread to run a bit
        Thread.sleep(5000);

        LOG.debug("Waking up consumer.");
        indexer.getConsumer().wakeup();

        LOG.debug("Thread joining.");
        t.join();

        assertNull("Consumer threw an unexpected exception: " + exceptionHolder.get().getMessage(),
                exceptionHolder.get());

        verify(underTest).onPartitionsRevoked(Collections.emptySet());
        verify(underTest).onPartitionsAssigned(new HashSet() {
            {
                add(new TopicPartition(topic, 0));
                add(new TopicPartition(topic, 1));
            }
        });


    }

    @Test
    public void testRebalance() throws Exception {
        // Clear out the index
        discoRepository.deleteAll();
        assertEquals(0, discoRepository.count());

        // Get some Rmap objects from the filesystem, and put them in the triplestore
        Map<RMapObjectType, Set<TestUtils.RDFResource>> rmapObjects = new HashMap<>();
        getRmapResources("/data/discos/rmd18mddcw", rdfHandler, RDFFormat.NQUADS, rmapObjects);
        assertFalse(rmapObjects.isEmpty());

        RMapAgent systemAgent = createSystemAgent(rMapService);
        RequestEventDetails requestEventDetails = new RequestEventDetails(systemAgent.getId().getIri());

        List<RMapAgent> agents = getRmapObjects(rmapObjects, RMapObjectType.AGENT, rdfHandler);
        assertNotNull(agents);
        assertTrue(agents.size() > 0);
        LOG.debug("Creating {} agents", agents.size());
        agents.forEach(agent -> {
//            User u = new User(agent.getName().getStringValue(), "foo@bar.baz");
//            u.setRmapAgentUri(agent.getId().getStringValue());
//            u.setUserIdentityProviders(Collections.singleton(new UserIdentityProvider(agent.getIdProvider()));
//            int userId = 0;
//            try (RepositoryConnection c = triplestore.getConnection()) {
//                userId = authService.addUser(u);
//                RMapEvent created = authService.createOrUpdateAgentFromUser(userId);
//                assertNotNull("Expected a creation event when creating an agent with uri " + ((agent.getIdProvider() != null) ? agent.getIdProvider().getStringValue() : "null") + " for user id " + userId, created);
//                LOG.debug("Created Agent {}", created);
//            } catch (Exception e) {
//                LOG.error("Error adding user {} id {}: {}", agent.getName().getStringValue(), userId, e.getMessage(), e);
//            }

//            LOG.debug("Looking up agent {}", agent.getId().getIri());
//            if (rMapService.readAgent(agent.getId().getIri()) == null) {
//                LOG.debug("Creating RMap Agent {}", agent.getId().getIri());
//                rMapService.createAgent(agent, requestEventDetails);
//            } else {
//                LOG.debug("RMap Agent {} already existed", agent.getId().getIri());
//            }

            try {
                rMapService.createAgent(agent, requestEventDetails);
            } catch (Exception e) {
                LOG.debug("Error creating agent {}: {}", agent.getId().getStringValue(), e.getMessage(), e);
            }
        });

        // Print out the triplestore contents to stderr
        System.err.println("Dump one:");
        dumpTriplestore(new PrintStream(System.err, true));

        rmapObjects.values().stream().flatMap(Set::stream)
                .forEach(source -> {
            try (InputStream in = source.getInputStream();
                 RepositoryConnection c = triplestore.getConnection();
            ) {
                assertTrue(c.isOpen());
                c.add(in, "http://foo/bar", source.getRdfFormat());
            } catch (IOException e) {
                e.printStackTrace(System.err);
                fail("Unexpected IOException");
            }
        });

        System.err.println("Dump two:");
        dumpTriplestore(new PrintStream(System.err, true));

        LOG.debug("Producing events.");
        // Produce some events, so they're waiting for the consumer when it starts.
        List<RMapEvent> events = getRmapObjects(rmapObjects, RMapObjectType.EVENT, rdfHandler);
        events.forEach(event -> producer.send(topic, event));
        producer.flush();

        LOG.debug("Starting indexer.");
        AtomicReference<Exception> exceptionHolder = new AtomicReference<>();
        // Boot up the first indexing consumer, and consume some events.
        Thread initialIndexerThread = new Thread(newConsumerRunnable(exceptionHolder), "Initial Indexer");
        initialIndexerThread.start();
        Thread.sleep(30000);

        // clean up
        indexer.getConsumer().wakeup();
        initialIndexerThread.join();

        assertExceptionHolderEmpty(exceptionHolder);
    }

    /**
     * Asserts the supplied {@code exceptionHolder} is empty, otherwise {@link Assert#fail(String) fail} the test,
     * emitting the stacktrace in the failure message.
     *
     * @param exceptionHolder an {@code AtomicReference} which may hold an {@code Exception}
     */
    private void assertExceptionHolderEmpty(AtomicReference<Exception> exceptionHolder) {
        if (exceptionHolder.get() == null) {
            return;
        }

        ByteArrayOutputStream trace = new ByteArrayOutputStream();
        exceptionHolder.get().printStackTrace(new PrintStream(trace, true));
        fail("Consumer threw an unexpected exception: \n" + trace);
    }

    private Runnable newConsumerRunnable(AtomicReference<Exception> caughtException) {
        return newConsumerRunnable(this.indexer, caughtException);
    }

    private Runnable newConsumerRunnable(IndexingConsumer indexer, AtomicReference<Exception> caughtExeption) {
        return () -> {
            try {
                indexer.consumeEarliest(topic);
            } catch (Exception e) {
                caughtExeption.set(e);
            }
        };
    }

    /**
     * Instantiate an {@link ORMapAgent} to represent a System agent, and use the {@link RMapService} to create the
     * agent.  Verifies the agent was created using {@link RMapService#isAgentId(URI)}
     *
     * @param rmapService used to create the agent in the underlying triplestore
     * @throws RMapException
     * @throws RMapDefectiveArgumentException
     * @throws URISyntaxException
     */
    private RMapAgent createSystemAgent(RMapService rmapService) throws RMapException, RMapDefectiveArgumentException, URISyntaxException {
        IRI AGENT_IRI = ORAdapter.getValueFactory().createIRI(TestConstants.SYSAGENT_ID);
        IRI ID_PROVIDER_IRI = ORAdapter.getValueFactory().createIRI(TestConstants.SYSAGENT_ID_PROVIDER);
        IRI AUTH_ID_IRI = ORAdapter.getValueFactory().createIRI(TestConstants.SYSAGENT_AUTH_ID);
        Literal NAME = ORAdapter.getValueFactory().createLiteral(TestConstants.SYSAGENT_NAME);
        RMapAgent sysagent = new ORMapAgent(AGENT_IRI, ID_PROVIDER_IRI, AUTH_ID_IRI, NAME);

        RequestEventDetails requestEventDetails = new RequestEventDetails(new URI(TestConstants.SYSAGENT_ID), new URI(TestConstants.SYSAGENT_KEY));

        //create new test agent
        URI agentId = sysagent.getId().getIri();
        if (!rmapService.isAgentId(agentId)) {
            rmapService.createAgent(sysagent, requestEventDetails);
        }

        // Check the agent was created
        assertTrue(rmapService.isAgentId(agentId));

        return sysagent;
    }

    /**
     * Dumps the contents of the triplestore to the provided output stream.
     *
     * @param outputStream
     * @throws Exception
     */
    private void dumpTriplestore(OutputStream outputStream) throws Exception {
        List<Statement> statements = triplestore.getStatementListBySPARQL("select ?s ?p ?o ?c where {GRAPH ?c {?s ?p ?o}}");
        statements.forEach(
                statement -> {
                    try {
                        outputStream.write(statement.toString().getBytes("UTF-8"));
                        outputStream.write("\n".getBytes());
                    } catch (IOException e) {
                        throw new RuntimeException(e.getMessage(), e);
                    }
                }
        );
    }
}