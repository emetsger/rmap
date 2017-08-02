package info.rmapproject.indexing.solr.model;

import java.net.URI;
import java.util.Collection;

/**
 * Provides common utility methods used by model classes.
 *
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
class ModelUtils {

    /**
     * Asserts the supplied string is a valid URI according to {@link URI#create(String)}.
     *
     * @param uri a string that claims to be a URI
     * @throws IllegalArgumentException if {@code uri} is {@code null} or is not a valid {@code URI}
     */
    static void assertValidUri(String uri) {
        if (uri == null) {
            throw new IllegalArgumentException("Supplied URI must not be null.");
        }
        //noinspection ResultOfMethodCallIgnored
        URI.create(uri);
    }

    /**
     * Asserts that each string in the supplied collection is a valid URI according to {@link URI#create(String)}.
     *
     * @param uris a collection of strings, each of which claims to be a URI
     * @throws IllegalArgumentException if {@code uri} is {@code null} or empty, or is not a valid {@code URI}
     */
    static void assertValidUri(Collection<String> uris) {
        if (uris == null) {
            throw new IllegalArgumentException("Supplied collection of URIs must not be null.");
        }
        if (uris.isEmpty()) {
            throw new IllegalArgumentException("Supplied collection of URIs must not be empty.");
        }
        uris.forEach(ModelUtils::assertValidUri);
    }
}
