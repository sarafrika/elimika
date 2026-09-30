package apps.sarafrika.elimika.search.internal.global;

import apps.sarafrika.elimika.search.dto.GlobalSearchResponse;
import apps.sarafrika.elimika.search.dto.TypeSearchResponse;
import apps.sarafrika.elimika.shared.search.GlobalSearchHit;
import apps.sarafrika.elimika.shared.search.GlobalSearchProvider;
import apps.sarafrika.elimika.shared.search.SearchAvailability;
import apps.sarafrika.elimika.shared.search.SearchFilter;
import apps.sarafrika.elimika.shared.search.SearchGateway;
import apps.sarafrika.elimika.shared.search.SearchIndexDefinition;
import apps.sarafrika.elimika.shared.search.SearchPage;
import apps.sarafrika.elimika.shared.search.SearchParamsTranslator;
import apps.sarafrika.elimika.shared.search.SearchRequest;
import apps.sarafrika.elimika.shared.search.SearchResults;
import apps.sarafrika.elimika.shared.search.SearchScope;
import apps.sarafrika.elimika.shared.search.SearchSort;
import apps.sarafrika.elimika.shared.search.SearchUnavailableException;
import apps.sarafrika.elimika.shared.utils.PageMetadata;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/**
 * Global search: one query across every type the caller may see, and a paged "see all" per type.
 * <p>
 * The search module owns the query, the owning modules own everything else through their
 * {@link GlobalSearchProvider}: whether the caller may see a type, the boundary, which attributes
 * the text may match and how a hit is displayed. A type is searched only when its provider returns a
 * scope <em>and</em> its index is read-enabled ({@code search.read-enabled.<index>}), so global
 * search goes live one index at a time, like the module listings.
 * <p>
 * Results are grouped per type rather than merged into one ranking: relevance scores from indexes
 * with different attributes and ranking rules are not comparable, a grouped dropdown is what the UI
 * renders, and grouping yields an exact total per type.
 * <p>
 * The query text is never logged: people search for names and email addresses, which are personal
 * data. Only the types and the text's length are.
 */
@Slf4j
@Service
public class GlobalSearchService {

    public static final int MIN_QUERY_LENGTH = 2;
    public static final int MAX_QUERY_LENGTH = 200;
    public static final int DEFAULT_LIMIT = 5;
    public static final int MAX_LIMIT = 20;
    public static final int DEFAULT_PAGE_SIZE = 20;

    /** The order types are returned in when the caller does not name any. */
    static final List<String> DEFAULT_ORDER = List.of(
            "courses", "programs", "classes", "marketplace_jobs", "instructors", "organisations", "people", "rubrics",
            "course_content");

    private static final String PEOPLE = "people";
    private static final int MAX_ECHOED_TYPE_LENGTH = 40;

    private final ObjectProvider<GlobalSearchProvider> providerBeans;
    private final SearchGateway gateway;
    private final SearchAvailability availability;
    private volatile Map<String, GlobalSearchProvider> providers;

    public GlobalSearchService(ObjectProvider<GlobalSearchProvider> providerBeans, SearchGateway gateway,
                               SearchAvailability availability) {
        this.providerBeans = providerBeans;
        this.gateway = gateway;
        this.availability = availability;
    }

