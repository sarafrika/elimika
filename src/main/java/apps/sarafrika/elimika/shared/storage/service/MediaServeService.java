package apps.sarafrika.elimika.shared.storage.service;

import apps.sarafrika.elimika.shared.storage.util.StoragePathUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Shared implementation behind every file-serving endpoint. The unified
 * {@code GET /api/v1/files/{*key}} endpoint and all legacy per-module media
 * endpoints delegate here so headers and error handling stay consistent.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MediaServeService {

    public static final Set<Integer> VARIANT_WIDTHS = Set.of(320, 640, 1280);
    private static final Set<String> RESIZABLE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "bmp");
    private static final String PRIVATE_PREFIX = "profile_documents/";
    private static final Duration ONE_YEAR = Duration.ofDays(365);

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
        String key = resolveKey(rawKey, fallbackKey);
        return key == null ? ResponseEntity.notFound().build() : respond(key, key);
    }

    /**
     * Serves a key, optionally as a downscaled variant {@code width} pixels wide.
     * Variants are generated on first request and cached under {@code variants/w<width>/}.
     */
    public ResponseEntity<Resource> serveSized(String rawKey, Integer width) {
        if (width == null) {
            return serve(rawKey);
        }
        if (!VARIANT_WIDTHS.contains(width)) {
            return ResponseEntity.badRequest().build();
        }
        String key = resolveKey(rawKey, null);
        if (key == null || !storageService.exists(key)) {
            return ResponseEntity.notFound().build();
        }
        if (isPrivate(key) || key.startsWith(StoragePathUtils.VARIANTS_FOLDER + "/")
                || !RESIZABLE_EXTENSIONS.contains(storageService.getFileExtension(key))) {
            return respond(key, key);
        }
        String variantKey = findOrCreateVariant(key, width);
        return respond(variantKey == null ? key : variantKey, key);
    }

    private String resolveKey(String rawKey, String fallbackKey) {
        String key = StoragePathUtils.normalizeRelativePath(rawKey);
        if (key == null || key.isEmpty()) {
            return null;
        }
        if (!storageService.exists(key) && fallbackKey != null) {
            String fallback = StoragePathUtils.normalizeRelativePath(fallbackKey);
            if (fallback != null && storageService.exists(fallback)) {
                key = fallback;
            }
        }
        return key;
    }

    private String findOrCreateVariant(String key, int width) {
        String base = StoragePathUtils.VARIANTS_FOLDER + "/w" + width + "/" + key;
        for (String candidate : List.of(base + ".jpg", base + ".png", base)) {
            if (storageService.exists(candidate)) {
                return candidate;
            }
        }
        try (InputStream in = storageService.load(key).getInputStream()) {
            byte[] original = in.readAllBytes();
            Optional<ImageVariantGenerator.Variant> variant = ImageVariantGenerator.resize(original, width);
            // Small or undecodable originals are cached as-is so they are never re-probed.
            String variantKey = variant.map(v -> base + "." + v.extension()).orElse(base);
            storageService.write(variantKey, variant.map(ImageVariantGenerator.Variant::bytes).orElse(original));
            return variantKey;
        } catch (Exception e) {
            log.warn("Could not create {}px variant of '{}': {}", width, key, e.getMessage());
            return null;
        }
    }

    private ResponseEntity<Resource> respond(String servedKey, String requestedKey) {
        try {
            Resource resource = storageService.load(servedKey);
            String contentType = storageService.getContentType(servedKey);
            String fileName = requestedKey.substring(requestedKey.lastIndexOf('/') + 1);
            CacheControl cacheControl = isPrivate(requestedKey)
                    ? CacheControl.maxAge(ONE_YEAR).cachePrivate().immutable()
                    : CacheControl.maxAge(ONE_YEAR).cachePublic().immutable();

            ResponseEntity.BodyBuilder response = ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .cacheControl(cacheControl)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"");
            if (resource.isFile()) {
                long lastModified = resource.lastModified();
                response.eTag(etag(servedKey, resource.contentLength(), lastModified)).lastModified(lastModified);
            }
            return response.body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    private static String etag(String key, long length, long lastModified) {
        String seed = key + ':' + length + ':' + lastModified;
        return '"' + DigestUtils.md5DigestAsHex(seed.getBytes(StandardCharsets.UTF_8)) + '"';
    }

    /** Credential documents are authenticated (see SecurityConfiguration), so never shared-cacheable. */
    private static boolean isPrivate(String key) {
        return key.startsWith(PRIVATE_PREFIX);
    }
}
