package apps.sarafrika.elimika.shared.search;

import java.util.List;
import java.util.Objects;

/**
 * One search against one index.
 *
 * @param index    the index to search
 * @param text     the user's query text, or {@code null}/blank to list by filter and sort only
 * @param filter   the caller's own filter (e.g. from {@link SearchParamsTranslator}), or {@code null}
 * @param scope    the visibility boundary - required, and always ANDed with {@code filter}
 * @param sort     orderings over sortable attributes; empty means relevance
 * @param page     0-based page number
 * @param size     page size, 1..{@value #MAX_SIZE}
 * @param facets   attributes to return a facet distribution for
 * @param searchOn restricts {@code text} to these searchable attributes, or {@code null} for all
 */
public record SearchRequest(
        String index,
        String text,
        SearchFilter filter,
        SearchScope scope,
        List<SearchSort> sort,
        int page,
        int size,
        List<String> facets,
        List<String> searchOn
) {

    public static final int MAX_SIZE = 100;

    public SearchRequest {
        Objects.requireNonNull(index, "index");
        Objects.requireNonNull(scope, "scope - every search must say what the caller may see");
        if (page < 0) {
            throw new IllegalArgumentException("page must be 0 or greater");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + MAX_SIZE);
        }
        sort = sort == null ? List.of() : List.copyOf(sort);
        facets = facets == null ? List.of() : List.copyOf(facets);
        searchOn = searchOn == null ? null : List.copyOf(searchOn);
    }

    /** A text search with a filter, relevance-ordered, no facets. */
    public static SearchRequest of(String index, String text, SearchFilter filter, SearchScope scope, int page, int size) {
        return new SearchRequest(index, text, filter, scope, List.of(), page, size, List.of(), null);
    }

    public boolean hasText() {
        return text != null && !text.isBlank();
    }
}
