package apps.sarafrika.elimika.shared.search;

import java.util.UUID;

/**
 * A projection written to a search index. Every document is keyed by the UUID of the row it
 * projects, which is the index's primary key. A record with a {@code uuid} component satisfies this
 * interface without any extra code.
 */
public interface SearchDocument {

    UUID uuid();
}
