package apps.sarafrika.elimika.classes.search;

import apps.sarafrika.elimika.shared.search.SearchAvailability;
import apps.sarafrika.elimika.shared.search.SearchGateway;
import apps.sarafrika.elimika.shared.search.SearchHit;
import apps.sarafrika.elimika.shared.search.SearchIndexRequests;
import apps.sarafrika.elimika.shared.search.SearchPage;
import apps.sarafrika.elimika.shared.search.SearchRequest;
import apps.sarafrika.elimika.shared.search.SearchUnavailableException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * The classes module's one door to the search platform: serves a {@code q} read from the engine, and
 * enqueues re-indexing for writes that bypass JPA.
 * <p>
 * A read answers with the matching UUIDs in rank order. Free text has no database fallback: when
 * search is off, the index's reads are not enabled, or the engine fails, it throws
 * {@link SearchUnavailableException} (503). Hydration stays with the services, so redaction and
 * response shapes are exactly those of the relational listings.
 */
@Component
@RequiredArgsConstructor
public class ClassesSearch {

    private final SearchAvailability searchAvailability;
    private final SearchGateway searchGateway;
    private final SearchIndexRequests searchIndexRequests;

    /** The ranked page of matches: document UUIDs in hit order and the total number of matches. */
    public record Hits(List<UUID> uuids, long totalHits) {
    }

    /**
     * Runs {@code request}.
     *
     * @throws SearchUnavailableException when reads for its index are off or the engine is unavailable
     */
    public Hits search(SearchRequest request) {
        if (!searchAvailability.isReadEnabled(request.index())) {
            throw new SearchUnavailableException("Search is not enabled for " + request.index());
        }
        SearchPage page = searchGateway.search(request);
        return new Hits(page.hits().stream().map(SearchHit::uuid).toList(), page.totalHits());
    }

    /** Re-index a marketplace job after a write that fires no entity trigger (bulk deletes). */
    public void reindexJob(UUID jobUuid) {
        searchIndexRequests.enqueue(MarketplaceJobSearchSource.INDEX, jobUuid);
    }
}
