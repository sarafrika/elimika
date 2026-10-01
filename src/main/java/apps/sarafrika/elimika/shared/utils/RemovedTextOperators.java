package apps.sarafrika.elimika.shared.utils;

import java.util.Locale;
import java.util.Set;

/**
 * The {@code _like}, {@code _startswith} and {@code _endswith} filter operators, which used to run SQL
 * {@code LIKE} matches. Free-text search is served only by the search engine through {@code q}, so a
 * request key using one of them is rejected with a 400 and one message wherever it arrives: the
 * specification builder, the search-index translator, and every listing endpoint that does not bind
 * its query string to a filter map at all.
 */
public final class RemovedTextOperators {

    public static final Set<String> OPERATIONS = Set.of("like", "startswith", "endswith");
    public static final String MESSAGE = "Text operators were removed; use the q parameter for text search";

    private static final int MAX_ECHOED_KEY_LENGTH = 64;

    private RemovedTextOperators() {
    }

    /** Whether {@code key} ends in one of the removed operators, compared case-insensitively. */
    public static boolean isRemoved(String key) {
        if (key == null) {
            return false;
        }
        int lastUnderscore = key.lastIndexOf('_');
        if (lastUnderscore < 0 || lastUnderscore == key.length() - 1) {
            return false;
        }
        return OPERATIONS.contains(key.substring(lastUnderscore + 1).toLowerCase(Locale.ROOT));
    }

    /**
     * @throws IllegalArgumentException (a 400) naming {@code key} when it uses a removed operator
     */
    public static void reject(String key) {
        if (isRemoved(key)) {
            throw new IllegalArgumentException(MESSAGE + " (rejected: " + sanitise(key) + ")");
        }
    }

    /**
     * @throws IllegalArgumentException (a 400) for the first key in {@code keys} using a removed operator
     */
    public static void rejectAny(Iterable<String> keys) {
        if (keys == null) {
            return;
        }
        for (String key : keys) {
            reject(key);
        }
    }

    private static String sanitise(String key) {
        String cleaned = key.replaceAll("[^A-Za-z0-9_.]", "");
        return cleaned.length() > MAX_ECHOED_KEY_LENGTH ? cleaned.substring(0, MAX_ECHOED_KEY_LENGTH) : cleaned;
    }
}
