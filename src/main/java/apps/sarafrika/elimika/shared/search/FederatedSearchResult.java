package apps.sarafrika.elimika.shared.search;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Results of one query run across several indexes and merged into a single ranking, e.g. a global
 * search box.
 *
 * @param hits               the merged hits, best first
 * @param estimatedTotalHits the engine's estimate of all matches across the queried indexes
 */
public record FederatedSearchResult(List<Hit> hits, long estimatedTotalHits) {

    public FederatedSearchResult {
        hits = hits == null ? List.of() : List.copyOf(hits);
    }

    /**
     * One merged hit.
     *
     * @param index        the index the document came from
     * @param uuid         the document's primary key
     * @param document     the stored document
     * @param formatted    the highlighted document, or {@code null}
     * @param rankingScore the weighted ranking score used to merge, when the engine reports one
     */
    public record Hit(
            String index,
            UUID uuid,
            Map<String, Object> document,
            Map<String, Object> formatted,
            Double rankingScore
    ) {
    }
}
