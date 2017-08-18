package info.rmapproject.indexing.solr.repository;

import info.rmapproject.core.model.RMapIri;
import info.rmapproject.core.model.agent.RMapAgent;
import info.rmapproject.core.model.event.RMapEvent;
import info.rmapproject.core.model.event.RMapEventType;
import info.rmapproject.indexing.solr.IndexUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;
import java.util.stream.Stream;

import static info.rmapproject.indexing.solr.IndexUtils.assertNotNull;
import static info.rmapproject.indexing.solr.IndexUtils.irisEqual;
import static info.rmapproject.indexing.solr.IndexUtils.ise;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class SimpleIndexDTOMapper implements IndexDTOMapper {

    private static final String ERR_INFER_STATUS = "Unable to infer the status for RMap DiSCO: %s Event: %s: Agent: %s";

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    
    @Autowired
    private StatusInferencer inferencer;

    public SimpleIndexDTOMapper() {

    }

    public SimpleIndexDTOMapper(StatusInferencer inferencer) {
        assertNotNull(inferencer, "StatusInferencer must not be null.");
        this.inferencer = inferencer;
    }


    @Override
    public Stream<IndexableThing> apply(IndexDTO indexDTO) {
        assertNotNull(indexDTO, "The supplied IndexDTO must not be null.");

        RMapEvent event = assertNotNull(indexDTO.getEvent(), ise("The IndexDTO must not have a null event."));
        RMapAgent agent = assertNotNull(indexDTO.getAgent(), ise("The IndexDTO must not have a null agent."));

        if (!(irisEqual(event.getAssociatedAgent(), agent.getId()))) {
            throw new RuntimeException(String.format(
                    "Missing agent '%s' of event %s", event.getAssociatedAgent().getStringValue(), event));
        }

        // The source IRI will be null in the case of a creation event
        RMapIri source = indexDTO.getEventSourceIri();
        // The target IRI will be null in the case of a delete, tombstone, or inactivation event
        RMapIri target = indexDTO.getEventTargetIri();

        // We do not index the source DiSCO for DERIVATION events; the source of a DERIVATION event does not need to
        // be updated at all in the index. (TODO: version repository implications)
        IndexableThing forSource = null;
        if (source != null && event.getEventType() != RMapEventType.DERIVATION) {
            forSource = new IndexableThing();

            forSource.eventSource = source;
            forSource.eventTarget = target;
            forSource.event = event;
            forSource.agent = agent;
            forSource.disco = indexDTO.getSourceDisco();
            forSource.status = inferencer.inferDiscoStatus(forSource.disco, forSource.event, forSource.agent)
                    .orElseThrow(() -> new RuntimeException(
                            String.format(ERR_INFER_STATUS, indexDTO.getSourceDisco().getId(),
                                    indexDTO.getEvent().getId(), indexDTO.getAgent().getId())));
        }

        IndexableThing forTarget = null;
        if (target != null) {
            forTarget = new IndexableThing();
            forTarget.eventSource = source;
            forTarget.eventTarget = target;
            forTarget.event = event;
            forTarget.agent = agent;
            forTarget.disco = indexDTO.getTargetDisco();
            forTarget.status = inferencer.inferDiscoStatus(forTarget.disco, forTarget.event, forTarget.agent)
                    .orElseThrow(() -> new RuntimeException(
                            String.format(ERR_INFER_STATUS, indexDTO.getTargetDisco().getId(),
                                    indexDTO.getEvent().getId(), indexDTO.getAgent().getId())));
        }

        Stream.Builder<IndexableThing> builder = Stream.builder();

        if (forSource != null) {
            builder.accept(forSource);
        }

        if (forTarget != null) {
            builder.accept(forTarget);
        }

        return builder.build();

    }

    StatusInferencer getInferencer() {
        return inferencer;
    }

    void setInferencer(StatusInferencer inferencer) {
        this.inferencer = inferencer;
    }
}
