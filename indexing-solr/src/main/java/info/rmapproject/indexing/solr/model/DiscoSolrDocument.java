package info.rmapproject.indexing.solr.model;

import org.apache.solr.client.solrj.beans.Field;
import org.springframework.data.annotation.Id;
import org.springframework.data.solr.core.mapping.SolrDocument;

import java.net.URI;
import java.util.Calendar;
import java.util.List;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
@SolrDocument(solrCoreName = "discos")
public class DiscoSolrDocument {

    @Id
    @Field
    private Long disco_id;

    @Field
    private URI disco_uri;

    @Field
    private URI disco_creator_uri;

    @Field
    private String disco_description;

    @Field
    private URI disco_providerid;

    @Field
    private List<URI> disco_aggregated_resource_uris;

    @Field
    private URI disco_provenance_uri;

    @Field
    private List<String> disco_related_statements;


    @Field
    private URI event_uri;

    @Field
    private URI event_agent_uri;

    @Field
    private Calendar event_start_time;

    @Field
    private Calendar event_end_time;

    @Field
    private String event_description;

    @Field
    private String event_type;

    @Field
    private List<URI> event_source_object_uris;

    @Field
    private List<URI> event_target_object_uris;


    @Field
    private URI agent_uri;

    @Field
    private URI agent_provider_uri;

    @Field
    private String agent_description;

    public DiscoSolrDocument() {

    }

    public Long getDisco_id() {
        return disco_id;
    }

    public void setDisco_id(Long disco_id) {
        this.disco_id = disco_id;
    }

    public URI getDisco_uri() {
        return disco_uri;
    }

    public void setDisco_uri(URI disco_uri) {
        this.disco_uri = disco_uri;
    }

    public URI getDisco_creator_uri() {
        return disco_creator_uri;
    }

    public void setDisco_creator_uri(URI disco_creator_uri) {
        this.disco_creator_uri = disco_creator_uri;
    }

    public String getDisco_description() {
        return disco_description;
    }

    public void setDisco_description(String disco_description) {
        this.disco_description = disco_description;
    }

    public URI getDisco_providerid() {
        return disco_providerid;
    }

    public void setDisco_providerid(URI disco_providerid) {
        this.disco_providerid = disco_providerid;
    }

    public List<URI> getDisco_aggregated_resource_uris() {
        return disco_aggregated_resource_uris;
    }

    public void setDisco_aggregated_resource_uris(List<URI> disco_aggregated_resource_uris) {
        this.disco_aggregated_resource_uris = disco_aggregated_resource_uris;
    }

    public URI getDisco_provenance_uri() {
        return disco_provenance_uri;
    }

    public void setDisco_provenance_uri(URI disco_provenance_uri) {
        this.disco_provenance_uri = disco_provenance_uri;
    }

    public List<String> getDisco_related_statements() {
        return disco_related_statements;
    }

    public void setDisco_related_statements(List<String> disco_related_statements) {
        this.disco_related_statements = disco_related_statements;
    }

    public URI getEvent_uri() {
        return event_uri;
    }

    public void setEvent_uri(URI event_uri) {
        this.event_uri = event_uri;
    }

    public URI getEvent_agent_uri() {
        return event_agent_uri;
    }

    public void setEvent_agent_uri(URI event_agent_uri) {
        this.event_agent_uri = event_agent_uri;
    }

    public Calendar getEvent_start_time() {
        return event_start_time;
    }

    public void setEvent_start_time(Calendar event_start_time) {
        this.event_start_time = event_start_time;
    }

    public Calendar getEvent_end_time() {
        return event_end_time;
    }

    public void setEvent_end_time(Calendar event_end_time) {
        this.event_end_time = event_end_time;
    }

    public String getEvent_description() {
        return event_description;
    }

    public void setEvent_description(String event_description) {
        this.event_description = event_description;
    }

    public String getEvent_type() {
        return event_type;
    }

    public void setEvent_type(String event_type) {
        this.event_type = event_type;
    }

    public List<URI> getEvent_source_object_uris() {
        return event_source_object_uris;
    }

    public void setEvent_source_object_uris(List<URI> event_source_object_uris) {
        this.event_source_object_uris = event_source_object_uris;
    }

    public List<URI> getEvent_target_object_uris() {
        return event_target_object_uris;
    }

    public void setEvent_target_object_uris(List<URI> event_target_object_uris) {
        this.event_target_object_uris = event_target_object_uris;
    }

    public URI getAgent_uri() {
        return agent_uri;
    }

    public void setAgent_uri(URI agent_uri) {
        this.agent_uri = agent_uri;
    }

