package apps.sarafrika.elimika.search.internal.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import apps.sarafrika.elimika.search.internal.state.SearchIndexState;
import apps.sarafrika.elimika.search.internal.state.SearchIndexStateStore;
import apps.sarafrika.elimika.search.internal.state.SearchIndexStatus;
import apps.sarafrika.elimika.shared.search.SearchDocument;
import apps.sarafrika.elimika.shared.search.SearchDocumentSource;
import apps.sarafrika.elimika.shared.search.SearchIndexDefinition;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Nightly full search rebuild")
class SearchFullRebuildSchedulerTest {

    private final SearchSourceRegistry registry = mock(SearchSourceRegistry.class);
    private final SearchIndexStateStore stateStore = mock(SearchIndexStateStore.class);
    private final SearchIndexRebuilder rebuilder = mock(SearchIndexRebuilder.class);
    private final SearchFullRebuildScheduler scheduler = new SearchFullRebuildScheduler(registry, stateStore, rebuilder);

    @Test
    @DisplayName("Queues every index except one that is already rebuilding")
    void skipsIndexAlreadyRebuilding() {
        List<SearchDocumentSource<SearchDocument>> sources = sources("courses", "classes", "people");
        when(registry.all()).thenReturn(sources);
        when(stateStore.find("courses")).thenReturn(Optional.of(state("courses", SearchIndexStatus.READY)));
        when(stateStore.find("classes")).thenReturn(Optional.of(state("classes", SearchIndexStatus.REBUILDING)));
        when(stateStore.find("people")).thenReturn(Optional.empty());
        when(rebuilder.rebuildAsync("courses")).thenReturn(true);
        when(rebuilder.rebuildAsync("people")).thenReturn(true);

        assertThat(scheduler.queueRebuilds()).containsExactly("courses", "people");
        verify(rebuilder, never()).rebuildAsync("classes");
    }

    @Test
    @DisplayName("Rebuilds a failed or stale index, and leaves out one the rebuilder already has queued")
    void rebuildsFailedAndStaleSkipsQueued() {
        List<SearchDocumentSource<SearchDocument>> sources = sources("rubrics", "programs", "instructors");
        when(registry.all()).thenReturn(sources);
        when(stateStore.find("rubrics")).thenReturn(Optional.of(state("rubrics", SearchIndexStatus.FAILED)));
        when(stateStore.find("programs")).thenReturn(Optional.of(state("programs", SearchIndexStatus.STALE)));
        when(stateStore.find("instructors")).thenReturn(Optional.of(state("instructors", SearchIndexStatus.READY)));
        when(rebuilder.rebuildAsync("rubrics")).thenReturn(true);
        when(rebuilder.rebuildAsync("programs")).thenReturn(true);
        when(rebuilder.rebuildAsync("instructors")).thenReturn(false);

        assertThat(scheduler.queueRebuilds()).containsExactly("rubrics", "programs");
    }

    @Test
    @DisplayName("One index that cannot be queued does not stop the others")
    void continuesPastAFailure() {
        List<SearchDocumentSource<SearchDocument>> sources = sources("courses", "organisations");
        when(registry.all()).thenReturn(sources);
        when(stateStore.find("courses")).thenThrow(new IllegalStateException("database down"));
        when(stateStore.find("organisations")).thenReturn(Optional.empty());
        when(rebuilder.rebuildAsync("organisations")).thenReturn(true);

        assertThat(scheduler.queueRebuilds()).containsExactly("organisations");
    }

    private static List<SearchDocumentSource<SearchDocument>> sources(String... indexes) {
        return java.util.Arrays.stream(indexes).map(SearchFullRebuildSchedulerTest::source).toList();
    }

    @SuppressWarnings("unchecked")
    private static SearchDocumentSource<SearchDocument> source(String index) {
        SearchDocumentSource<SearchDocument> source = mock(SearchDocumentSource.class);
        when(source.definition()).thenReturn(SearchIndexDefinition.of(index, 1, List.of(), List.of(), List.of()));
        return source;
    }

    private static SearchIndexState state(String index, SearchIndexStatus status) {
        SearchIndexState state = SearchIndexState.initial(index);
        state.setStatus(status);
        return state;
    }
}
