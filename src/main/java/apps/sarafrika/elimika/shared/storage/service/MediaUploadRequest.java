package apps.sarafrika.elimika.shared.storage.service;

import apps.sarafrika.elimika.shared.storage.util.MediaCategory;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Everything the storage facade needs to validate, store and register one upload.
 *
 * @param file          the uploaded file
 * @param category      validation rules to apply
 * @param folder        storage folder (may contain subfolders, e.g. {@code assignments/uuid/attachments})
 * @param ownerType     registry owner category, e.g. {@code COURSE_THUMBNAIL}
 * @param ownerUuid     uuid of the owning domain row, when known
 * @param previousValue the reference currently persisted on the owner (key or legacy URL);
 *                      its file and registry entry are removed after the new file is stored
 */
public record MediaUploadRequest(
        MultipartFile file,
        MediaCategory category,
        String folder,
        String ownerType,
        UUID ownerUuid,
        String previousValue
) {
    public MediaUploadRequest {
        if (folder == null || folder.isBlank()) {
            throw new IllegalArgumentException("Storage folder is required");
        }
        if (category == null) {
            throw new IllegalArgumentException("Media category is required");
        }
        if (ownerType == null || ownerType.isBlank()) {
            throw new IllegalArgumentException("Owner type is required");
        }
    }
}
