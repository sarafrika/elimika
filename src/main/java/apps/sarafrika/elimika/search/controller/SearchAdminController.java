package apps.sarafrika.elimika.search.controller;

import apps.sarafrika.elimika.search.config.SearchProperties;
import apps.sarafrika.elimika.search.dto.SearchIndexStatusResponse;
import apps.sarafrika.elimika.search.dto.SearchRebuildResponse;
import apps.sarafrika.elimika.search.internal.state.SearchIndexState;
import apps.sarafrika.elimika.search.internal.state.SearchIndexStateStore;
import apps.sarafrika.elimika.search.internal.sync.SearchIndexRebuilder;
import apps.sarafrika.elimika.search.internal.sync.SearchSourceRegistry;
import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.search.SearchDocument;
import apps.sarafrika.elimika.shared.search.SearchDocumentSource;
import apps.sarafrika.elimika.shared.search.SearchIndexAdmin;
import apps.sarafrika.elimika.shared.search.SearchIndexAdmin.SearchIndexStats;
import apps.sarafrika.elimika.shared.search.SearchIndexRequests;
import apps.sarafrika.elimika.shared.search.SearchUnavailableException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Operates the search indexes: inspect their state, rebuild them, and force one document back in
 * sync. Platform administrators only - a rebuild reads every row of a table.
 * <p>
 * Only registered when search is enabled; with search off these routes do not exist.
 * <p>
 * Rebuilds and syncs run in the background, so the write endpoints answer 202 Accepted.
 */
@RestController
@RequestMapping("/api/v1/admin/search")
@RequiredArgsConstructor
@Tag(name = "Search Administration", description = "Search index state, rebuilds and manual syncs")
@PreAuthorize("@domainSecurityService.isPlatformAdmin()")
@ConditionalOnProperty(prefix = "search", name = "enabled", havingValue = "true")
public class SearchAdminController {

    private final SearchSourceRegistry registry;
    private final SearchIndexStateStore stateStore;
    private final SearchIndexAdmin admin;
    private final SearchIndexRebuilder rebuilder;
    private final SearchIndexRequests indexRequests;
    private final SearchProperties properties;

    @GetMapping("/indexes")
    @Operation(summary = "List search indexes", description = "Definition, recorded sync state and live engine stats of every index")
    public ResponseEntity<ApiResponse<List<SearchIndexStatusResponse>>> listIndexes() {
        List<SearchIndexStatusResponse> indexes = registry.all().stream().map(this::describe).toList();
        return ResponseEntity.ok(ApiResponse.success(indexes, "Search indexes retrieved successfully"));
    }

    @PostMapping("/indexes/{index}/rebuild")
    @Operation(summary = "Rebuild one index", description = "Blue/green rebuild from the source tables, in the background")
    public ResponseEntity<ApiResponse<SearchRebuildResponse>> rebuildIndex(@PathVariable String index) {
        requireSource(index);
        boolean queued = rebuilder.rebuildAsync(index);
        SearchRebuildResponse response = queued
                ? new SearchRebuildResponse(List.of(index), List.of())
                : new SearchRebuildResponse(List.of(), List.of(index));
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(response, queued ? "Rebuild queued" : "Rebuild already queued or running"));
    }

    @PostMapping("/rebuild")
    @Operation(summary = "Rebuild many indexes", description = "Rebuilds every index, or only those owned by the given module")
    public ResponseEntity<ApiResponse<SearchRebuildResponse>> rebuild(
            @RequestParam(value = "module", required = false) String module
    ) {
        List<SearchDocumentSource<SearchDocument>> sources = module == null || module.isBlank()
                ? List.copyOf(registry.all())
                : registry.ofModule(module);
        List<String> queued = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        for (SearchDocumentSource<SearchDocument> source : sources) {
            String index = source.definition().name();
            (rebuilder.rebuildAsync(index) ? queued : skipped).add(index);
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(new SearchRebuildResponse(queued, skipped), "Rebuilds queued"));
    }

    @PostMapping("/indexes/{index}/documents/{uuid}/sync")
    @Operation(summary = "Sync one document", description = "Reloads one document from its source and writes or deletes it in the index")
    public ResponseEntity<ApiResponse<SearchRebuildResponse>> syncDocument(
            @PathVariable String index,
            @PathVariable UUID uuid
    ) {
        requireSource(index);
        indexRequests.enqueue(index, uuid);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(new SearchRebuildResponse(List.of(uuid.toString()), List.of()), "Sync queued"));
    }

    private SearchIndexStatusResponse describe(SearchDocumentSource<SearchDocument> source) {
        String index = source.definition().name();
        Optional<SearchIndexState> state = stateStore.find(index);
        Long engineCount = null;
        Boolean engineIndexing = null;
        String engineError = null;
        try {
            Optional<SearchIndexStats> stats = admin.stats(index);
            if (stats.isPresent()) {
                engineCount = stats.get().numberOfDocuments();
                engineIndexing = stats.get().indexing();
            } else {
                engineError = "Index does not exist in the engine";
            }
        } catch (SearchUnavailableException ex) {
            engineError = ex.getMessage();
        }
        return new SearchIndexStatusResponse(
                index,
                SearchSourceRegistry.moduleOf(source),
                source.definition().schemaVersion(),
                state.map(SearchIndexState::getSchemaVersion).orElse(null),
                state.map(s -> s.getStatus().name()).orElse(null),
                properties.isReadEnabled(index),
                state.map(SearchIndexState::getBuildIndexName).orElse(null),
                state.map(SearchIndexState::getRebuildCheckpointId).orElse(null),
                state.map(SearchIndexState::getLastBuiltAt).orElse(null),
                state.map(SearchIndexState::getLastReconciledAt).orElse(null),
                state.map(SearchIndexState::getDocumentCount).orElse(null),
                state.map(SearchIndexState::getDrift).orElse(null),
                state.map(SearchIndexState::getLastError).orElse(null),
                engineCount,
                engineIndexing,
                engineError);
    }

    private void requireSource(String index) {
        if (registry.find(index).isEmpty()) {
            throw new ResourceNotFoundException("No search index named " + index);
        }
    }
}