    /**
     * Searches {@code types} (every type when blank) for {@code q}, at most {@code limit} hits each.
     *
     * @throws IllegalArgumentException    for a missing or short query, a bad limit or an unknown type (400)
     * @throws SearchUnavailableException when search is off, none of the types is enabled, or the engine fails (503)
     */
    public GlobalSearchResponse search(String q, String types, Integer limit) {
        String text = requireQuery(q);
        int perType = limit == null ? DEFAULT_LIMIT : limit;
        if (perType < 1 || perType > MAX_LIMIT) {
            throw new IllegalArgumentException("limit must be between 1 and " + MAX_LIMIT);
        }
        List<String> requested = parseTypes(types);
        requireEnabled();

        List<GlobalSearchProvider> searched = new ArrayList<>();
        List<SearchRequest> requests = new ArrayList<>();
        boolean anyReadEnabled = false;
        for (String type : requested) {
            GlobalSearchProvider provider = providers().get(type);
            if (!availability.isReadEnabled(provider.index())) {
                continue;
            }
            anyReadEnabled = true;
            Optional<SearchScope> scope = provider.scopeForCurrentCaller();
            if (scope.isEmpty()) {
                continue;
            }
            searched.add(provider);
            requests.add(new SearchRequest(provider.index(), text, null, scope.get(), List.of(), 0, perType,
                    List.of(), provider.searchOnForCurrentCaller()));
        }
        if (!anyReadEnabled) {
            throw new SearchUnavailableException("Search is not enabled for " + String.join(", ", requested));
        }
        log.debug("Global search over {} (query length {})", searched.stream().map(GlobalSearchProvider::type).toList(),
                text.length());
        if (requests.isEmpty()) {
            return new GlobalSearchResponse(List.of(), Map.of());
        }

        List<SearchPage> pages = run(requested, () -> gateway.multiSearchPerIndex(requests));
        List<GlobalSearchHit> hits = new ArrayList<>();
        Map<String, Long> totals = new LinkedHashMap<>();
        for (int i = 0; i < searched.size(); i++) {
            GlobalSearchProvider provider = searched.get(i);
            SearchPage page = pages.get(i);
            List<GlobalSearchHit> shown = authorised(provider, page);
            hits.addAll(shown);
            totals.put(provider.type(), SearchResults.total(page.totalHits(), page.hits().size(), shown.size()));
        }
        return new GlobalSearchResponse(hits, Collections.unmodifiableMap(totals));
    }

    /**
     * One page of one type. {@code params} filter in the {@code field_op} vocabulary over the index's
     * filterable attributes; {@code facets} must be filterable attributes too.
     *
     * @throws IllegalArgumentException    for an unknown type, filter, facet or sort, or a bad page (400)
     * @throws AccessDeniedException       when the caller may not see this type (403)
     * @throws SearchUnavailableException when search or this type is not enabled, or the engine fails (503)
     */
    public TypeSearchResponse searchType(String type, String q, Map<String, String> params, String facets,
                                         String sort, Integer page, Integer size) {
        GlobalSearchProvider provider = requireProvider(type);
        SearchIndexDefinition definition = provider.definition();
        String text = q == null || q.isBlank() ? null : requireQuery(q);
        int pageNumber = page == null ? 0 : page;
        int pageSize = size == null ? DEFAULT_PAGE_SIZE : size;
        if (pageNumber < 0) {
            throw new IllegalArgumentException("page must be 0 or greater");
        }
        if (pageSize < 1 || pageSize > SearchRequest.MAX_SIZE) {
            throw new IllegalArgumentException("size must be between 1 and " + SearchRequest.MAX_SIZE);
        }
        SearchFilter filter = SearchParamsTranslator.toFilter(params, definition);
        List<SearchSort> sorts = SearchParamsTranslator.toSort(sort, definition);
        List<String> facetAttributes = parseFacets(facets, definition);

        requireEnabled();
        if (!availability.isReadEnabled(provider.index())) {
            throw new SearchUnavailableException("Search is not enabled for " + provider.type());
        }
        SearchScope scope = provider.scopeForCurrentCaller()
                .orElseThrow(() -> new AccessDeniedException("You may not search " + provider.type()));

        SearchRequest request = new SearchRequest(provider.index(), text, filter, scope, sorts, pageNumber, pageSize,
                facetAttributes, provider.searchOnForCurrentCaller());
        log.debug("Search of {} (query length {})", provider.type(), text == null ? 0 : text.length());
        SearchPage result = run(List.of(provider.type()), () -> gateway.search(request));

        List<GlobalSearchHit> content = authorised(provider, result);
        PageMetadata metadata = PageMetadata.from(new PageImpl<>(content, PageRequest.of(pageNumber, pageSize),
                SearchResults.total(result.totalHits(), result.hits().size(), content.size())));
        return new TypeSearchResponse(content, metadata, result.facetDistribution());
    }

