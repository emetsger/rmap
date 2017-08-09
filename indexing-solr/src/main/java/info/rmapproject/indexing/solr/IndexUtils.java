package info.rmapproject.indexing.solr;

import info.rmapproject.core.model.RMapIri;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

import java.net.URI;
import java.rmi.server.RMIClassLoader;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Provides common utility methods used by model classes.
 *
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class IndexUtils {

    /**
     * Asserts the supplied string is a valid URI according to {@link URI#create(String)}.  {@code null} URIs are OK,
     * which make the calling code a little easier to write, and cleaner looking.
     *
     * @param uri a string that claims to be a URI
     * @throws IllegalArgumentException if {@code uri} is not a valid {@code URI}
     */
    public static void assertValidUri(String uri) {
        if (uri != null) {
            //noinspection ResultOfMethodCallIgnored
            URI.create(uri);
        }
    }

    /**
     * Asserts that each string in the supplied collection is a valid URI according to {@link URI#create(String)}.
     *
     * @param uris a collection of strings, each of which claims to be a URI
     * @throws IllegalArgumentException if {@code uri} is {@code null} or empty, or is not a valid {@code URI}
     */
    public static void assertValidUri(Collection<String> uris) {
        if (uris == null) {
            throw new IllegalArgumentException("Supplied collection of URIs must not be null.");
        }
        if (uris.isEmpty()) {
            throw new IllegalArgumentException("Supplied collection of URIs must not be empty.");
        }
        uris.forEach(IndexUtils::assertValidUri);
    }

    public static String assertNotNullOrEmpty(String s) {
        if (s == null) {
            throw new IllegalArgumentException("Supplied string must not be null.");
        }

        if (s.trim().length() == 0) {
            throw new IllegalArgumentException("Supplied string must not be empty.");
        }

        return s;
    }

    public static <T> List<T> assertNotNullOrEmpty(List<T> list) {
        if (list == null) {
            throw new IllegalArgumentException("Supplied List must not be null.");
        }

        if (list.isEmpty()) {
            throw new IllegalArgumentException("Supplied List must not be empty.");
        }

        return list;
    }

    public static <K, V> Map<K, V> assertNotNullOrEmpty(Map<K, V> map) {
        if (map == null) {
            throw new IllegalArgumentException("Supplied Map must not be null.");
        }

        if (map.isEmpty()) {
            throw new IllegalArgumentException("Supplied Map must not be empty.");
        }

        return map;
    }

    public static <T> Set<T> assertNotNullOrEmpty(Set<T> set) {
        if (set == null) {
            throw new IllegalArgumentException("Supplied Set must not be null.");
        }

        if (set.isEmpty()) {
            throw new IllegalArgumentException("Supplied Set must not be empty.");
        }

        return set;
    }

    public static <T> T assertNotNull(T o) {
        if (o == null) {
            throw new IllegalArgumentException("Supplied object must not be null.");
        }

        return o;
    }

    public static String dateToString(Date d) {
        return ISODateTimeFormat.dateTime().withZoneUTC().print(new DateTime(d));
    }

    public static boolean irisEqual(RMapIri one, RMapIri two) {
        if (one != null && two != null) {
            return one.getStringValue().equals(two.getStringValue());
        } else if (one == null && two == null) {
            return true;
        }

        return false;
    }

    public static boolean irisEqual(Optional<RMapIri> optional, RMapIri two) {
        return irisEqual(optional.get(), two);
    }

    public static boolean irisEqual(Optional<RMapIri> optionalOne, Optional<RMapIri> optionalTwo) {
        return irisEqual(optionalOne.get(), optionalTwo.get());
    }
}
