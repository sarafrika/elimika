package apps.sarafrika.elimika.tenancy.search;

import apps.sarafrika.elimika.shared.search.SearchResults;
import apps.sarafrika.elimika.shared.search.SearchIndexDefinition;
import apps.sarafrika.elimika.shared.search.SearchPage;
import apps.sarafrika.elimika.shared.search.SearchParamsTranslator;
import apps.sarafrika.elimika.shared.search.SearchRequest;
import apps.sarafrika.elimika.shared.search.SearchSort;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Turns a bound {@link Pageable} into search paging and sorting, and a page of hits back into a
 * Spring {@link Page} of hydrated rows in hit order.
 */
final class SearchPaging {

    static final int DEFAULT_SIZE = 20;

    /** Entity property names the list endpoints already accept, mapped to document attributes. */
    private static final Map<String, String> SORT_ALIASES = Map.of(
            "createdDate", "created_at",
            "created_date", "created_at");

    private SearchPaging() {
    }

    static int page(Pageable pageable) {
        return pageable == null || pageable.isUnpaged() ? 0 : pageable.getPageNumber();
    }

    static int size(Pageable pageable) {
        if (pageable == null || pageable.isUnpaged()) {
            return DEFAULT_SIZE;
        }
        return Math.max(1, Math.min(pageable.getPageSize(), SearchRequest.MAX_SIZE));
    }

    /**
     * The request's sort as search sorts. Unsortable properties are rejected with
     * {@link IllegalArgumentException} (a 400), exactly like the database path's allow-list.
     */
    static List<SearchSort> sorts(Pageable pageable, SearchIndexDefinition definition) {
        if (pageable == null || pageable.isUnpaged() || pageable.getSort().isUnsorted()) {
            return List.of();
        }
        List<SearchSort> sorts = new ArrayList<>();
        for (Sort.Order order : pageable.getSort()) {
            String property = SORT_ALIASES.getOrDefault(order.getProperty(), order.getProperty());
            sorts.addAll(SearchParamsTranslator.toSort(
                    property + "," + (order.isAscending() ? "asc" : "desc"), definition));
        }
        return sorts;
    }

    /**
     * Rebuilds a page from the hits and the rows that survived hydration, in hit order. When the
     * hydration query withheld rows the engine returned, the total is restated from what is shown,
     * as {@code EnrollmentVisibilityService#visibleToCaller} does: a total counted over rows the
     * caller may not see would disclose them just as plainly as returning them.
     */
    static <T> Page<T> toPage(SearchPage result, Collection<T> rows, Function<T, UUID> uuidOf, Pageable pageable) {
        List<T> ordered = SearchResults.inHitOrder(SearchResults.hitUuids(result), rows, uuidOf);
        long total = SearchResults.total(result.totalHits(), result.hits().size(), ordered.size());
        Sort sort = pageable == null || pageable.isUnpaged() ? Sort.unsorted() : pageable.getSort();
        return new PageImpl<>(ordered, PageRequest.of(result.page(), result.size(), sort), total);
    }

    static List<UUID> hitUuids(SearchPage result) {
        return SearchResults.hitUuids(result);
    }
}
