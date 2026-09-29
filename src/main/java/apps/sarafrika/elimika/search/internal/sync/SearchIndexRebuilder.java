package apps.sarafrika.elimika.search.internal.sync;

import apps.sarafrika.elimika.search.config.SearchProperties;
import apps.sarafrika.elimika.search.internal.state.SearchIndexState;
import apps.sarafrika.elimika.search.internal.state.SearchIndexStateStore;
import apps.sarafrika.elimika.search.internal.state.SearchIndexStatus;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.search.SearchBatch;
import apps.sarafrika.elimika.shared.search.SearchDocument;
import apps.sarafrika.elimika.shared.search.SearchDocumentSource;
import apps.sarafrika.elimika.shared.search.SearchGateway;
import apps.sarafrika.elimika.shared.search.SearchIndexAdmin;
import apps.sarafrika.elimika.shared.search.SearchIndexAdmin.SearchIndexStats;
import apps.sarafrika.elimika.shared.search.SearchIndexDefinition;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Rebuilds an index from its source without taking search offline (blue/green):
 * <ol>
 *     <li>create {@code <index>__build_<yyyyMMddHHmm>} and apply the definition's settings to it
 *     first, so documents are indexed once, with the final settings;</li>
 *     <li>copy the source into it in keyset batches ({@link SearchDocumentSource#loadAfter}), each in
 *     its own short read-only transaction, recording the last id copied as a checkpoint - a rebuild
 *     interrupted by a restart resumes from there;</li>
 *     <li>swap the build index with the live one atomically, then delete the old contents.</li>
 * </ol>
 * While it runs the index is {@code REBUILDING} and the indexing listener writes every change to both
 * indexes, so nothing committed during the copy is lost by the swap. On failure the build index is
 * dropped, the live index is left untouched and the state is {@code FAILED}.
 * <p>
 * Rebuilds run one at a time on their own thread; asking for an index that is already queued or
 * running is a no-op.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "search", name = "enabled", havingValue = "true")
public class SearchIndexRebuilder implements DisposableBean {

    private static final DateTimeFormatter BUILD_SUFFIX = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
    private static final String BUILD_MARKER = "__build_";

    private final SearchSourceRegistry registry;
    private final SearchGateway gateway;
    private final SearchIndexAdmin admin;
    private final SearchIndexStateStore stateStore;
    private final SearchProperties properties;
    private final TransactionTemplate readOnlyTransaction;
    private final ThreadPoolTaskExecutor rebuildExecutor;
    private final Set<String> scheduled = ConcurrentHashMap.newKeySet();

    public SearchIndexRebuilder(
            SearchSourceRegistry registry,
            SearchGateway gateway,
            SearchIndexAdmin admin,
            SearchIndexStateStore stateStore,
            SearchProperties properties,
            PlatformTransactionManager transactionManager
    ) {
        this.registry = registry;
        this.gateway = gateway;
        this.admin = admin;
        this.stateStore = stateStore;
        this.properties = properties;
        this.readOnlyTransaction = new TransactionTemplate(transactionManager);
        this.readOnlyTransaction.setReadOnly(true);

        this.rebuildExecutor = new ThreadPoolTaskExecutor();
        this.rebuildExecutor.setCorePoolSize(1);
        this.rebuildExecutor.setMaxPoolSize(1);
        this.rebuildExecutor.setQueueCapacity(100);
        this.rebuildExecutor.setThreadNamePrefix("search-rebuild-");
        this.rebuildExecutor.initialize();
    }

    /** Queues a rebuild of one index. Returns false when it is already queued or running. */
    public boolean rebuildAsync(String index) {
        SearchDocumentSource<SearchDocument> source = requireSource(index);
        String name = source.definition().name();
        if (!scheduled.add(name)) {
            return false;
        }
        try {
            rebuildExecutor.execute(() -> {
                try {
                    rebuild(name);
                } catch (Exception ex) {
                    log.error("Rebuild of search index {} failed: {}", name, ex.getMessage(), ex);
                } finally {
                    scheduled.remove(name);
                }
            });
            return true;
        } catch (TaskRejectedException ex) {
            scheduled.remove(name);
            throw ex;
        }
    }

    /** Queues a rebuild of every index. Returns the indexes queued. */
    public List<String> rebuildAllAsync() {
        return registry.all().stream()
                .map(source -> source.definition().name())
                .filter(this::rebuildAsync)
                .toList();
    }

    /** Queues a rebuild of every index owned by {@code module} (e.g. {@code course}). Returns the indexes queued. */
    public List<String> rebuildModuleAsync(String module) {
        return registry.ofModule(module).stream()
                .map(source -> source.definition().name())
                .filter(this::rebuildAsync)
                .toList();
    }

    /** Rebuilds every index on the calling thread. */
    public void rebuildAll() {
        registry.all().forEach(source -> rebuild(source.definition().name()));
    }

    /** Rebuilds every index owned by {@code module} on the calling thread. */
    public void rebuildModule(String module) {
        registry.ofModule(module).forEach(source -> rebuild(source.definition().name()));
    }

    /**
     * Rebuilds one index on the calling thread, resuming an interrupted rebuild from its checkpoint.
     *
     * @throws RuntimeException when the rebuild fails; the state is FAILED and the live index untouched
     */
    public void rebuild(String index) {
        SearchDocumentSource<SearchDocument> source = requireSource(index);
        SearchIndexDefinition definition = source.definition();
        SearchIndexState state = stateStore.getOrCreate(index);

        String build;
        long checkpoint;
        if (isResumable(state)) {
            build = state.getBuildIndexName();
            checkpoint = state.getRebuildCheckpointId();
            log.info("Resuming rebuild of search index {} into {} after id {}", index, build, checkpoint);
        } else {
            build = index + BUILD_MARKER + ZonedDateTime.now(ZoneOffset.UTC).format(BUILD_SUFFIX);
            checkpoint = 0L;
            log.info("Rebuilding search index {} into {}", index, build);
        }

        try {
            // The live index must exist to be swapped with; settings go on the build index first.
            admin.ensureIndex(index, definition);
            admin.ensureIndex(build, definition);
            stateStore.markRebuilding(index, build, checkpoint);

            int batchSize = Math.max(1, properties.getRebuildBatchSize());
            long copied = 0;
            while (true) {
                long after = checkpoint;
                SearchBatch<SearchDocument> batch =
                        readOnlyTransaction.execute(status -> source.loadAfter(after, batchSize));
                if (batch == null || batch.lastId() <= after) {
                    break;
                }
                gateway.upsert(build, batch.documents());
                copied += batch.documents().size();
                checkpoint = batch.lastId();
                stateStore.checkpoint(index, checkpoint);
            }

            admin.swapIndexes(index, build);
            admin.deleteIndex(build);
            Long count = admin.stats(index).map(SearchIndexStats::numberOfDocuments).orElse(null);
            stateStore.markReady(index, definition.schemaVersion(), count);
            log.info("Rebuilt search index {}: {} document(s) copied this run, {} live", index, copied, count);
        } catch (RuntimeException ex) {
            log.error("Rebuild of search index {} failed; dropping {}", index, build, ex);
            try {
                admin.deleteIndex(build);
            } catch (RuntimeException cleanup) {
                log.warn("Could not drop build index {}: {}", build, cleanup.getMessage());
            }
            stateStore.markFailed(index, ex.getMessage());
            throw ex;
        }
    }

    private static boolean isResumable(SearchIndexState state) {
        return state.getStatus() == SearchIndexStatus.REBUILDING
                && state.getBuildIndexName() != null
                && state.getRebuildCheckpointId() != null;
    }

    private SearchDocumentSource<SearchDocument> requireSource(String index) {
        return registry.find(index)
                .orElseThrow(() -> new ResourceNotFoundException("No search index named " + index));
    }

    @Override
    public void destroy() {
        rebuildExecutor.shutdown();
    }
}
