package apps.sarafrika.elimika.shared.storage.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Single place that converts between canonical storage keys and public URLs.
 * <p>
 * Canonical persisted form is the bare storage key ({@code course_thumbnails/uuid.jpg}).
 * Public form is {@code /api/v1/files/<key>}. Genuinely external URLs (anything on a
 * non-Elimika host) pass through unchanged in both directions of serialization, and
 * legacy per-module API URLs still present in unmigrated data are tolerated.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FileUrlResolver {

    public static final String PUBLIC_PREFIX = "/api/v1/files/";

    private static final Pattern EXTERNAL_URL = Pattern.compile("^https?://", Pattern.CASE_INSENSITIVE);
    private static final Pattern SELF_HOST = Pattern.compile(
            "^https?://[^/]*\\bsarafrika\\.com(/.*)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern DOCUMENT_FILES = Pattern.compile(
            "^/api/v1/(?:instructors|course-creators)/[^/]+/documents/files/(.+)$");

    /**
     * Legacy per-module URL prefixes mapped to the folder that must be re-prefixed onto
     * the remaining path when the legacy URL carried only a bare filename.
     */
    private static final List<LegacyPrefix> LEGACY_PREFIXES = List.of(
            new LegacyPrefix("/api/v1/users/profile-image/", "profile_images"),
            new LegacyPrefix("/api/v1/courses/content-media/", null),
            new LegacyPrefix("/api/v1/courses/media/", null),
            new LegacyPrefix("/api/v1/classes/media/", null),
            new LegacyPrefix("/api/v1/assignments/submission-media/", null),
            new LegacyPrefix("/api/v1/assignments/media/", null),
            new LegacyPrefix("/api/v1/certificates/files/", "certificates"),
            new LegacyPrefix(PUBLIC_PREFIX, null)
    );

    private record LegacyPrefix(String prefix, String impliedFolder) {
    }

    /**
     * Resolves a persisted value to the URL exposed to clients.
     * Bare storage keys become {@code /api/v1/files/<encoded key>}; external URLs and
     * already-absolute API paths (legacy unmigrated data) pass through unchanged.
     */
    public static String publicUrl(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (isExternal(trimmed) || trimmed.startsWith("/api/v1/")) {
            return trimmed;
        }
        String key = StoragePathUtils.normalizeRelativePath(trimmed);
        return PUBLIC_PREFIX + UriUtils.encodePath(key, StandardCharsets.UTF_8);
    }

    /**
     * True when the value points at a host outside this application (a real external
     * link that must never be rewritten or served from local storage).
     */
    public static boolean isExternal(String value) {
        return value != null && EXTERNAL_URL.matcher(value.trim()).find()
                && !SELF_HOST.matcher(value.trim()).find();
    }

    /**
     * Reduces any historical representation to the canonical bare storage key:
     * absolute self-host URLs, legacy per-module API paths (decoding {@code %}-escapes)
     * and bare keys all normalize; genuinely external URLs and unrecognizable values
     * return {@code null}.
     */
    public static String toKey(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String v = value.trim();

        boolean wasSelfHost = false;
        Matcher selfHost = SELF_HOST.matcher(v);
        if (selfHost.find()) {
            v = selfHost.group(1);
            wasSelfHost = true;
        } else if (EXTERNAL_URL.matcher(v).find()) {
            return null;
        }

        if (!v.startsWith("/")) {
            return StoragePathUtils.normalizeRelativePath(v);
        }

        for (LegacyPrefix legacy : LEGACY_PREFIXES) {
            if (v.startsWith(legacy.prefix())) {
                String remainder = UriUtils.decode(v.substring(legacy.prefix().length()), StandardCharsets.UTF_8);
                remainder = StoragePathUtils.normalizeRelativePath(remainder);
                if (remainder == null || remainder.isBlank()) {
                    return null;
                }
                if (legacy.impliedFolder() != null && !remainder.startsWith(legacy.impliedFolder() + "/")) {
                    return legacy.impliedFolder() + "/" + remainder;
                }
                return remainder;
            }
        }

        Matcher docs = DOCUMENT_FILES.matcher(v);
        if (docs.matches()) {
            return StoragePathUtils.normalizeRelativePath(
                    UriUtils.decode(docs.group(1), StandardCharsets.UTF_8));
        }

        // Self-host URL without /api/v1 (e.g. https://host/assignments/... written by an
        // old release): the path itself is the key.
        if (wasSelfHost) {
            return StoragePathUtils.normalizeRelativePath(v);
        }
        return null;
    }

    /**
     * Converts a client-supplied value to what may be persisted on an entity:
     * genuinely external URLs pass through, anything else is reduced to the
     * canonical storage key (or {@code null} when unrecognizable). Use this on
     * every DTO→entity write path so resolved {@code /api/v1/files/...} URLs are
     * never written back to the database.
     */
    public static String toStorableValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        if (isExternal(trimmed)) {
            return trimmed;
        }
        return toKey(trimmed);
    }

    /**
     * Returns the last path segment of a key or URL, for use as a download filename.
     */
    public static String fileName(String value) {
        String key = toKey(value);
        if (key == null) {
            return null;
        }
        int lastSeparator = key.lastIndexOf('/');
        return lastSeparator >= 0 ? key.substring(lastSeparator + 1) : key;
    }
}
