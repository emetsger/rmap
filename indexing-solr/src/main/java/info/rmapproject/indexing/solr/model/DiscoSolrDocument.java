package info.rmapproject.indexing.solr.model;

import org.apache.solr.client.solrj.beans.Field;
import org.springframework.data.annotation.Id;
import org.springframework.data.solr.core.mapping.SolrDocument;

import java.util.Calendar;
import java.util.List;

import static info.rmapproject.indexing.solr.model.ModelUtils.assertValidUri;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
@SolrDocument(solrCoreName = "discos")
public class DiscoSolrDocument {

    @Id
    @Field("disco_id")
    private Long discoId;

    @Field("disco_uri")
    private String discoUri;

    @Field("disco_creator_uri")
    private String discoCreatorUri;

    @Field("disco_description")
    private String discoDescription;

    @Field("disco_providerid")
    private String discoProviderid;

    @Field("disco_aggregated_resource_uris")
    private List<String> discoAggregatedResourceUris;

    @Field("disco_provenance_uri")
    private String discoProvenanceUri;

    @Field("disco_related_statements")
    private List<String> discoRelatedStatements;


    @Field("event_uri")
    private String eventUri;

    @Field("event_agent_uri")
    private String eventAgentUri;

    @Field("event_start_time")
    private String eventStartTime;

    @Field("event_end_time")
    private String eventEndTime;

    @Field("event_description")
    private String eventDescription;

    @Field("event_type")
    private String eventType;

    @Field("event_source_object_uris")
    private List<String> eventSourceObjectUris;

    @Field("event_target_object_uris")
    private List<String> eventTargetObjectUris;


    @Field("agent_uri")
    private String agentUri;

    @Field("agent_provider_uri")
    private String agentProviderUri;

    @Field("agent_description")
    private String agentDescription;

    public DiscoSolrDocument() {

    }

    public Long getDiscoId() {
        return discoId;
    }

    public void setDiscoId(Long discoId) {
        this.discoId = discoId;
    }

    public String getDiscoUri() {
        return discoUri;
    }

    public void setDiscoUri(String discoUri) {
        assertValidUri(discoUri);
        this.discoUri = discoUri;
    }

    public String getDiscoCreatorUri() {
        return discoCreatorUri;
    }

    public void setDiscoCreatorUri(String discoCreatorUri) {
        assertValidUri(discoCreatorUri);
        this.discoCreatorUri = discoCreatorUri;
    }

    public String getDiscoDescription() {
        return discoDescription;
    }

    public void setDiscoDescription(String discoDescription) {
        this.discoDescription = discoDescription;
    }

    public String getDiscoProviderid() {
        return discoProviderid;
    }

    public void setDiscoProviderid(String discoProviderid) {
        assertValidUri(discoProviderid);
        this.discoProviderid = discoProviderid;
    }

    public List<String> getDiscoAggregatedResourceUris() {
        return discoAggregatedResourceUris;
    }

    public void setDiscoAggregatedResourceUris(List<String> discoAggregatedResourceUris) {
        assertValidUri(discoAggregatedResourceUris);
        this.discoAggregatedResourceUris = discoAggregatedResourceUris;
    }

    public String getDiscoProvenanceUri() {
        return discoProvenanceUri;
    }

    public void setDiscoProvenanceUri(String discoProvenanceUri) {
        assertValidUri(discoProvenanceUri);
        this.discoProvenanceUri = discoProvenanceUri;
    }

    public List<String> getDiscoRelatedStatements() {
        return discoRelatedStatements;
    }

    public void setDiscoRelatedStatements(List<String> discoRelatedStatements) {
        this.discoRelatedStatements = discoRelatedStatements;
    }

    public String getEventUri() {
        return eventUri;
    }

    public void setEventUri(String eventUri) {
        assertValidUri(eventUri);
        this.eventUri = eventUri;
    }

    public String getEventAgentUri() {
        return eventAgentUri;
    }

    public void setEventAgentUri(String eventAgentUri) {
        assertValidUri(eventAgentUri);
        this.eventAgentUri = eventAgentUri;
    }

    public String getEventStartTime() {
        return eventStartTime;
    }

    public void setEventStartTime(String eventStartTime) {
        this.eventStartTime = eventStartTime;
    }

    public String getEventEndTime() {
        return eventEndTime;
    }

    public void setEventEndTime(String eventEndTime) {
        this.eventEndTime = eventEndTime;
    }

    public String getEventDescription() {
        return eventDescription;
    }

