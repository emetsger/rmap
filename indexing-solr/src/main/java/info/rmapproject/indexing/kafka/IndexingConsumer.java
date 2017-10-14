package info.rmapproject.indexing.kafka;

import info.rmapproject.core.model.RMapIri;
import info.rmapproject.core.model.agent.RMapAgent;
import info.rmapproject.core.model.disco.RMapDiSCO;
import info.rmapproject.core.model.event.RMapEvent;
import info.rmapproject.core.rmapservice.RMapService;
import info.rmapproject.indexing.IndexingTimeoutException;
import info.rmapproject.indexing.solr.repository.CustomRepo;
import info.rmapproject.indexing.solr.repository.IndexDTO;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static info.rmapproject.indexing.IndexUtils.EventDirection.SOURCE;
import static info.rmapproject.indexing.IndexUtils.EventDirection.TARGET;
import static info.rmapproject.indexing.IndexUtils.findEventIri;
import static info.rmapproject.indexing.IndexUtils.ise;
import static java.util.Collections.singleton;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class IndexingConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(IndexingConsumer.class);

    @Autowired
    private RMapService rmapService;

    private CustomRepo repo;

    private Consumer<String, RMapEvent> consumer;

    private String topic;

    private int pollTimeoutMs;

    private Thread shutdownHook;

    private ConsumerAwareRebalanceListener<String, RMapEvent> rebalanceListener;

    private IndexingRetryHandler retryHandler;

    void consume() {
        consume(topic,
                consumer.assignment().stream().filter(tp -> tp.topic().equals(topic)).findAny()
                        .orElseThrow(ise("Missing expected TopicPartition for topic " + topic)).partition(),
                Integer.MIN_VALUE); // TODO: look up current offset from index
    }

    void consume(String consumeFromTopic, int consumeFromPartition, long startingFromOffset) {

        rebalanceListener.setConsumer(consumer);
        consumer.subscribe(singleton(consumeFromTopic), rebalanceListener);

        consumer.poll(0); // join consumer group, and get partitions
        consumer.seek(new TopicPartition(consumeFromTopic, consumeFromPartition), startingFromOffset);

        Map<TopicPartition, OffsetAndMetadata> offsetsToCommit = new HashMap<>(1);

        while (true) {

            offsetsToCommit.clear();
            ConsumerRecords<String, RMapEvent> records = null;

            try {
                records = consumer.poll(pollTimeoutMs);
            } catch (WakeupException e) {
                LOG.info("WakeupException encountered, closing consumer.");
                consumer.close();
                break;
            }

            records.forEach(record -> {
                RMapEvent event = record.value();
                String recordTopic = record.topic();
                long recordOffset = record.offset();
                int recordPartition = record.partition();

                processEventRecord(recordTopic, recordPartition, recordOffset, event);

                offsetsToCommit.put(new TopicPartition(recordTopic, recordPartition),
                        new OffsetAndMetadata(recordOffset));
            });

            commitOffsets(offsetsToCommit);

        }
    }

    private void commitOffsets(Map<TopicPartition, OffsetAndMetadata> offsetsToCommit) {
        consumer.commitAsync(offsetsToCommit, (offsets, exception) -> {
            if (exception != null) {
                LOG.warn("Unable to commit offsets {}: {}",
                        offsetsAsString(offsets),
                        exception.getMessage(),
                        exception);
            } else {
                LOG.debug("Successfully committed offsets {}",
                        offsetsAsString(offsets));
            }
        });
    }

    private void processEventRecord(String recordTopic, int recordPartition, long recordOffset, RMapEvent event) {
        // Store offsets in the index
        Map<String, String> md = new HashMap<String, String>() {
            {
                put("md_kafka_topic", recordTopic);
                put("md_kafka_partition", String.valueOf(recordPartition));
                put("md_kafka_offset", String.valueOf(recordOffset));
            }
        };

        IndexDTO dto = composeDTO(event, rmapService);
        dto.setMetadata(md);

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
            } catch (IndexingTimeoutException timeoutEx) {
                throw new RuntimeException(
                        "Failed to index event " + event.getId().getStringValue(), timeoutEx);
            }
        }
    }

    private static String offsetsAsString(Map<TopicPartition, OffsetAndMetadata> offsets) {
        return offsets
                .entrySet()
                .stream()
                .map((entry) -> entry.getKey().toString() + ": " + String.valueOf(entry.getValue().offset()))
                .collect(Collectors.joining(", "));
    }

    private IndexDTO composeDTO(RMapEvent event, RMapService rmapService) {
        RMapDiSCO sourceDisco = getDisco(findEventIri(event, SOURCE).get(), rmapService);
        RMapDiSCO targetDisco = getDisco(findEventIri(event, TARGET).get(), rmapService);
        RMapAgent agent = getAgent(event.getAssociatedAgent().getIri(), rmapService);

        return new IndexDTO(event, agent, sourceDisco, targetDisco);
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

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
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
}
