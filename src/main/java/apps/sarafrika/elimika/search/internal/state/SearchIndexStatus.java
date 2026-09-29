package apps.sarafrika.elimika.search.internal.state;

import java.util.Locale;

/** Where an index stands. */
public enum SearchIndexStatus {
    /** Built with the current schema version and kept in sync. */
    READY,
    /** A blue/green rebuild is filling a build index; writes go to both. */
    REBUILDING,
    /** The last rebuild failed; the live index is whatever it was before. */
    FAILED,
    /** Never built, or built with an older schema version; needs a rebuild. */
    STALE;

    public static SearchIndexStatus fromValue(String value) {
        return valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
