package info.rmapproject.indexing.solr.model;

import org.apache.solr.client.solrj.beans.Field;
import org.springframework.data.annotation.Id;
import org.springframework.data.solr.core.mapping.SolrDocument;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static info.rmapproject.indexing.solr.model.ModelUtils.assertValidUri;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
@SolrDocument(solrCoreName = "versions")
public class DiscoVersionDocument {

    @Id
    @Field
    private Long version_id;

    @Field("disco_uri")
    private String discoUri;

    @Field("disco_status")
    private String discoStatus;

    @Field("past_uris")
    private List<String> pastUris;

    @Field("last_updated")
    private Long lastUpdated;

    public DiscoVersionDocument() {

    }

    public Long getVersion_id() {
        return version_id;
    }

    public void setVersion_id(Long version_id) {
        this.version_id = version_id;
    }

    public String getDiscoUri() {
        return discoUri;
    }

    public void setDiscoUri(String discoUri) {
        assertValidUri(discoUri);
        this.discoUri = discoUri;
    }

    public String getDiscoStatus() {
        return discoStatus;
    }

    public void setDiscoStatus(String discoStatus) {
        this.discoStatus = discoStatus;
    }

    public List<String> getPastUris() {
        return pastUris;
    }

    public void setPastUris(List<String> pastUris) {
        assertValidUri(pastUris);
        this.pastUris = pastUris;
    }

    public Long getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DiscoVersionDocument that = (DiscoVersionDocument) o;

        if (version_id != null ? !version_id.equals(that.version_id) : that.version_id != null) return false;
        if (discoUri != null ? !discoUri.equals(that.discoUri) : that.discoUri != null) return false;
        if (discoStatus != null ? !discoStatus.equals(that.discoStatus) : that.discoStatus != null) return false;
        if (pastUris != null ? !pastUris.equals(that.pastUris) : that.pastUris != null) return false;
        return lastUpdated != null ? lastUpdated.equals(that.lastUpdated) : that.lastUpdated == null;
    }

    @Override
    public int hashCode() {
        int result = version_id != null ? version_id.hashCode() : 0;
        result = 31 * result + (discoUri != null ? discoUri.hashCode() : 0);
        result = 31 * result + (discoStatus != null ? discoStatus.hashCode() : 0);
        result = 31 * result + (pastUris != null ? pastUris.hashCode() : 0);
        result = 31 * result + (lastUpdated != null ? lastUpdated.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "DiscoVersionDocument{" +
                "version_id=" + version_id +
                ", discoUri=" + discoUri +
                ", discoStatus='" + discoStatus + '\'' +
                ", pastUris=" + pastUris +
                ", lastUpdated=" + lastUpdated +
                '}';
    }

    public static class Builder {

        private DiscoVersionDocument instance;
        private boolean lastUpdatedInvoked;

        public Builder() {

        }

        public Builder(DiscoVersionDocument instance) {
            this.instance = instance;
        }

        public Builder id(Long id) {
            instantiateIfNull();
            instance.setVersion_id(id);
            return this;
        }

        public Builder activeUri(String uri) {
            instantiateIfNull();
            assertValidUri(uri);
            if (instance.getDiscoUri() != null) {
                this.addPastUri(instance.getDiscoUri());
            }
            instance.setDiscoUri(uri);
            instance.setLastUpdated(Calendar.getInstance().getTimeInMillis());
            return this;
        }

        public Builder discoUri(String uri) {
            instantiateIfNull();
            assertValidUri(uri);
            instance.setDiscoUri(uri);
            return this;
        }

        public Builder status(String status) {
            instantiateIfNull();
            instance.setDiscoStatus(status);
            return this;
        }

        public Builder lastUpdated(Long lastUpdated) {
            instantiateIfNull();
            instance.setLastUpdated(lastUpdated);
            lastUpdatedInvoked = true;
            return this;
        }

        public Builder addPastUri(String uri) {
            instantiateIfNull();
            assertValidUri(uri);
            if (instance.getPastUris() == null) {
                instance.pastUris = new ArrayList<>();
            }
            instance.getPastUris().add(uri);
            return this;
        }

        public DiscoVersionDocument build() {
            instantiateIfNull();
            if (!lastUpdatedInvoked) {
                lastUpdated(Calendar.getInstance().getTimeInMillis());
            }
            return instance;
        }

        private void instantiateIfNull() {
            if (instance == null) {
                instance = new DiscoVersionDocument();
            }
        }

        private void reset() {
            instance = null;
            lastUpdatedInvoked = false;
        }

    }

}
