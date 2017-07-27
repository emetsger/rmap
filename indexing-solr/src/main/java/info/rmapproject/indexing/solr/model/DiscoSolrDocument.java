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
    private URI disco_description;

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

    public URI getDisco_description() {
        return disco_description;
    }

    public void setDisco_description(URI disco_description) {
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
}
