package info.rmapproject.indexing.solr.repository;

import info.rmapproject.indexing.solr.model.DiscoSolrDocument;

import java.net.URI;
import java.util.Collections;
import java.util.stream.Collectors;

import static info.rmapproject.indexing.solr.IndexUtils.assertNotNull;
import static info.rmapproject.indexing.solr.IndexUtils.dateToString;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class SimpleIndexableThingMapper implements IndexableThingMapper {

    /**
     * Maps a {@link IndexableThing} to a {@link DiscoSolrDocument} field by field.  The mapping logic is tolerant with
     * respect to {@code null} values; no field validation is performed.  Any validation logic ought to exist outside of
     * this method.
     *
     * @param indexableThing the thing to be indexed
     * @return the {@code DiscoSolrDocument}
     */
    @Override
    public DiscoSolrDocument map(IndexableThing indexableThing) {
        assertNotNull(indexableThing, "The supplied object to index must not be null.");

        DiscoSolrDocument doc = new DiscoSolrDocument();

        // Fields mapped from indexableThing.disco

        if (notNull(indexableThing.disco)) {
            if (notNull(indexableThing.disco.getRelatedStatements())) {
                doc.setDiscoRelatedStatements(indexableThing.disco.getRelatedStatements().stream()
                        .map(t -> String.format(
                                "%s %s %s",
                                t.getSubject().getStringValue(),
                                t.getPredicate().getStringValue(),
                                t.getObject().getStringValue()))
                        .collect(Collectors.toList()));
            }

            if (notNull(indexableThing.disco.getId())) {
                doc.setDiscoUri(indexableThing.disco.getId().getStringValue());
            }

            if (notNull(indexableThing.disco.getCreator())) {
                // TODO: Resolve creator and index creator properties as well as the creator URI?
                doc.setDiscoCreatorUri(indexableThing.disco.getCreator().getStringValue());
            }

            if (notNull(indexableThing.disco.getAggregatedResources())) {
                doc.setDiscoAggregatedResourceUris(indexableThing.disco.getAggregatedResources()
                        .stream().map(URI::toString).collect(Collectors.toList()));
            }

            if (notNull(indexableThing.disco.getDescription())) {
                doc.setDiscoDescription(indexableThing.disco.getDescription().getStringValue());
            }

            if (notNull(indexableThing.disco.getProvGeneratedBy())) {
                doc.setDiscoProvenanceUri(indexableThing.disco.getProvGeneratedBy().getStringValue());
            }
        }

        // Fields mapped from indexableThing.status

        if (notNull(indexableThing.status)) {
            doc.setDiscoStatus(indexableThing.status.toString());
        }

        // Fields mapped from indexableThing.agent

        if (notNull(indexableThing.agent)) {
            if (notNull(indexableThing.agent.getId())) {
                doc.setAgentUri(indexableThing.agent.getId().getStringValue());
            }
            if (notNull(indexableThing.agent.getName())) {
                doc.setAgentDescription(indexableThing.agent.getName().getStringValue());
            }
            if (notNull(indexableThing.agent.getIdProvider())) {
                doc.setAgentProviderUri(indexableThing.agent.getIdProvider().getStringValue());
            }
            // TODO? toIndex.agent.getAuthId()
        }

        // Fields mapped from indexableThing.event

        if (notNull(indexableThing.event)) {
            if (notNull(indexableThing.event.getId())) {
                doc.setEventUri(indexableThing.event.getId().getStringValue());
            }
            if (notNull(indexableThing.event.getAssociatedAgent())) {
                doc.setEventAgentUri(indexableThing.event.getAssociatedAgent().getStringValue());
            }
            if (notNull(indexableThing.event.getDescription())) {
                doc.setEventDescription(indexableThing.event.getDescription().getStringValue());
            }
            if (notNull(indexableThing.event.getStartTime())) {
                doc.setEventStartTime(dateToString(indexableThing.event.getStartTime()));
            }
            if (notNull(indexableThing.event.getEndTime())) {
                doc.setEventEndTime(dateToString(indexableThing.event.getEndTime()));
            }
            if (notNull(indexableThing.event.getEventType())) {
                doc.setEventType(indexableThing.event.getEventType().name());
            }

            // Fields mapped from eventSource and eventTarget

            if (notNull(indexableThing.eventTarget)) {
                doc.setEventTargetObjectUris(Collections.singletonList(indexableThing.eventTarget.getStringValue()));
            }
            if (notNull(indexableThing.eventSource)) {
                doc.setEventSourceObjectUris(Collections.singletonList(indexableThing.eventSource.getStringValue()));
            }
        }

        return doc;
    }

    private static boolean notNull(Object o) {
        return o != null;
    }

}