    public void setEventDescription(String eventDescription) {
        this.eventDescription = eventDescription;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public List<String> getEventSourceObjectUris() {
        return eventSourceObjectUris;
    }

    public void setEventSourceObjectUris(List<String> eventSourceObjectUris) {
        assertValidUri(eventSourceObjectUris);
        this.eventSourceObjectUris = eventSourceObjectUris;
    }

    public List<String> getEventTargetObjectUris() {
        return eventTargetObjectUris;
    }

    public void setEventTargetObjectUris(List<String> eventTargetObjectUris) {
        assertValidUri(eventTargetObjectUris);
        this.eventTargetObjectUris = eventTargetObjectUris;
    }

    public String getAgentUri() {
        return agentUri;
    }

    public void setAgentUri(String agentUri) {
        assertValidUri(agentUri);
        this.agentUri = agentUri;
    }

    public String getAgentProviderUri() {
        return agentProviderUri;
    }

    public void setAgentProviderUri(String agentProviderUri) {
        assertValidUri(agentProviderUri);
        this.agentProviderUri = agentProviderUri;
    }

    public String getAgentDescription() {
        return agentDescription;
    }

    public void setAgentDescription(String agentDescription) {
        this.agentDescription = agentDescription;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DiscoSolrDocument that = (DiscoSolrDocument) o;

        if (discoId != null ? !discoId.equals(that.discoId) : that.discoId != null) return false;
        if (discoUri != null ? !discoUri.equals(that.discoUri) : that.discoUri != null) return false;
        if (discoCreatorUri != null ? !discoCreatorUri.equals(that.discoCreatorUri) : that.discoCreatorUri != null)
            return false;
        if (discoDescription != null ? !discoDescription.equals(that.discoDescription) : that.discoDescription != null)
            return false;
        if (discoProviderid != null ? !discoProviderid.equals(that.discoProviderid) : that.discoProviderid != null)
            return false;
        if (discoAggregatedResourceUris != null ? !discoAggregatedResourceUris.equals(that.discoAggregatedResourceUris) : that.discoAggregatedResourceUris != null)
            return false;
        if (discoProvenanceUri != null ? !discoProvenanceUri.equals(that.discoProvenanceUri) : that.discoProvenanceUri != null)
            return false;
        if (discoRelatedStatements != null ? !discoRelatedStatements.equals(that.discoRelatedStatements) : that.discoRelatedStatements != null)
            return false;
        if (eventUri != null ? !eventUri.equals(that.eventUri) : that.eventUri != null) return false;
        if (eventAgentUri != null ? !eventAgentUri.equals(that.eventAgentUri) : that.eventAgentUri != null)
            return false;
        if (eventStartTime != null ? !eventStartTime.equals(that.eventStartTime) : that.eventStartTime != null)
            return false;
        if (eventEndTime != null ? !eventEndTime.equals(that.eventEndTime) : that.eventEndTime != null)
            return false;
        if (eventDescription != null ? !eventDescription.equals(that.eventDescription) : that.eventDescription != null)
            return false;
        if (eventType != null ? !eventType.equals(that.eventType) : that.eventType != null) return false;
        if (eventSourceObjectUris != null ? !eventSourceObjectUris.equals(that.eventSourceObjectUris) : that.eventSourceObjectUris != null)
            return false;
        if (eventTargetObjectUris != null ? !eventTargetObjectUris.equals(that.eventTargetObjectUris) : that.eventTargetObjectUris != null)
            return false;
        if (agentUri != null ? !agentUri.equals(that.agentUri) : that.agentUri != null) return false;
        if (agentProviderUri != null ? !agentProviderUri.equals(that.agentProviderUri) : that.agentProviderUri != null)
            return false;
        return agentDescription != null ? agentDescription.equals(that.agentDescription) : that.agentDescription == null;
    }

    @Override
    public int hashCode() {
        int result = discoId != null ? discoId.hashCode() : 0;
        result = 31 * result + (discoUri != null ? discoUri.hashCode() : 0);
        result = 31 * result + (discoCreatorUri != null ? discoCreatorUri.hashCode() : 0);
        result = 31 * result + (discoDescription != null ? discoDescription.hashCode() : 0);
        result = 31 * result + (discoProviderid != null ? discoProviderid.hashCode() : 0);
        result = 31 * result + (discoAggregatedResourceUris != null ? discoAggregatedResourceUris.hashCode() : 0);
        result = 31 * result + (discoProvenanceUri != null ? discoProvenanceUri.hashCode() : 0);
        result = 31 * result + (discoRelatedStatements != null ? discoRelatedStatements.hashCode() : 0);
        result = 31 * result + (eventUri != null ? eventUri.hashCode() : 0);
        result = 31 * result + (eventAgentUri != null ? eventAgentUri.hashCode() : 0);
        result = 31 * result + (eventStartTime != null ? eventStartTime.hashCode() : 0);
        result = 31 * result + (eventEndTime != null ? eventEndTime.hashCode() : 0);
        result = 31 * result + (eventDescription != null ? eventDescription.hashCode() : 0);
        result = 31 * result + (eventType != null ? eventType.hashCode() : 0);
        result = 31 * result + (eventSourceObjectUris != null ? eventSourceObjectUris.hashCode() : 0);
        result = 31 * result + (eventTargetObjectUris != null ? eventTargetObjectUris.hashCode() : 0);
        result = 31 * result + (agentUri != null ? agentUri.hashCode() : 0);
        result = 31 * result + (agentProviderUri != null ? agentProviderUri.hashCode() : 0);
        result = 31 * result + (agentDescription != null ? agentDescription.hashCode() : 0);
        return result;
    }
}
