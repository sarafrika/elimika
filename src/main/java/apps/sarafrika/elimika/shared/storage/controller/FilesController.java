package apps.sarafrika.elimika.shared.storage.controller;

import apps.sarafrika.elimika.shared.dto.ApiResponse;
import apps.sarafrika.elimika.shared.storage.internal.MediaReconciliationService;
import apps.sarafrika.elimika.shared.storage.service.MediaServeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Unified file-serving endpoint. Every stored file is addressable as
 * {@code /api/v1/files/<storage key>} regardless of which module owns it;
 * legacy per-module media endpoints delegate to the same serving logic.
 */
@RestController
@RequestMapping(FilesController.API_ROOT_PATH)
@RequiredArgsConstructor
@Tag(name = "Files", description = "Unified access to stored files")
public class FilesController {

    public static final String API_ROOT_PATH = "/api/v1/files";

    private final MediaServeService mediaServeService;
    private final MediaReconciliationService mediaReconciliationService;

    @Operation(summary = "Reconcile stored files with database references",
            description = """
                    Cross-checks disk, the media_files registry and every domain file-reference column.
                    Fills missing registry metadata and flags registry rows whose file is gone.
                    With prune=true, domain references to lost files are cleared so API responses stop
                    returning URLs that would 404 (users can then re-upload).
                    """)
    @PostMapping("/admin/reconcile")
    public ResponseEntity<ApiResponse<MediaReconciliationService.ReconcileReport>> reconcile(
            @Parameter(description = "Clear domain references whose files no longer exist on disk")
            @RequestParam(defaultValue = "false") boolean prune) {
        MediaReconciliationService.ReconcileReport report = mediaReconciliationService.reconcile(prune);
        return ResponseEntity.ok(ApiResponse.success(report, "Media reconciliation completed"));
    }

    @Operation(summary = "Sweep for orphaned files",
            description = "Reports files on disk that no database reference or registry entry points to. "
                    + "With deleteOrphans=true, the orphaned files are removed from disk.")
    @PostMapping("/admin/sweep")
    public ResponseEntity<ApiResponse<MediaReconciliationService.SweepReport>> sweep(
            @Parameter(description = "Delete the orphaned files instead of only reporting them")
            @RequestParam(defaultValue = "false") boolean deleteOrphans) {
        MediaReconciliationService.SweepReport report = mediaReconciliationService.sweep(deleteOrphans);
        return ResponseEntity.ok(ApiResponse.success(report, "Media sweep completed"));
    }

    @Operation(summary = "Get a stored file by its storage key",
            description = "Serves any stored file (images, videos, documents, certificates) by its canonical storage key.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "File retrieved successfully")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "File not found")
    @GetMapping("/{*key}")
    public ResponseEntity<Resource> getFile(
            @Parameter(description = "Canonical storage key, e.g. course_thumbnails/uuid.jpg", required = true)
            @PathVariable String key) {
        return mediaServeService.serve(key);
    }
}
