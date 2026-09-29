package apps.sarafrika.elimika.search.internal.sync;

import apps.sarafrika.elimika.search.internal.state.SearchIndexStateStore;
import apps.sarafrika.elimika.search.internal.state.SearchIndexStatus;
import apps.sarafrika.elimika.shared.search.SearchDocument;
import apps.sarafrika.elimika.shared.search.SearchDocumentSource;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Rebuilds every index once a night, which is what bounds how stale a denormalised copy can get.
 * <p>
 * Class, job and course documents copy names owned by other modules - organisation, branch, course,
 * program, instructor and course-creator names. A rename in the owning module fires no trigger on the
 * copying index (a module cannot register triggers on another module's entities), so the copy is
 * refreshed only when the document itself changes or the index is rebuilt. The dataset is small
 * enough that a nightly blue/green rebuild of everything is cheap, and it also repairs any drift the
 * reconciler reported.
 * <p>
 * Runs on {@code search.full-rebuild-cron} (default {@code 0 30 1 * * *}, 01:30 UTC; {@code -}
 * disables it). An index that is already {@code REBUILDING} - an admin-triggered rebuild, or one
 * interrupted by a restart that will resume from its checkpoint - is skipped. The rest are queued on
 * the rebuilder's single thread, so they run one after another and the scheduler thread is not held.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "search", name = "enabled", havingValue = "true")
public class SearchFullRebuildScheduler {

    private final SearchSourceRegistry registry;
    private final SearchIndexStateStore stateStore;
    private final SearchIndexRebuilder rebuilder;

    public SearchFullRebuildScheduler(
            SearchSourceRegistry registry,
            SearchIndexStateStore stateStore,
            SearchIndexRebuilder rebuilder
    ) {
        this.registry = registry;
        this.stateStore = stateStore;
        this.rebuilder = rebuilder;
    }

    @Scheduled(cron = "${search.full-rebuild-cron:0 30 1 * * *}", zone = "UTC")
    public void rebuildAll() {
        List<String> queued = queueRebuilds();
        log.info("Nightly search rebuild queued {} index(es): {}", queued.size(), queued);
    }

    /** Queues a rebuild of every index that is not already rebuilding. Returns the indexes queued. */
    List<String> queueRebuilds() {
        List<String> queued = new ArrayList<>();
        for (SearchDocumentSource<SearchDocument> source : registry.all()) {
            String index = source.definition().name();
            try {
                if (isRebuilding(index)) {
                    log.info("Nightly rebuild skips search index {}: a rebuild is already in progress", index);
                    continue;
                }
                if (rebuilder.rebuildAsync(index)) {
                    queued.add(index);
                } else {
                    log.info("Nightly rebuild skips search index {}: a rebuild is already queued", index);
                }
            } catch (RuntimeException ex) {
                log.error("Nightly rebuild could not queue search index {}: {}", index, ex.getMessage(), ex);
            }
        }
        return queued;
    }

    private boolean isRebuilding(String index) {
        return stateStore.find(index)
                .map(state -> state.getStatus() == SearchIndexStatus.REBUILDING)
                .orElse(false);
    }
}
