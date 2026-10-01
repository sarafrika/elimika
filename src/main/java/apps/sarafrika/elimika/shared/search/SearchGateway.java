package apps.sarafrika.elimika.shared.search;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * The engine-neutral search contract. Modules search through this and never see an engine type.
 * <p>
 * Every method throws {@link SearchUnavailableException} when search is disabled or the engine
 * cannot answer. Free text has no database fallback: callers let it propagate, and the global
 * handler answers 503 ("Search is unavailable").
 */
public interface SearchGateway {

    /** One page of one index, bounded by the request's scope. */
    SearchPage search(SearchRequest request);

    /**
     * Runs several requests and merges their hits into one ranking of at most {@code limit} hits.
     * Pagination, sorting and facets of the individual requests are ignored; scopes and filters are
     * not.
     */
    FederatedSearchResult multiSearch(List<SearchRequest> requests, int limit);

    /**
     * Runs several requests and merges their hits into one ranking, then returns the slice
     * {@code [offset, offset + limit)} of it - a page of a federated listing such as the public
     * catalogue. Unlike {@link #multiSearch(List, int)}, each request's sort is honoured, so the
     * merged ranking is ordered by it; the requests must then sort on comparable attributes, and the
     * indexes must rank compatibly, or the engine refuses ({@link SearchUnavailableException}).
     * Pagination and facets of the individual requests are ignored; scopes and filters are not.
     * {@link FederatedSearchResult#estimatedTotalHits()} is the engine's estimate across all requests.
     */
    FederatedSearchResult federatedSearch(List<SearchRequest> requests, int offset, int limit);

    /**
     * Runs several requests in one round trip without merging them: one page per request, in request
     * order, each with its own total, sort and facets. Used where results are shown grouped by index.
     */
    List<SearchPage> multiSearchPerIndex(List<SearchRequest> requests);

    /** Adds or replaces documents, keyed by the index's primary key. Waits until they are applied. */
    void upsert(String index, List<?> documents);

    /** Removes documents by primary key. Waits until they are applied. */
    void delete(String index, Collection<UUID> uuids);
}
