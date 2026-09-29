package apps.sarafrika.elimika.search.internal.sync;

import apps.sarafrika.elimika.search.internal.state.SearchIndexStateStore;
import apps.sarafrika.elimika.search.internal.state.SearchIndexStatus;
import apps.sarafrika.elimika.shared.search.SearchDocument;
import apps.sarafrika.elimika.shared.search.SearchDocumentSource;
import apps.sarafrika.elimika.shared.search.SearchIndexAdmin;
import apps.sarafrika.elimika.shared.search.SearchIndexAdmin.SearchIndexStats;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Compares each index's document count with what its source says it should hold, and records the
 * difference as drift. Drift means a sync was lost somewhere; it is logged loudly and repaired by a
 * rebuild, which this job deliberately does not start on its own.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "search", name = "enabled", havingValue = "true")
public class SearchIndexReconciler {

    private final SearchSourceRegistry registry;
    private final SearchIndexAdmin admin;
    private final SearchIndexStateStore stateStore;
    private final TransactionTemplate readOnlyTransaction;

    public SearchIndexReconciler(
            SearchSourceRegistry registry,
            SearchIndexAdmin admin,
            SearchIndexStateStore stateStore,
            PlatformTransactionManager transactionManager
    ) {
        this.registry = registry;
        this.admin = admin;
        this.stateStore = stateStore;
        this.readOnlyTransaction = new TransactionTemplate(transactionManager);
        this.readOnlyTransaction.setReadOnly(true);
    }

    @Scheduled(
            initialDelayString = "${search.reconcile-initial-delay:PT10M}",
            fixedDelayString = "${search.reconcile-interval:PT1H}")
    public void reconcile() {
        for (SearchDocumentSource<SearchDocument> source : registry.all()) {
            String index = source.definition().name();
            try {
                reconcile(source);
            } catch (Exception ex) {
                log.error("Reconciliation of search index {} failed: {}", index, ex.getMessage(), ex);
            }
        }
    }

    void reconcile(SearchDocumentSource<SearchDocument> source) {
        String index = source.definition().name();
        boolean rebuilding = stateStore.find(index)
                .map(state -> state.getStatus() == SearchIndexStatus.REBUILDING)
                .orElse(false);
        if (rebuilding) {
            return;
        }
        Optional<SearchIndexStats> stats = admin.stats(index);
        if (stats.isEmpty()) {
            log.warn("Search index {} does not exist in the engine", index);
            return;
        }
        long documents = stats.get().numberOfDocuments();
        Long expected = readOnlyTransaction.execute(status -> source.countIndexable());
        Long drift = expected == null || expected < 0 ? null : documents - expected;
        stateStore.recordReconciliation(index, documents, drift);
        if (drift != null && drift != 0) {
            log.warn("Search index {} has drifted: {} document(s) indexed, {} expected (drift {}). "
                    + "Rebuild it through POST /api/v1/admin/search/indexes/{}/rebuild", index, documents, expected, drift, index);
        }
    }
}
