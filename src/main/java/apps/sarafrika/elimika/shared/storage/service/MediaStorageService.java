package apps.sarafrika.elimika.shared.storage.service;

import java.util.UUID;

/**
 * High-level file management facade all modules use for uploads and lifecycle:
 * validate → store → register in the {@code media_files} registry → clean up the
 * replaced file. Domain tables persist the returned bare storage key; public URLs
 * are produced at serialization time via
 * {@link apps.sarafrika.elimika.shared.storage.util.FileUrlResolver}.
 */
public interface MediaStorageService {

    /**
     * Validates, stores and registers an upload, then removes the previous file
     * referenced by {@link MediaUploadRequest#previousValue()} (best effort).
     */
    StoredMedia store(MediaUploadRequest request);

    /**
     * Deletes the file and registry entry for a stored reference. Accepts the
     * canonical key or any legacy URL form; external URLs and nulls are no-ops.
     * Disk failures are logged, never thrown.
     */
    void delete(String keyOrLegacyUrl);

    /**
     * Deletes every registered file belonging to an owner (entity-delete hook).
     */
    void deleteAllForOwner(String ownerType, UUID ownerUuid);
}
