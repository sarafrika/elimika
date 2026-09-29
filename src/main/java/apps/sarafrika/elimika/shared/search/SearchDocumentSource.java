package apps.sarafrika.elimika.shared.search;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The single place a module says what one of its search indexes contains. Implemented as a Spring
 * bean inside the owning module - one per index - and discovered by the search module, which owns
 * the engine and the sync machinery but knows nothing about any domain.
 * <p>
 * PostgreSQL stays the source of truth: the index is a disposable projection, rebuilt at any time
 * from {@link #loadAfter}, and kept current by {@link #loadByUuids} after every relevant commit.
 *
 * @param <D> the document record; serialised to JSON with its {@code @JsonProperty} names
 */
public interface SearchDocumentSource<D extends SearchDocument> {

    /** The index this source feeds. */
    SearchIndexDefinition definition();

    /**
     * Loads the current documents for these UUIDs. Returns only documents that <em>should</em> be in
     * the index - a UUID that is missing from the result (deleted, archived, unpublished) is deleted
     * from the index. Called in a read-only transaction, in chunks of at most 500.
     */
    List<D> loadByUuids(Collection<UUID> uuids);

    /**
     * Keyset scan used by rebuilds: the indexable documents among the next {@code batchSize} rows with
     * {@code id > lastId}, ordered by id. Each call runs in its own read-only transaction.
     */
    SearchBatch<D> loadAfter(long lastId, int batchSize);

    /** The entity changes that should re-index documents of this index. */
    List<SearchIndexTrigger<?>> triggers();

    /**
     * Resolves a fan-out key produced by one of this source's fan-out triggers (such as
     * {@code "category:<uuid>"}) into the UUIDs of the documents it affects. Runs after commit, in a
     * read-only transaction, so it may query freely.
     */
    default Set<UUID> resolveFanOut(String key) {
        return Set.of();
    }

    /**
     * How many documents the index should hold, for drift reconciliation, or {@code -1} when the
     * source cannot say cheaply.
     */
    default long countIndexable() {
        return -1;
    }
}
