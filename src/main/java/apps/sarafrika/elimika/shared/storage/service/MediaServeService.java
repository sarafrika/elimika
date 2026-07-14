package apps.sarafrika.elimika.shared.storage.service;

import apps.sarafrika.elimika.shared.storage.util.StoragePathUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/**
 * Shared implementation behind every file-serving endpoint. The unified
 * {@code GET /api/v1/files/{*key}} endpoint and all legacy per-module media
 * endpoints delegate here so headers and error handling stay consistent.
 */
@Service
@RequiredArgsConstructor
public class MediaServeService {

    private final StorageService storageService;

    /**
     * Serves the file for a bare storage key. Stored files are immutable
     * (UUID-named), so long-lived caching is safe.
     */
    public ResponseEntity<Resource> serve(String rawKey) {
        return serve(rawKey, null);
    }

    /**
     * Serves a key, falling back to alternative keys when the primary is absent
     * (used by legacy endpoints whose historical URLs had inconsistent nesting).
     */
    public ResponseEntity<Resource> serve(String rawKey, String fallbackKey) {
        String key = StoragePathUtils.normalizeRelativePath(rawKey);
        if (key == null || key.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (!storageService.exists(key) && fallbackKey != null) {
            String fallback = StoragePathUtils.normalizeRelativePath(fallbackKey);
            if (fallback != null && storageService.exists(fallback)) {
                key = fallback;
            }
        }
        try {
            Resource resource = storageService.load(key);
            String contentType = storageService.getContentType(key);
            String fileName = key.substring(key.lastIndexOf('/') + 1);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=31536000, immutable")
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}
