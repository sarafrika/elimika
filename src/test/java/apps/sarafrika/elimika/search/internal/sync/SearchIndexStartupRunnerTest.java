package apps.sarafrika.elimika.search.internal.sync;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import apps.sarafrika.elimika.search.config.SearchProperties;
import apps.sarafrika.elimika.search.internal.state.SearchIndexState;
import apps.sarafrika.elimika.search.internal.state.SearchIndexStateStore;
import apps.sarafrika.elimika.shared.search.SearchDocument;
import apps.sarafrika.elimika.shared.search.SearchDocumentSource;
import apps.sarafrika.elimika.shared.search.SearchIndexAdmin;
import apps.sarafrika.elimika.shared.search.SearchIndexDefinition;
import apps.sarafrika.elimika.shared.search.SearchSynonymSource;
import apps.sarafrika.elimika.shared.search.SearchUnavailableException;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

@DisplayName("Search startup: ensure-index retries")
class SearchIndexStartupRunnerTest {

    private final SearchSourceRegistry registry = mock(SearchSourceRegistry.class);
    private final SearchIndexAdmin admin = mock(SearchIndexAdmin.class);
    private final SearchIndexStateStore stateStore = mock(SearchIndexStateStore.class);
    private final SearchIndexRebuilder rebuilder = mock(SearchIndexRebuilder.class);
    private final SearchProperties properties = new SearchProperties();
    private SearchIndexStartupRunner runner;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        properties.setStartupRetryAttempts(3);
        properties.setStartupRetryInitialBackoff(Duration.ofMillis(1));
        ObjectProvider<SearchSynonymSource> none = mock(ObjectProvider.class);
        when(none.orderedStream()).thenAnswer(invocation -> java.util.stream.Stream.empty());
        runner = new SearchIndexStartupRunner(registry, admin, stateStore, rebuilder, properties,
                new SearchDefinitionResolver(none));
        when(stateStore.getOrCreate(any())).thenAnswer(invocation -> {
            SearchIndexState state = SearchIndexState.initial(invocation.getArgument(0));
            state.setSchemaVersion(1);
            return state;
        });
    }

    @Test
    @DisplayName("A timeout is retried until it succeeds")
    void retriesUntilItSucceeds() {
        List<SearchDocumentSource<SearchDocument>> sources = List.of(source("courses"));
        when(registry.all()).thenReturn(sources);
        doThrow(new SearchUnavailableException("timed out"))
                .doThrow(new SearchUnavailableException("timed out"))
                .doNothing()
                .when(admin).ensureIndex(eq("courses"), any());

        runner.onReady();

        verify(admin, times(3)).ensureIndex(eq("courses"), any());
        verify(stateStore).getOrCreate("courses");
    }

    @Test
    @DisplayName("An index that keeps failing gives up after the attempts and does not stop the others")
    void oneFailingIndexDoesNotStopTheOthers() {
        List<SearchDocumentSource<SearchDocument>> sources = List.of(source("classes"), source("people"));
        when(registry.all()).thenReturn(sources);
        doThrow(new SearchUnavailableException("timed out")).when(admin).ensureIndex(eq("classes"), any());
        doNothing().when(admin).ensureIndex(eq("people"), any());

        runner.onReady();

        verify(admin, times(3)).ensureIndex(eq("classes"), any());
        verify(stateStore, never()).getOrCreate("classes");
        verify(admin, times(1)).ensureIndex(eq("people"), any());
        verify(stateStore).getOrCreate("people");
    }

    @SuppressWarnings("unchecked")
    private static SearchDocumentSource<SearchDocument> source(String index) {
        SearchDocumentSource<SearchDocument> source = mock(SearchDocumentSource.class);
        when(source.definition()).thenReturn(SearchIndexDefinition.of(index, 1, List.of(), List.of(), List.of()));
        return source;
    }
}
