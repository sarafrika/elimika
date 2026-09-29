package apps.sarafrika.elimika.course.internal.search;

import apps.sarafrika.elimika.shared.search.SearchAvailability;
import apps.sarafrika.elimika.shared.search.SearchFilter;
import apps.sarafrika.elimika.shared.search.SearchGateway;
import apps.sarafrika.elimika.shared.search.SearchHit;
import apps.sarafrika.elimika.shared.search.SearchIndexDefinition;
import apps.sarafrika.elimika.shared.search.SearchPage;
import apps.sarafrika.elimika.shared.search.SearchParamsTranslator;
import apps.sarafrika.elimika.shared.search.SearchRequest;
import apps.sarafrika.elimika.shared.search.SearchScope;
import apps.sarafrika.elimika.shared.search.SearchSort;
import apps.sarafrika.elimika.shared.search.SearchUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Routes a catalogue read that carries free text ({@code q}) to the search engine, and tells the
 * caller when it must serve the read from the database instead.
 * <p>
 * A read goes to search only when {@code q} is present and the index is read-enabled. The remaining
 * {@code searchParams} are translated against the index's filterable allow-list, the caller's scope is
 * ANDed on, and the hits are hydrated back into the endpoint's existing DTOs in hit order, so the
 * response shape does not change. Whenever search cannot answer - it is off, the engine is down, a
 * parameter or sort has no search equivalent, or the page is larger than search allows - the result is
 * empty and the caller runs its existing SQL path, with {@code q} mapped to a {@code *_like} filter.
 */
@Slf4j
@Component
public class CatalogueSearchRouter {

    public static final String QUERY_PARAM = "q";

    private final SearchAvailability availability;
    private final SearchGateway gateway;

    public CatalogueSearchRouter(SearchAvailability availability, SearchGateway gateway) {
        this.availability = availability;
        this.gateway = gateway;
    }

    /** The trimmed free-text query in {@code searchParams}, or {@code null} when there is none. */
    public static String queryText(Map<String, String> searchParams) {
        if (searchParams == null) {
            return null;
        }
        String q = searchParams.get(QUERY_PARAM);
        return StringUtils.hasText(q) ? q.trim() : null;
    }

    /**
     * The params the SQL path should see: {@code q} removed (the specification builders reject unknown
     * keys) and, when there was a query, mapped to {@code likeKey} unless the caller already sent one.
     */
    public static Map<String, String> forDatabase(Map<String, String> searchParams, String likeKey) {
        if (searchParams == null || !searchParams.containsKey(QUERY_PARAM)) {
            return searchParams;
        }
        Map<String, String> params = new HashMap<>(searchParams);
        String q = queryText(params);
        params.remove(QUERY_PARAM);
        if (q != null && likeKey != null) {
            params.putIfAbsent(likeKey, q);
        }
        return params;
    }

    /**
     * Runs the read through search when it can, or returns empty to send the caller to the database.
     *
     * @param route      the index and the endpoint-specific translation rules
     * @param q          the free-text query; blank means "use the database"
     * @param params     the endpoint's other search params (reserved keys are ignored)
     * @param pageable   the bound page request; its sort is honoured only from the sortable allow-list
     * @param scope      the caller's visibility boundary, built only when search is actually used
     * @param hydrate    loads the DTOs for the hit UUIDs in one batch query; order does not matter
     * @param uuidOf     the UUID of a hydrated DTO
     */
    public <T> Optional<Page<T>> search(
            Route route,
            String q,
            Map<String, String> params,
            Pageable pageable,
            Supplier<SearchScope> scope,
            Function<List<UUID>, List<T>> hydrate,
            Function<T, UUID> uuidOf
    ) {
        String index = route.definition().name();
        if (!StringUtils.hasText(q) || !availability.isReadEnabled(index)) {
            return Optional.empty();
        }
        if (pageable == null || pageable.isUnpaged() || pageable.getPageSize() > SearchRequest.MAX_SIZE) {
            log.debug("Serving {} query from the database: page size is outside what search allows", index);
            return Optional.empty();
        }
        SearchRequest request;
        try {
            SearchFilter filter = SearchParamsTranslator.toFilter(route.normalise(params), route.definition());
            List<SearchSort> sort = route.toSort(pageable.getSort());
            request = new SearchRequest(index, q.trim(), filter, scope.get(), sort,
                    pageable.getPageNumber(), pageable.getPageSize(), List.of(), null);
        } catch (IllegalArgumentException ex) {
            log.debug("Serving {} query from the database: {}", index, ex.getMessage());
            return Optional.empty();
        }
        SearchPage result;
        try {
            result = gateway.search(request);
        } catch (SearchUnavailableException ex) {
            log.debug("Search unavailable for {}, falling back to the database: {}", index, ex.getMessage());
            return Optional.empty();
        }
        return Optional.of(toPage(result, pageable, hydrate, uuidOf));
    }

