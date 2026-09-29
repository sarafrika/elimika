package apps.sarafrika.elimika.classes.search;

import apps.sarafrika.elimika.shared.search.SearchAvailability;
import apps.sarafrika.elimika.shared.search.SearchGateway;
import apps.sarafrika.elimika.shared.search.SearchHit;
import apps.sarafrika.elimika.shared.search.SearchIndexRequests;
import apps.sarafrika.elimika.shared.search.SearchPage;
import apps.sarafrika.elimika.shared.search.SearchRequest;
import apps.sarafrika.elimika.shared.search.SearchUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The classes module's one door to the search platform: routes a {@code q} read to the engine when
 * the index is read-enabled, and enqueues re-indexing for writes that bypass JPA.
 * <p>
 * A read answers with the matching UUIDs in rank order, or empty when reads are off or the engine
 * cannot answer, which is the caller's cue to fall back to the database. Hydration stays with the
 * services, so redaction and response shapes are exactly those of the database path.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClassesSearch {

    private final SearchAvailability searchAvailability;
    private final SearchGateway searchGateway;
    private final SearchIndexRequests searchIndexRequests;

    /** The ranked page of matches: document UUIDs in hit order and the total number of matches. */
    public record Hits(List<UUID> uuids, long totalHits) {
    }

    public boolean isReadEnabled(String index) {
        return searchAvailability.isReadEnabled(index);
    }

    /** Runs {@code request}, or answers empty when reads for its index are off or the engine is unavailable. */
    public Optional<Hits> search(SearchRequest request) {
        if (!searchAvailability.isReadEnabled(request.index())) {
            return Optional.empty();
        }
        try {
            SearchPage page = searchGateway.search(request);
            return Optional.of(new Hits(page.hits().stream().map(SearchHit::uuid).toList(), page.totalHits()));
        } catch (SearchUnavailableException ex) {
            log.warn("Search on {} unavailable, falling back to the database: {}", request.index(), ex.getMessage());
            return Optional.empty();
        }
    }

    /** Re-index a marketplace job after a write that fires no entity trigger (bulk deletes). */
    public void reindexJob(UUID jobUuid) {
        searchIndexRequests.enqueue(MarketplaceJobSearchSource.INDEX, jobUuid);
    }
}
