package apps.sarafrika.elimika.course.internal.search;

import apps.sarafrika.elimika.shared.search.SearchAvailability;
import apps.sarafrika.elimika.shared.search.SearchFilter;
import apps.sarafrika.elimika.shared.search.SearchGateway;
import apps.sarafrika.elimika.shared.search.SearchResults;
import apps.sarafrika.elimika.shared.search.SearchIndexDefinition;
import apps.sarafrika.elimika.shared.search.SearchPage;
import apps.sarafrika.elimika.shared.search.SearchParamsTranslator;
import apps.sarafrika.elimika.shared.search.SearchRequest;
import apps.sarafrika.elimika.shared.search.SearchScope;
import apps.sarafrika.elimika.shared.search.SearchSort;
import apps.sarafrika.elimika.shared.search.SearchUnavailableException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Routes a catalogue read that carries free text ({@code q}) to the search engine.
 * <p>
 * Free text is served only by search: there is no database fallback. The remaining
 * {@code searchParams} are translated against the index's filterable allow-list, the caller's scope is
 * ANDed on, and the hits are hydrated back into the endpoint's existing DTOs in hit order, so the
 * response shape does not change.
 * <ul>
 *     <li>Search off, the index's read flag off, or the engine failing raises
 *     {@link SearchUnavailableException}, which the global handler maps to 503.</li>
 *     <li>A parameter or sort with no index equivalent, an unpaged request, or a page larger than
 *     {@value SearchRequest#MAX_SIZE} raises {@link IllegalArgumentException} naming it (400).</li>
 * </ul>
 * Requests without {@code q} never reach this class; the caller serves them from its relational
 * SQL filters (see {@link #withoutQuery(Map)}).
 */
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
     * The params the relational SQL path should see: a blank {@code q} removed (the specification
     * builders reject unknown keys). A {@code q} with text is refused, since the database never
     * answers free text; callers route those through {@link #search} first.
     *
     * @throws IllegalArgumentException when {@code q} carries text on an endpoint without search
     */
    public static Map<String, String> withoutQuery(Map<String, String> searchParams) {
        if (searchParams == null || !searchParams.containsKey(QUERY_PARAM)) {
            return searchParams;
        }
        if (queryText(searchParams) != null) {
            throw new IllegalArgumentException("The q parameter is not supported on this endpoint");
        }
        Map<String, String> params = new HashMap<>(searchParams);
        params.remove(QUERY_PARAM);
        return params;
    }

    /**
     * Runs a free-text read through search.
     *
     * @param route      the index and the endpoint-specific translation rules
     * @param q          the free-text query; must not be blank
     * @param params     the endpoint's other search params (reserved keys are ignored)
     * @param pageable   the bound page request; its sort is honoured only from the sortable allow-list
     * @param scope      the caller's visibility boundary
     * @param hydrate    loads the DTOs for the hit UUIDs in one batch query; order does not matter
     * @param uuidOf     the UUID of a hydrated DTO
     * @throws SearchUnavailableException when search, or this index's reads, are off, or the engine fails (503)
     * @throws IllegalArgumentException   for a parameter, sort or page the index cannot serve (400)
     */
    public <T> Page<T> search(
            Route route,
            String q,
            Map<String, String> params,
            Pageable pageable,
            Supplier<SearchScope> scope,
            Function<List<UUID>, List<T>> hydrate,
            Function<T, UUID> uuidOf
    ) {
        String index = route.definition().name();
        if (!StringUtils.hasText(q)) {
            throw new IllegalArgumentException("q must not be blank");
        }
        if (pageable == null || pageable.isUnpaged()) {
            throw new IllegalArgumentException("A request with q must be paged");
        }
        if (pageable.getPageSize() > SearchRequest.MAX_SIZE) {
            throw new IllegalArgumentException("size must be at most " + SearchRequest.MAX_SIZE + " when q is present");
        }
        SearchFilter filter = SearchParamsTranslator.toFilter(route.normalise(params), route.definition());
        List<SearchSort> sort = route.toSort(pageable.getSort());
        if (!availability.isReadEnabled(index)) {
            throw new SearchUnavailableException("Search is not enabled for " + index);
        }
        SearchRequest request = new SearchRequest(index, q.trim(), filter, scope.get(), sort,
                pageable.getPageNumber(), pageable.getPageSize(), List.of(), null);
        return toPage(gateway.search(request), pageable, hydrate, uuidOf);
    }

    private static <T> Page<T> toPage(SearchPage result, Pageable pageable,
                                      Function<List<UUID>, List<T>> hydrate, Function<T, UUID> uuidOf) {
        List<UUID> uuids = SearchResults.hitUuids(result);
        if (uuids.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, result.totalHits());
        }
        // Hit order is the ranking; a hit the database no longer returns (stale, or no longer
        // visible to the caller) is dropped rather than shown, and the total restated.
        List<T> ordered = SearchResults.inHitOrder(uuids, hydrate.apply(uuids), uuidOf);
        return new PageImpl<>(ordered, pageable,
                SearchResults.total(result.totalHits(), result.hits().size(), ordered.size()));
    }

    /**
     * How one endpoint's parameters map onto an index.
     *
     * @param definition          the index definition, whose filterable and sortable lists are the allow-lists
     * @param sortAliases         request sort properties (normalised: no underscores, lower case) that
     *                            name a document attribute differently, e.g. {@code createddate -> created_at}
     * @param lowercaseAttributes attributes stored lower case (enum values such as {@code status}), whose
     *                            filter values are lower-cased so {@code status=PUBLISHED} keeps working
     * @param paramAliases        request filter fields (normalised) that name a document attribute
     *                            differently, e.g. {@code lifecyclestage -> status}
     * @param statusFlags         boolean request flags (normalised) that stand for one {@code status}
     *                            value, e.g. {@code ispublished -> published}: {@code true} becomes
     *                            {@code status = value}, {@code false} becomes {@code status != value}
     */
    public record Route(SearchIndexDefinition definition, Map<String, String> sortAliases,
                        Set<String> lowercaseAttributes, Map<String, String> paramAliases,
                        Map<String, String> statusFlags) {

        private static final Set<String> OPERATIONS =
                Set.of("eq", "noteq", "in", "notin", "gt", "gte", "lt", "lte", "between");

        public Route {
            sortAliases = sortAliases == null ? Map.of() : Map.copyOf(sortAliases);
            lowercaseAttributes = lowercaseAttributes == null ? Set.of() : Set.copyOf(lowercaseAttributes);
            paramAliases = paramAliases == null ? Map.of() : Map.copyOf(paramAliases);
            statusFlags = statusFlags == null ? Map.of() : Map.copyOf(statusFlags);
        }

        public Route(SearchIndexDefinition definition, Map<String, String> sortAliases, Set<String> lowercaseAttributes) {
            this(definition, sortAliases, lowercaseAttributes, Map.of(), Map.of());
        }

        /** Applies the aliases and lower-cases values of lower-case attributes. */
        Map<String, String> normalise(Map<String, String> params) {
            if (params == null || params.isEmpty()) {
                return params;
            }
            Map<String, String> normalised = new HashMap<>(params.size());
            params.forEach((key, value) -> {
                if (key == null) {
                    return;
                }
                String flagStatus = statusFlags.get(squash(key));
                if (flagStatus != null) {
                    if (value == null || value.isBlank()) {
                        return;
                    }
                    boolean on = Boolean.parseBoolean(value.trim());
                    put(normalised, on ? "status" : "status_noteq", flagStatus);
                    return;
                }
                String rewritten = aliasKey(key);
                put(normalised, rewritten, value != null && lowercaseAttributes.contains(fieldOf(rewritten))
                        ? value.toLowerCase(Locale.ROOT) : value);
            });
            return normalised;
        }

        private static void put(Map<String, String> params, String key, String value) {
            if (params.containsKey(key)) {
                throw new IllegalArgumentException("Conflicting filters on " + key);
            }
            params.put(key, value);
        }

        /** {@code lifecycle_stage_in} becomes {@code status_in} when {@code lifecyclestage -> status}. */
        private String aliasKey(String key) {
            String field = key;
            String suffix = "";
            int lastUnderscore = key.lastIndexOf('_');
            if (lastUnderscore > 0 && OPERATIONS.contains(key.substring(lastUnderscore + 1).toLowerCase(Locale.ROOT))) {
                field = key.substring(0, lastUnderscore);
                suffix = key.substring(lastUnderscore);
            }
            String alias = paramAliases.get(squash(field));
            return alias == null ? key : alias + suffix;
        }

        /**
         * The page request's sort as search sorts. Unsorted means relevance. A property outside the
         * sortable allow-list throws, which the global handler turns into a 400 naming it.
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
                if (OPERATIONS.contains(suffix)) {
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