    private static <T> Page<T> toPage(SearchPage result, Pageable pageable,
                                      Function<List<UUID>, List<T>> hydrate, Function<T, UUID> uuidOf) {
        List<UUID> uuids = result.hits().stream().map(SearchHit::uuid).toList();
        if (uuids.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, result.totalHits());
        }
        Map<UUID, T> byUuid = new LinkedHashMap<>();
        for (T dto : hydrate.apply(uuids)) {
            byUuid.put(uuidOf.apply(dto), dto);
        }
        // Hit order is the ranking; a hit the database no longer returns (stale, or no longer
        // visible to the caller) is dropped rather than shown.
        List<T> ordered = new ArrayList<>(uuids.size());
        for (UUID uuid : uuids) {
            T dto = byUuid.get(uuid);
            if (dto != null) {
                ordered.add(dto);
            }
        }
        return new PageImpl<>(ordered, pageable, result.totalHits());
    }

    /**
     * How one endpoint's parameters map onto an index.
     *
     * @param definition          the index definition, whose filterable and sortable lists are the allow-lists
     * @param sortAliases         request sort properties (normalised: no underscores, lower case) that
     *                            name a document attribute differently, e.g. {@code createddate -> created_at}
     * @param lowercaseAttributes attributes stored lower case (enum values such as {@code status}), whose
     *                            filter values are lower-cased so {@code status=PUBLISHED} keeps working
     */
    public record Route(SearchIndexDefinition definition, Map<String, String> sortAliases, Set<String> lowercaseAttributes) {

        public Route {
            sortAliases = sortAliases == null ? Map.of() : Map.copyOf(sortAliases);
            lowercaseAttributes = lowercaseAttributes == null ? Set.of() : Set.copyOf(lowercaseAttributes);
        }

        Map<String, String> normalise(Map<String, String> params) {
            if (params == null || params.isEmpty() || lowercaseAttributes.isEmpty()) {
                return params;
            }
            Map<String, String> normalised = new HashMap<>(params.size());
            params.forEach((key, value) -> normalised.put(key,
                    value != null && key != null && lowercaseAttributes.contains(fieldOf(key))
                            ? value.toLowerCase(Locale.ROOT) : value));
            return normalised;
        }

        /**
         * The page request's sort as search sorts. Unsorted means relevance. A property outside the
         * sortable allow-list throws, which sends the read to the database.
         */
        List<SearchSort> toSort(Sort sort) {
            if (sort == null || sort.isUnsorted()) {
                return List.of();
            }
            StringBuilder expression = new StringBuilder();
            for (Sort.Order order : sort) {
                String property = order.getProperty();
                String attribute = sortAliases.getOrDefault(squash(property), property);
                if (!expression.isEmpty()) {
                    expression.append(',');
                }
                expression.append(attribute).append(',').append(order.isAscending() ? "asc" : "desc");
            }
            return SearchParamsTranslator.toSort(expression.toString(), definition);
        }

        private String fieldOf(String key) {
            String field = key;
            int lastUnderscore = key.lastIndexOf('_');
            if (lastUnderscore > 0) {
                String suffix = key.substring(lastUnderscore + 1).toLowerCase(Locale.ROOT);
                if (Set.of("eq", "noteq", "in", "notin", "gt", "gte", "lt", "lte", "between").contains(suffix)) {
                    field = key.substring(0, lastUnderscore);
                }
            }
            String squashed = squash(field);
            return lowercaseAttributes.stream().filter(attribute -> squash(attribute).equals(squashed))
                    .findFirst().orElse(field);
        }

        private static String squash(String value) {
            return value.replace("_", "").toLowerCase(Locale.ROOT);
        }
    }
}
