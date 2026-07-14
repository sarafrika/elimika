package apps.sarafrika.elimika.shared.storage.service;

import apps.sarafrika.elimika.shared.storage.util.FileUrlResolver;

/**
 * Result of a successful upload through the storage facade.
 *
 * @param key              canonical bare storage key — this is what domain tables persist
 * @param originalFilename filename as uploaded by the client
 * @param sizeBytes        stored size
 * @param mimeType         detected content type
 */
public record StoredMedia(
        String key,
        String originalFilename,
        long sizeBytes,
        String mimeType
) {
    public String publicUrl() {
        return FileUrlResolver.publicUrl(key);
    }
}
