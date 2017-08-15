package info.rmapproject.indexing.solr;

import info.rmapproject.core.model.RMapIri;
import info.rmapproject.core.model.event.RMapEvent;
import info.rmapproject.core.model.event.RMapEventCreation;
import info.rmapproject.core.model.event.RMapEventDeletion;
import info.rmapproject.core.model.event.RMapEventDerivation;
import info.rmapproject.core.model.event.RMapEventInactivation;
import info.rmapproject.core.model.event.RMapEventTombstone;
import info.rmapproject.core.model.event.RMapEventUpdate;
import info.rmapproject.core.model.event.RMapEventUpdateWithReplace;
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

import static info.rmapproject.indexing.solr.IndexUtils.assertNotNullOrEmpty;

/**
 * Provides common utility methods used by model classes.
 *
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class IndexUtils {

    public enum EventDirection { SOURCE, TARGET, EITHER }

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

    /**
     * Retrieves the source or target of the supplied event.
     * <p>
     * Most RMap events have a source and a target.  For example, an update event will have the disco <em>being
     * updated</em> as the source and the <em>updated disco</em> as the target.  The Java method used to retrieve the
     * source or target of the event depends on the sub-type of {@code RMapEvent}.
     * </p>
     *
     * @param event the event to examine
     * @param direction the direction of
     * @return an {@code Optional} with the IRI of the referenced disco
     * @throws IllegalArgumentException if an unknown {@code RMapEvent} is encountered
     * @throws NullPointerException if the source or target IRI is {@code null}
     */
    public static Optional<RMapIri> findEventIri(RMapEvent event, EventDirection direction) {
        Optional<RMapIri> iri = Optional.empty();

        if (direction == EventDirection.TARGET) {
            switch (event.getEventType()) {
                case CREATION:
                    // TODO: handle multiple creation ids
                    iri = Optional.of(assertNotNullOrEmpty(((RMapEventCreation) event).getCreatedObjectIds()).get(0));
                    break;

                case DERIVATION:
                    iri = Optional.of(((RMapEventDerivation) event).getDerivedObjectId());
                    break;

                case UPDATE:
                    iri = Optional.of(((RMapEventUpdate) event).getDerivedObjectId());
                    break;

                case DELETION:
                    // TODO: handle multiple deletion ids
                    // TODO: decide what the source IRI and target IRI is for a deletion event
                    // Right now, the IRI of the deleted DiSCO is used for both
                    iri = Optional.of(assertNotNullOrEmpty(((RMapEventDeletion)event).getDeletedObjectIds()).get(0));
                    break;

                case TOMBSTONE:
                    // TODO: decide what the source IRI and target IRI is for a tombstone event
                    // Right now, the IRI of the tombstoned resource is used for both
                    iri = Optional.of(((RMapEventTombstone) event).getTombstonedResourceId());
                    break;

                case INACTIVATION:
                    // TODO: decide what the source IRI and target IRI is for an inactivation event
                    // Right now, the IRI of the inactivated disco is used for both
                    iri = Optional.of(((RMapEventInactivation) event).getInactivatedObjectId());
                    break;

                case REPLACE:
                    // TODO: missing the source object of a replacement?
                    iri = Optional.empty();

                default:
                    throw new IllegalArgumentException("Unknown RMap event type: " + event);

            }
        }

        if (direction == EventDirection.SOURCE) {
            switch (event.getEventType()) {
                case CREATION:
                    // TODO: handle multiple creation ids
                    iri = Optional.empty();
                    break;

                case DERIVATION:
                    iri = Optional.of(((RMapEventDerivation) event).getSourceObjectId());
                    break;

                case UPDATE:
                    iri = Optional.of(((RMapEventUpdate)event).getInactivatedObjectId());
                    break;

                case DELETION:
                    // TODO: handle multiple deletion ids
                    // TODO: decide what the source IRI and target IRI is for a deletion event
                    // Right now, the IRI of the deleted DiSCO is used for both
                    iri = Optional.of(assertNotNullOrEmpty(((RMapEventDeletion)event).getDeletedObjectIds()).get(0));
                    break;

                case TOMBSTONE:
                    // TODO: decide what the source IRI and target IRI is for a tombstone event
                    // Right now, the IRI of the tombstoned resource is used for both
                    iri = Optional.of(((RMapEventTombstone) event).getTombstonedResourceId());

                case INACTIVATION:
                    // TODO: decide what the source IRI and target IRI is for an inactivation event
                    // Right now, the IRI of the inactivated disco is used for both
                    iri = Optional.of(((RMapEventInactivation) event).getInactivatedObjectId());
                    break;

                case REPLACE:
                    iri = Optional.of(((RMapEventUpdateWithReplace) event).getUpdatedObjectId());

                default:
                    throw new IllegalArgumentException("Unknown RMap event type: " + event.getEventType());

            }
        }

        return iri;
    }
}