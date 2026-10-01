package apps.sarafrika.elimika.search.internal.sync;

import apps.sarafrika.elimika.search.config.SearchProperties;
import apps.sarafrika.elimika.search.internal.state.SearchIndexState;
import apps.sarafrika.elimika.search.internal.state.SearchIndexStateStore;
import apps.sarafrika.elimika.search.internal.state.SearchIndexStatus;
import apps.sarafrika.elimika.shared.search.SearchDocument;
import apps.sarafrika.elimika.shared.search.SearchDocumentSource;
import apps.sarafrika.elimika.shared.search.SearchIndexAdmin;
import apps.sarafrika.elimika.shared.search.SearchIndexDefinition;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Brings every index in line with its definition once the application is up:
 * <ul>
 *     <li>creates the index if it is missing and (re)applies its settings;</li>
 *     <li>marks it {@code STALE} when the definition's schema version is newer than the version it
 *     was built with, and rebuilds it in the background when {@code search.auto-rebuild} is on;</li>
 *     <li>resumes a rebuild that a restart interrupted.</li>
 * </ul>
 * Creating the index and applying its settings is retried with exponential backoff
 * ({@code search.startup-retry-attempts}, first pause {@code search.startup-retry-initial-backoff},
 * doubling): the engine can be slow to answer while another instance is rebuilding. Failed attempts
 * are logged at warn; only giving up is an error. Each index is prepared on its own, so one that
 * cannot be prepared never stops the others.
 * <p>
 * A failure here is logged, never fatal: the application must start even when the engine does not.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "search", name = "enabled", havingValue = "true")
public class SearchIndexStartupRunner {

    private final SearchSourceRegistry registry;
    private final SearchIndexAdmin admin;
    private final SearchIndexStateStore stateStore;
    private final SearchIndexRebuilder rebuilder;
    private final SearchProperties properties;
    private final SearchDefinitionResolver resolver;

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        for (SearchDocumentSource<SearchDocument> source : registry.all()) {
            SearchIndexDefinition definition = source.definition();
            try {
                prepare(definition);
            } catch (Exception ex) {
                log.error("Could not prepare search index {}: {}", definition.name(), ex.getMessage(), ex);
            }
        }
    }

    private void prepare(SearchIndexDefinition definition) {
        String index = definition.name();
        ensureWithRetry(index, resolver.effective(definition));
        SearchIndexState state = stateStore.getOrCreate(index);

        if (state.getStatus() == SearchIndexStatus.REBUILDING) {
            log.info("Search index {} was mid-rebuild at shutdown; resuming", index);
            rebuilder.rebuildAsync(index);
            return;
        }
        if (definition.schemaVersion() > state.getSchemaVersion()) {
            stateStore.markStale(index);
            if (properties.isAutoRebuild()) {
                log.info("Search index {} is at schema version {} but the definition is at {}; rebuilding",
                        index, state.getSchemaVersion(), definition.schemaVersion());
                rebuilder.rebuildAsync(index);
            } else {
                log.warn("Search index {} is stale (schema version {} < {}); rebuild it through "
                                + "POST /api/v1/admin/search/indexes/{}/rebuild or set search.auto-rebuild=true",
                        index, state.getSchemaVersion(), definition.schemaVersion(), index);
            }
        }
    }

    /** Creates the index and applies its settings, retrying with exponential backoff; throws the last failure. */
    void ensureWithRetry(String index, SearchIndexDefinition definition) {
        int attempts = Math.max(1, properties.getStartupRetryAttempts());
        Duration pause = properties.getStartupRetryInitialBackoff() == null
                ? Duration.ofSeconds(1) : properties.getStartupRetryInitialBackoff();
        for (int attempt = 1; ; attempt++) {
            try {
                admin.ensureIndex(index, definition);
                return;
            } catch (RuntimeException ex) {
                if (attempt >= attempts) {
                    throw ex;
                }
                log.warn("Preparing search index {} failed (attempt {}/{}): {}; retrying in {}",
                        index, attempt, attempts, ex.getMessage(), pause);
                sleep(pause);
                pause = pause.multipliedBy(2);
            }
        }
    }

    private static void sleep(Duration pause) {
        try {
            Thread.sleep(pause.toMillis());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting to retry a search index", ex);
        }
    }
}
