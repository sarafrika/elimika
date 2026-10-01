package apps.sarafrika.elimika.search.internal.meilisearch;

import apps.sarafrika.elimika.shared.search.FederatedSearchResult;
import apps.sarafrika.elimika.shared.search.SearchGateway;
import apps.sarafrika.elimika.shared.search.SearchIndexAdmin;
import apps.sarafrika.elimika.shared.search.SearchIndexDefinition;
import apps.sarafrika.elimika.shared.search.SearchPage;
import apps.sarafrika.elimika.shared.search.SearchRequest;
import apps.sarafrika.elimika.shared.search.SearchUnavailableException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Stands in for the engine while {@code search.enabled=false}, so modules can inject
 * {@link SearchGateway} unconditionally. Every call refuses with {@link SearchUnavailableException},
 * which is the signal callers already handle by falling back to the database. No HTTP is made.
 */
@Component
@ConditionalOnProperty(prefix = "search", name = "enabled", havingValue = "false", matchIfMissing = true)
public class DisabledSearchGateway implements SearchGateway, SearchIndexAdmin {

    private static SearchUnavailableException disabled() {
        return new SearchUnavailableException("Search is disabled (search.enabled=false)");
    }

    @Override
    public SearchPage search(SearchRequest request) {
        throw disabled();
    }

    @Override
    public FederatedSearchResult multiSearch(List<SearchRequest> requests, int limit) {
        throw disabled();
    }

    @Override
    public FederatedSearchResult federatedSearch(List<SearchRequest> requests, int offset, int limit) {
        throw disabled();
    }

    @Override
    public List<SearchPage> multiSearchPerIndex(List<SearchRequest> requests) {
        throw disabled();
    }

    @Override
    public void upsert(String index, List<?> documents) {
        throw disabled();
    }

    @Override
    public void delete(String index, Collection<UUID> uuids) {
        throw disabled();
    }

    @Override
    public void ensureIndex(String uid, SearchIndexDefinition definition) {
        throw disabled();
    }

    @Override
    public void swapIndexes(String first, String second) {
        throw disabled();
    }

    @Override
    public void deleteIndex(String uid) {
        throw disabled();
    }

    @Override
    public Optional<SearchIndexStats> stats(String uid) {
        throw disabled();
    }
}
