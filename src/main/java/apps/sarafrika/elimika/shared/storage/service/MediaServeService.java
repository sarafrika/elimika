package apps.sarafrika.elimika.shared.storage.service;

import apps.sarafrika.elimika.shared.storage.config.StorageProperties;
import apps.sarafrika.elimika.shared.storage.util.StoragePathUtils;
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
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;

/**
 * Shared implementation behind every file-serving endpoint. The unified
 * {@code GET /api/v1/files/{*key}} endpoint and all legacy per-module media
 * endpoints delegate here so headers and error handling stay consistent.
 */
@Slf4j
@Service
public class MediaServeService {

    public static final Set<Integer> VARIANT_WIDTHS = StoragePathUtils.VARIANT_WIDTHS;
    private static final Set<String> RESIZABLE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "bmp");
    private static final Duration ONE_YEAR = Duration.ofDays(365);
    /** Caps concurrent full-image decodes so a cold catalogue page cannot exhaust heap. */
    private static final int MAX_CONCURRENT_RESIZES = 2;

    private final StorageService storageService;
    private final String privatePrefix;
    private final ConcurrentMap<String, CompletableFuture<String>> inflightVariants = new ConcurrentHashMap<>();
    private final Semaphore resizePermits = new Semaphore(MAX_CONCURRENT_RESIZES);

    public MediaServeService(StorageService storageService, StorageProperties storageProperties) {
        this.storageService = storageService;
        this.privatePrefix = StoragePathUtils.normalizeRelativePath(
                storageProperties.getFolders().getProfileDocuments()) + "/";
    }

    /**
     * Serves the file for a bare storage key as a private (browser-only) cacheable
     * response; legacy endpoints may sit behind authentication.
     */
    public ResponseEntity<Resource> serve(String rawKey) {
        return serve(rawKey, null);
    }

    /**
     * Serves a key privately, falling back to an alternative key when the primary is
     * absent (legacy endpoints whose historical URLs had inconsistent nesting).
     */
    public ResponseEntity<Resource> serve(String rawKey, String fallbackKey) {
        return serve(rawKey, fallbackKey, false);
    }

    /** Serves a key; {@code shareable} lets shared caches (CDN, proxies) store the response. */
    public ResponseEntity<Resource> serve(String rawKey, String fallbackKey, boolean shareable) {
        String key = resolveKey(rawKey, fallbackKey);
        return key == null ? ResponseEntity.notFound().build() : respond(key, key, shareable);
    }

    /**
     * Serves a key, optionally as a downscaled variant {@code width} pixels wide.
     * Variants are generated on first request and cached under {@code variants/w<width>/}.
     */
    public ResponseEntity<Resource> serveSized(String rawKey, Integer width) {
        if (width != null && !VARIANT_WIDTHS.contains(width)) {
            return ResponseEntity.badRequest().build();
        }
        String key = resolveKey(rawKey, null);
        if (key == null || !storageService.exists(key)) {
            return ResponseEntity.notFound().build();
        }
        boolean shareable = !key.startsWith(privatePrefix);
        if (width == null || !shareable || key.startsWith(StoragePathUtils.VARIANTS_FOLDER + "/")
                || !RESIZABLE_EXTENSIONS.contains(storageService.getFileExtension(key))) {
            return respond(key, key, shareable);
        }
        String variantKey = findOrCreateVariant(key, width);
        return respond(variantKey == null ? key : variantKey, key, true);
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
        String existing = findVariant(key, width);
        if (existing != null) {
            return existing;
        }
        // Concurrent first requests for the same variant share one generation.
        String inflightKey = width + ":" + key;
        CompletableFuture<String> mine = new CompletableFuture<>();
        CompletableFuture<String> running = inflightVariants.putIfAbsent(inflightKey, mine);
        if (running != null) {
            return running.join();
        }
        try {
            String created = createVariant(key, width);
            mine.complete(created);
            return created;
        } finally {
            mine.complete(null);
            inflightVariants.remove(inflightKey, mine);
        }
    }

    private String findVariant(String key, int width) {
        for (String candidate : StoragePathUtils.variantKeys(key, width)) {
            if (storageService.exists(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private String createVariant(String key, int width) {
        String base = StoragePathUtils.variantBase(key, width);
        boolean permitted = false;
        try (InputStream in = storageService.load(key).getInputStream()) {
            resizePermits.acquire();
            permitted = true;
            String raced = findVariant(key, width);
            if (raced != null) {
                return raced;
            }
            byte[] original = in.readAllBytes();
            Optional<ImageVariantGenerator.Variant> variant = ImageVariantGenerator.resize(original, width);
            // Small or undecodable originals are cached as-is so they are never re-probed.
            String variantKey = variant.map(v -> base + "." + v.extension()).orElse(base);
            storageService.write(variantKey, variant.map(ImageVariantGenerator.Variant::bytes).orElse(original));
            return variantKey;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception e) {
            log.warn("Could not create {}px variant of '{}': {}", width, key, e.getMessage());
            return null;
        } finally {
            if (permitted) {
                resizePermits.release();
            }
        }
    }

    private ResponseEntity<Resource> respond(String servedKey, String requestedKey, boolean shareable) {
        try {
            Resource resource = storageService.load(servedKey);
            String contentType = storageService.getContentType(servedKey);
            String fileName = requestedKey.substring(requestedKey.lastIndexOf('/') + 1);
            CacheControl cacheControl = shareable
                    ? CacheControl.maxAge(ONE_YEAR).cachePublic().immutable()
                    : CacheControl.maxAge(ONE_YEAR).cachePrivate().immutable();

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
}