    public URI getAgent_provider_uri() {
        return agent_provider_uri;
    }

    public void setAgent_provider_uri(URI agent_provider_uri) {
        this.agent_provider_uri = agent_provider_uri;
    }

    public String getAgent_description() {
        return agent_description;
    }

    public void setAgent_description(String agent_description) {
        this.agent_description = agent_description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DiscoSolrDocument that = (DiscoSolrDocument) o;

        if (disco_id != null ? !disco_id.equals(that.disco_id) : that.disco_id != null) return false;
        if (disco_uri != null ? !disco_uri.equals(that.disco_uri) : that.disco_uri != null) return false;
        if (disco_creator_uri != null ? !disco_creator_uri.equals(that.disco_creator_uri) : that.disco_creator_uri != null)
            return false;
        if (disco_description != null ? !disco_description.equals(that.disco_description) : that.disco_description != null)
            return false;
        if (disco_providerid != null ? !disco_providerid.equals(that.disco_providerid) : that.disco_providerid != null)
            return false;
        if (discoAggregatedResourceUris != null ? !discoAggregatedResourceUris.equals(that.discoAggregatedResourceUris) : that.discoAggregatedResourceUris != null)
            return false;
        if (disco_provenance_uri != null ? !disco_provenance_uri.equals(that.disco_provenance_uri) : that.disco_provenance_uri != null)
            return false;
        if (disco_related_statements != null ? !disco_related_statements.equals(that.disco_related_statements) : that.disco_related_statements != null)
            return false;
        if (event_uri != null ? !event_uri.equals(that.event_uri) : that.event_uri != null) return false;
        if (event_agent_uri != null ? !event_agent_uri.equals(that.event_agent_uri) : that.event_agent_uri != null)
            return false;
        if (event_start_time != null ? !event_start_time.equals(that.event_start_time) : that.event_start_time != null)
            return false;
        if (event_end_time != null ? !event_end_time.equals(that.event_end_time) : that.event_end_time != null)
            return false;
        if (event_description != null ? !event_description.equals(that.event_description) : that.event_description != null)
            return false;
        if (event_type != null ? !event_type.equals(that.event_type) : that.event_type != null) return false;
        if (event_source_object_uris != null ? !event_source_object_uris.equals(that.event_source_object_uris) : that.event_source_object_uris != null)
            return false;
        if (event_target_object_uris != null ? !event_target_object_uris.equals(that.event_target_object_uris) : that.event_target_object_uris != null)
            return false;
        if (agent_uri != null ? !agent_uri.equals(that.agent_uri) : that.agent_uri != null) return false;
        if (agent_provider_uri != null ? !agent_provider_uri.equals(that.agent_provider_uri) : that.agent_provider_uri != null)
            return false;
        return agent_description != null ? agent_description.equals(that.agent_description) : that.agent_description == null;
    }

    @Override
    public int hashCode() {
        int result = disco_id != null ? disco_id.hashCode() : 0;
        result = 31 * result + (disco_uri != null ? disco_uri.hashCode() : 0);
        result = 31 * result + (disco_creator_uri != null ? disco_creator_uri.hashCode() : 0);
        result = 31 * result + (disco_description != null ? disco_description.hashCode() : 0);
        result = 31 * result + (disco_providerid != null ? disco_providerid.hashCode() : 0);
        result = 31 * result + (discoAggregatedResourceUris != null ? discoAggregatedResourceUris.hashCode() : 0);
        result = 31 * result + (disco_provenance_uri != null ? disco_provenance_uri.hashCode() : 0);
        result = 31 * result + (disco_related_statements != null ? disco_related_statements.hashCode() : 0);
        result = 31 * result + (event_uri != null ? event_uri.hashCode() : 0);
        result = 31 * result + (event_agent_uri != null ? event_agent_uri.hashCode() : 0);
        result = 31 * result + (event_start_time != null ? event_start_time.hashCode() : 0);
        result = 31 * result + (event_end_time != null ? event_end_time.hashCode() : 0);
        result = 31 * result + (event_description != null ? event_description.hashCode() : 0);
        result = 31 * result + (event_type != null ? event_type.hashCode() : 0);
        result = 31 * result + (event_source_object_uris != null ? event_source_object_uris.hashCode() : 0);
        result = 31 * result + (event_target_object_uris != null ? event_target_object_uris.hashCode() : 0);
        result = 31 * result + (agent_uri != null ? agent_uri.hashCode() : 0);
        result = 31 * result + (agent_provider_uri != null ? agent_provider_uri.hashCode() : 0);
        result = 31 * result + (agent_description != null ? agent_description.hashCode() : 0);
        return result;
    }
}
