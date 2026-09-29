package apps.sarafrika.elimika.shared.search;

import java.util.Optional;

/**
 * Index lifecycle operations, used by the search module's rebuild and startup machinery. Kept apart
 * from {@link SearchGateway} so a module that only searches never sees them.
 */
public interface SearchIndexAdmin {

    /**
     * Creates {@code uid} when it does not exist and applies the definition's settings to it. The
     * {@code uid} may differ from the definition's name, which is how a build index for a blue/green
     * rebuild gets the live index's settings.
     */
    void ensureIndex(String uid, SearchIndexDefinition definition);

    /** Atomically exchanges the contents of two existing indexes. */
    void swapIndexes(String first, String second);

    /** Deletes an index; a missing index is not an error. */
    void deleteIndex(String uid);

    /** Document count and indexing flag, or empty when the index does not exist. */
    Optional<SearchIndexStats> stats(String uid);

    /**
     * @param numberOfDocuments documents currently in the index
     * @param indexing          whether the engine is still applying writes to it
     */
    record SearchIndexStats(long numberOfDocuments, boolean indexing) {
    }
}
