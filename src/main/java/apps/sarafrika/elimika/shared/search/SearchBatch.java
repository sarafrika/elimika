package apps.sarafrika.elimika.shared.search;

import java.util.List;

/**
 * One keyset page of a rebuild scan.
 *
 * @param documents the indexable documents found in this page (rows that should not be indexed are
 *                  simply left out)
 * @param lastId    the highest row id examined in this page, indexable or not. The next page starts
 *                  after it. When no rows are left, return the {@code lastId} you were given, which
 *                  is how the rebuilder knows the scan is finished.
 */
public record SearchBatch<D>(List<D> documents, long lastId) {

    public SearchBatch {
        documents = documents == null ? List.of() : List.copyOf(documents);
    }

    /** The scan is finished. */
    public static <D> SearchBatch<D> end(long lastId) {
        return new SearchBatch<>(List.of(), lastId);
    }
}
