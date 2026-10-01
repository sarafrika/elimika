package apps.sarafrika.elimika.search.internal.sync;

import apps.sarafrika.elimika.search.config.SearchConfiguration;
import apps.sarafrika.elimika.search.internal.state.SearchIndexState;
import apps.sarafrika.elimika.search.internal.state.SearchIndexStateStore;
import apps.sarafrika.elimika.search.internal.state.SearchIndexStatus;
import apps.sarafrika.elimika.shared.search.SearchDocument;
import apps.sarafrika.elimika.shared.search.SearchDocumentSource;
import apps.sarafrika.elimika.shared.search.SearchIndexAdmin;
import apps.sarafrika.elimika.shared.search.SearchIndexDefinition;
import apps.sarafrika.elimika.shared.search.SearchSynonymsChanged;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Re-applies index settings when a synonym source changed (for example a skill or its aliases), so
 * the engine picks up the new words without a rebuild: synonyms are applied at query time.
 * <p>
 * Every index whose definition names the source is refreshed, and so is its build index while a
 * blue/green rebuild is running, so the swap does not bring back the old words. Runs after commit on
 * the single indexing thread. A failure is rethrown so the Modulith publication stays incomplete and
 * is retried like any indexing request.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "search", name = "enabled", havingValue = "true")
public class SearchSynonymsRefresher {

    private final SearchSourceRegistry registry;
    private final SearchIndexAdmin admin;
    private final SearchIndexStateStore stateStore;
    private final SearchDefinitionResolver resolver;

    @Async(SearchConfiguration.INDEX_EXECUTOR)
    @TransactionalEventListener(fallbackExecution = true)
    public void on(SearchSynonymsChanged event) {
        List<String> refreshed = refresh(event.source());
        log.info("Synonym source '{}' changed; refreshed settings of {}", event.source(), refreshed);
    }

    /** Re-applies the settings of every index that takes synonyms from {@code source}. Returns them. */
    public List<String> refresh(String source) {
        List<String> refreshed = new ArrayList<>();
        for (SearchDocumentSource<SearchDocument> documentSource : registry.all()) {
            SearchIndexDefinition definition = documentSource.definition();
            if (!SearchDefinitionResolver.uses(definition, source)) {
                continue;
            }
            SearchIndexDefinition effective = resolver.effective(definition);
            admin.ensureIndex(definition.name(), effective);
            refreshed.add(definition.name());
            stateStore.find(definition.name())
                    .filter(state -> state.getStatus() == SearchIndexStatus.REBUILDING)
                    .map(SearchIndexState::getBuildIndexName)
                    .ifPresent(build -> admin.ensureIndex(build, effective));
        }
        return refreshed;
    }
}
