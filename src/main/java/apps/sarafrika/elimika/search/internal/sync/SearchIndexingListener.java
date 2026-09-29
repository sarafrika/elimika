package apps.sarafrika.elimika.search.internal.sync;

import apps.sarafrika.elimika.search.config.SearchConfiguration;
import apps.sarafrika.elimika.search.internal.state.SearchIndexState;
import apps.sarafrika.elimika.search.internal.state.SearchIndexStateStore;
import apps.sarafrika.elimika.search.internal.state.SearchIndexStatus;
import apps.sarafrika.elimika.shared.search.SearchDocument;
import apps.sarafrika.elimika.shared.search.SearchDocumentSource;
import apps.sarafrika.elimika.shared.search.SearchGateway;
import apps.sarafrika.elimika.shared.search.SearchIndexRequested;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Applies {@link SearchIndexRequested} to the engine: reloads the requested documents from their
 * source, upserts the ones that should be indexed and deletes the rest.
 * <p>
 * A persistent Spring Modulith listener, on the same pattern as the wallet credit listener:
 * <ul>
 *     <li>{@code @TransactionalEventListener} makes Modulith record an {@code event_publication} row
 *     in the publishing transaction, before this runs;</li>
 *     <li>{@code fallbackExecution = true} also runs it for requests published outside a
 *     transaction (the admin sync endpoint, jobs);</li>
 *     <li>{@code @Async} on the single-threaded {@code searchIndexExecutor} keeps it off the request
 *     thread and applies writes in commit order;</li>
 *     <li>failures are rethrown, so the publication stays incomplete and is resubmitted by
 *     {@link IncompletePublicationResubmitter} and on restart.</li>
 * </ul>
 * Replaying a request is always safe: it reloads the current rows, so it can only converge.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "search", name = "enabled", havingValue = "true")
public class SearchIndexingListener {

    static final int CHUNK_SIZE = 500;

    private final SearchSourceRegistry registry;
    private final SearchGateway gateway;
    private final SearchIndexStateStore stateStore;

    @Async(SearchConfiguration.INDEX_EXECUTOR)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(fallbackExecution = true)
    public void on(SearchIndexRequested event) {
        Optional<SearchDocumentSource<SearchDocument>> found = registry.find(event.index());
        if (found.isEmpty()) {
            // Not an error worth retrying forever: the source was removed or renamed.
            log.warn("Ignoring search index request for unknown index {}", event.index());
            return;
        }
        SearchDocumentSource<SearchDocument> source = found.get();

        Set<UUID> uuids = new LinkedHashSet<>(event.uuids());
        for (String key : event.fanOutKeys()) {
            uuids.addAll(source.resolveFanOut(key));
        }
        if (uuids.isEmpty()) {
            return;
        }

        List<String> targets = targets(event.index());
        List<UUID> all = new ArrayList<>(uuids);
        for (int from = 0; from < all.size(); from += CHUNK_SIZE) {
            List<UUID> chunk = all.subList(from, Math.min(from + CHUNK_SIZE, all.size()));
            sync(source, targets, chunk);
        }
        log.debug("Synced {} document(s) of {} to {}", uuids.size(), event.index(), targets);
    }

    private void sync(SearchDocumentSource<SearchDocument> source, List<String> targets, List<UUID> chunk) {
        List<SearchDocument> documents = source.loadByUuids(chunk);
        Set<UUID> present = new HashSet<>();
        for (SearchDocument document : documents) {
            present.add(document.uuid());
        }
        List<UUID> absent = chunk.stream().filter(uuid -> !present.contains(uuid)).toList();
        for (String target : targets) {
            gateway.upsert(target, documents);
            gateway.delete(target, absent);
        }
    }

    /** The live index, plus the build index while a blue/green rebuild is filling it. */
    private List<String> targets(String index) {
        return stateStore.find(index)
                .filter(state -> state.getStatus() == SearchIndexStatus.REBUILDING)
                .map(SearchIndexState::getBuildIndexName)
                .map(build -> List.of(index, build))
                .orElse(List.of(index));
    }
}
