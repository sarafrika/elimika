package apps.sarafrika.elimika.shared.search;

import java.util.List;
import java.util.Map;

/**
 * One page of search results.
 *
 * @param hits              the documents on this page, in rank order
 * @param totalHits         the number of documents matching the query and scope (capped by the
 *                          index's {@code maxTotalHits})
 * @param page              the 0-based page number
 * @param size              the requested page size
 * @param facetDistribution per requested facet, the count of matching documents for each value
 */
public record SearchPage(
        List<SearchHit> hits,
        long totalHits,
        int page,
        int size,
        Map<String, Map<String, Long>> facetDistribution
) {

    public SearchPage {
        hits = hits == null ? List.of() : List.copyOf(hits);
        facetDistribution = facetDistribution == null ? Map.of() : Map.copyOf(facetDistribution);
    }

    public int totalPages() {
        return size == 0 ? 0 : (int) Math.ceil((double) totalHits / size);
    }
}