    /**
     * The page's hits as results, after the provider's database re-check: the engine narrows, SQL
     * authorizes. When the re-check drops hits the caller restates the total through
     * {@link SearchResults#total}.
     */
    private static List<GlobalSearchHit> authorised(GlobalSearchProvider provider, SearchPage page) {
        List<GlobalSearchHit> hits = page.hits().stream()
                .filter(hit -> hit.uuid() != null)
                .map(provider::toHit)
                .toList();
        return hits.isEmpty() ? hits : List.copyOf(provider.recheck(hits));
    }

    private <T> T run(List<String> types, java.util.function.Supplier<T> call) {
        try {
            return call.get();
        } catch (SearchUnavailableException ex) {
            if (types.contains(PEOPLE)) {
                // The engine's error can echo the request; with people in it, that may be personal data.
                log.warn("Global search over {} unavailable", types);
            } else {
                log.warn("Global search over {} unavailable: {}", types, ex.getMessage());
            }
            throw ex;
        }
    }

    private void requireEnabled() {
        if (!availability.isEnabled()) {
            throw new SearchUnavailableException("Search is disabled");
        }
    }

    private static String requireQuery(String q) {
        String text = q == null ? "" : q.trim();
        if (text.length() < MIN_QUERY_LENGTH) {
            throw new IllegalArgumentException("q is required and must be at least " + MIN_QUERY_LENGTH + " characters");
        }
        if (text.length() > MAX_QUERY_LENGTH) {
            throw new IllegalArgumentException("q must be at most " + MAX_QUERY_LENGTH + " characters");
        }
        return text;
    }

    /** The requested types in request order, without duplicates; every known type when blank. */
    List<String> parseTypes(String types) {
        if (types == null || types.isBlank()) {
            List<String> all = new ArrayList<>(DEFAULT_ORDER.stream().filter(providers()::containsKey).toList());
            providers().keySet().stream().filter(type -> !all.contains(type)).forEach(all::add);
            return all;
        }
        Set<String> parsed = new LinkedHashSet<>();
        for (String raw : types.split(",")) {
            String type = raw.trim().toLowerCase(Locale.ROOT);
            if (type.isEmpty()) {
                continue;
            }
            requireProvider(type);
            parsed.add(type);
        }
        if (parsed.isEmpty()) {
            throw new IllegalArgumentException("types names no type");
        }
        return List.copyOf(parsed);
    }

    private GlobalSearchProvider requireProvider(String type) {
        String key = type == null ? "" : type.trim().toLowerCase(Locale.ROOT);
        GlobalSearchProvider provider = providers().get(key);
        if (provider == null) {
            throw new IllegalArgumentException("Unknown search type: " + sanitise(key)
                    + ". Known types: " + String.join(", ", providers().keySet()));
        }
        return provider;
    }

    private static List<String> parseFacets(String facets, SearchIndexDefinition definition) {
        if (facets == null || facets.isBlank()) {
            return List.of();
        }
        List<String> attributes = Arrays.stream(facets.split(","))
                .map(String::trim)
                .filter(facet -> !facet.isEmpty())
                .distinct()
                .toList();
        for (String attribute : attributes) {
            if (!definition.filterableAttributes().contains(attribute)) {
                throw new IllegalArgumentException("Unsupported facet: " + sanitise(attribute));
            }
        }
        return attributes;
    }

    private Map<String, GlobalSearchProvider> providers() {
        Map<String, GlobalSearchProvider> resolved = providers;
        if (resolved != null) {
            return resolved;
        }
        synchronized (this) {
            if (providers == null) {
                Map<String, GlobalSearchProvider> byType = new LinkedHashMap<>();
                providerBeans.orderedStream().forEach(provider -> {
                    GlobalSearchProvider previous = byType.putIfAbsent(provider.type(), provider);
                    if (previous != null) {
                        throw new IllegalStateException("Two global search providers claim type '" + provider.type() + "'");
                    }
                });
                providers = Collections.unmodifiableMap(byType);
            }
            return providers;
        }
    }

    private static String sanitise(String value) {
        String cleaned = value.replaceAll("[^A-Za-z0-9_]", "");
        return cleaned.length() > MAX_ECHOED_TYPE_LENGTH ? cleaned.substring(0, MAX_ECHOED_TYPE_LENGTH) : cleaned;
    }
}
