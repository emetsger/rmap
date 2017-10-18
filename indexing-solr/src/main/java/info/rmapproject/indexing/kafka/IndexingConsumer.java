package info.rmapproject.indexing.kafka;

import info.rmapproject.core.model.RMapIri;
import info.rmapproject.core.model.agent.RMapAgent;
import info.rmapproject.core.model.disco.RMapDiSCO;
import info.rmapproject.core.model.event.RMapEvent;
import info.rmapproject.core.rmapservice.RMapService;
import info.rmapproject.indexing.IndexUtils;
import info.rmapproject.indexing.IndexingInterruptedException;
import info.rmapproject.indexing.IndexingTimeoutException;
import info.rmapproject.indexing.solr.repository.CustomRepo;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.InterruptException;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static info.rmapproject.indexing.IndexUtils.EventDirection.SOURCE;
import static info.rmapproject.indexing.IndexUtils.EventDirection.TARGET;
import static info.rmapproject.indexing.IndexUtils.findEventIri;
import static info.rmapproject.indexing.IndexUtils.iae;
import static info.rmapproject.indexing.IndexUtils.ise;
import static info.rmapproject.indexing.kafka.KafkaUtils.commitOffsets;
import static java.util.Collections.singleton;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class IndexingConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(IndexingConsumer.class);

    private static final ConsumerRecords<String, RMapEvent> EMPTY_RECORDS =
            new ConsumerRecords<>(Collections.emptyMap());

    @Autowired
    private RMapService rmapService;

    private CustomRepo repo;

    private Consumer<String, RMapEvent> consumer;

    private int pollTimeoutMs;

    private Thread shutdownHook;

    private ConsumerAwareRebalanceListener<String, RMapEvent> rebalanceListener;

    private IndexingRetryHandler retryHandler;

    private OffsetLookup offsetLookup;

    void consumeLatest(String topic) throws UnknownOffsetException {
        consume(topic, Seek.LATEST);
    }

    void consumeEarliest(String topic) throws UnknownOffsetException {
        consume(topic, Seek.EARLIEST);
    }

    void consume(String topic, Seek seek) throws UnknownOffsetException {
        IndexUtils.assertNotNull(consumer, ise("Consumer must not be null."));
        IndexUtils.assertNotNullOrEmpty(topic, "Topic must not be null or empty");
        IndexUtils.assertNotNull(seek, ise("Seek must not be null."));

        rebalanceListener.setConsumer(consumer);
        consumer.subscribe(singleton(topic), rebalanceListener);
        consumer.poll(0); // join consumer group, get partitions, seek to correct offset

        Map<TopicPartition, OffsetAndMetadata> offsetsToCommit = new HashMap<>(1);

        while (true) {

            offsetsToCommit.clear();
            ConsumerRecords<String, RMapEvent> records = null;

            try {
                LOG.trace("Entering poll for {} ms", pollTimeoutMs);
                records = consumer.poll(pollTimeoutMs);
            } catch (WakeupException e) {
                LOG.info("WakeupException encountered, closing consumer.");
                consumer.close();
                break;
            } catch (InterruptException e) {
                LOG.info("InterruptException encountered, exiting consumer.poll({}) early.", pollTimeoutMs);
                Thread.interrupted();
                // guard against null records
                records = EMPTY_RECORDS;
            }

            LOG.trace("Processing {} records", records.count());
            records.forEach(record -> {
                RMapEvent event = record.value();
                String recordTopic = record.topic();
                long recordOffset = record.offset();
                int recordPartition = record.partition();

                LOG.trace("Processing record {}/{}/{} for event: {}", recordTopic, recordPartition, recordOffset, event);
                processEventRecord(recordTopic, recordPartition, recordOffset, event);

                offsetsToCommit.put(new TopicPartition(recordTopic, recordPartition),
                        new OffsetAndMetadata(recordOffset));
            });

            LOG.trace("Committing offsets {}", KafkaUtils.offsetsAsString(offsetsToCommit));
            commitOffsets(consumer, offsetsToCommit, true);

        }
    }

    private void processEventRecord(String recordTopic, int recordPartition, long recordOffset, RMapEvent event) {
        KafkaDTO dto = composeDTO(event, rmapService);

        // Store offsets in the index
        dto.setTopic(recordTopic);
        dto.setPartition(recordPartition);
        dto.setOffset(recordOffset);

        try {
            repo.index(dto);
            LOG.debug("Indexed event {} ({}/{}/{})",
                    event.getId().getStringValue(),
                    recordTopic,
                    recordPartition,
                    recordOffset);
        } catch (Exception e) {
            LOG.debug("Retrying indexing operation for event {} ({}/{}/{}): {}",
                    event.getId().getStringValue(),
                    recordTopic,
                    recordPartition,
                    recordOffset,
                    e.getMessage(),
                    e);

            // in this case we don't want to commit the offset to Kafka until we've successfully retried
            try {
                retryHandler.retry(dto);
            } catch (IndexingTimeoutException|IndexingInterruptedException ex) {
                throw new RuntimeException(
                        "Failed to index event " + event.getId().getStringValue(), ex);
            }
        }
    }

    private KafkaDTO composeDTO(RMapEvent event, RMapService rmapService) {
        RMapDiSCO sourceDisco = getDisco(findEventIri(event, SOURCE).get(), rmapService);
        RMapDiSCO targetDisco = getDisco(findEventIri(event, TARGET).get(), rmapService);
        RMapAgent agent = getAgent(event.getAssociatedAgent().getIri(), rmapService);

        return new KafkaDTO(event, agent, sourceDisco, targetDisco);
    }

    private static RMapDiSCO getDisco(RMapIri optionalIri, RMapService rmapService) {
        RMapDiSCO disco = null;
        if (optionalIri != null) {
            disco = rmapService.readDiSCO(optionalIri.getIri());
        }

        return disco;
    }

    private static RMapAgent getAgent(URI agentUri, RMapService rmapService) {
        return rmapService.readAgent(agentUri);
    }

    public RMapService getRmapService() {
        return rmapService;
    }

    public void setRmapService(RMapService rmapService) {
        this.rmapService = rmapService;
    }

    public CustomRepo getRepo() {
        return repo;
    }

    public void setRepo(CustomRepo repo) {
        this.repo = repo;
    }

    public Consumer<String, RMapEvent> getConsumer() {
        return consumer;
    }

    public void setConsumer(Consumer<String, RMapEvent> consumer) {
        this.consumer = consumer;
    }

    public int getPollTimeoutMs() {
        return pollTimeoutMs;
    }

    public void setPollTimeoutMs(int pollTimeoutMs) {
        this.pollTimeoutMs = pollTimeoutMs;
    }

    public Thread getShutdownHook() {
        return shutdownHook;
    }

    public void setShutdownHook(Thread shutdownHook) {
        this.shutdownHook = shutdownHook;
        Runtime.getRuntime().addShutdownHook(shutdownHook);
    }

    public ConsumerRebalanceListener getRebalanceListener() {
        return rebalanceListener;
    }

    public void setRebalanceListener(ConsumerAwareRebalanceListener<String, RMapEvent> rebalanceListener) {
        this.rebalanceListener = rebalanceListener;
    }

    public IndexingRetryHandler getRetryHandler() {
        return retryHandler;
    }

    public void setRetryHandler(IndexingRetryHandler retryHandler) {
        this.retryHandler = retryHandler;
    }

    public OffsetLookup getOffsetLookup() {
        return offsetLookup;
    }

    public void setOffsetLookup(OffsetLookup offsetLookup) {
        this.offsetLookup = offsetLookup;
    }
}
