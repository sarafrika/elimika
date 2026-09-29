package apps.sarafrika.elimika.search.internal.state;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * State transitions of {@code search_index_state}. Every write commits on its own
 * ({@code REQUIRES_NEW}), so a rebuild's progress and failures are recorded even when the work around
 * them rolls back.
 */
@Component
@RequiredArgsConstructor
public class SearchIndexStateStore {

    private static final int MAX_ERROR_LENGTH = 4000;

    private final SearchIndexStateRepository repository;

    @Transactional(readOnly = true)
    public Optional<SearchIndexState> find(String index) {
        return repository.findById(index);
    }

    @Transactional(readOnly = true)
    public List<SearchIndexState> findAll() {
        return repository.findAll();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SearchIndexState getOrCreate(String index) {
        return repository.findById(index).orElseGet(() -> repository.save(SearchIndexState.initial(index)));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markStale(String index) {
        SearchIndexState state = load(index);
        state.setStatus(SearchIndexStatus.STALE);
        repository.save(state);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markRebuilding(String index, String buildIndexName, long checkpoint) {
        SearchIndexState state = load(index);
        state.setStatus(SearchIndexStatus.REBUILDING);
        state.setBuildIndexName(buildIndexName);
        state.setRebuildCheckpointId(checkpoint);
        state.setLastError(null);
        repository.save(state);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void checkpoint(String index, long lastId) {
        SearchIndexState state = load(index);
        state.setRebuildCheckpointId(lastId);
        repository.save(state);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markReady(String index, int schemaVersion, Long documentCount) {
        SearchIndexState state = load(index);
        state.setStatus(SearchIndexStatus.READY);
        state.setSchemaVersion(schemaVersion);
        state.setBuildIndexName(null);
        state.setRebuildCheckpointId(null);
        state.setLastBuiltAt(Instant.now());
        state.setDocumentCount(documentCount);
        state.setDrift(null);
        state.setLastError(null);
        repository.save(state);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(String index, String error) {
        SearchIndexState state = load(index);
        state.setStatus(SearchIndexStatus.FAILED);
        state.setBuildIndexName(null);
        state.setRebuildCheckpointId(null);
        state.setLastError(truncate(error));
        repository.save(state);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordReconciliation(String index, long documentCount, Long drift) {
        SearchIndexState state = load(index);
        state.setDocumentCount(documentCount);
        state.setDrift(drift);
        state.setLastReconciledAt(Instant.now());
        repository.save(state);
    }

    private SearchIndexState load(String index) {
        return repository.findById(index).orElseGet(() -> SearchIndexState.initial(index));
    }

    private static String truncate(String error) {
        if (error == null) {
            return null;
        }
        return error.length() > MAX_ERROR_LENGTH ? error.substring(0, MAX_ERROR_LENGTH) : error;
    }
}
