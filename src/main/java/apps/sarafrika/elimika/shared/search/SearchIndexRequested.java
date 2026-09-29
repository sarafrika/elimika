package apps.sarafrika.elimika.shared.search;

import java.util.Set;
import java.util.UUID;

/**
 * Asks the search module to bring these documents of one index back in line with the database.
 * <p>
 * Published by {@link SearchIndexRequests} just before a transaction commits, so Spring Modulith
 * records the publication in the same transaction as the business change: either both happen or
 * neither does, and a failed sync stays as an incomplete publication until it succeeds.
 *
 * @param index      the index to sync
 * @param uuids      documents to reload (or delete, when their source no longer returns them)
 * @param fanOutKeys keys the index's source resolves into further document UUIDs
 */
public record SearchIndexRequested(String index, Set<UUID> uuids, Set<String> fanOutKeys) {

    public SearchIndexRequested {
        uuids = uuids == null ? Set.of() : Set.copyOf(uuids);
        fanOutKeys = fanOutKeys == null ? Set.of() : Set.copyOf(fanOutKeys);
    }
}
