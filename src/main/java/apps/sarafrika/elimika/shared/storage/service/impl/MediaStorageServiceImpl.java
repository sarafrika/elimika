package apps.sarafrika.elimika.shared.storage.service.impl;

import apps.sarafrika.elimika.shared.storage.model.MediaFile;
import apps.sarafrika.elimika.shared.storage.repository.MediaFileRepository;
import apps.sarafrika.elimika.shared.storage.service.*;
import apps.sarafrika.elimika.shared.storage.util.FileUrlResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaStorageServiceImpl implements MediaStorageService {

    private final StorageService storageService;
    private final MediaValidationService mediaValidationService;
    private final MediaFileRepository mediaFileRepository;

    @Override
    @Transactional
    public StoredMedia store(MediaUploadRequest request) {
        mediaValidationService.validate(request.file(), request.category());

        String key = storageService.store(request.file(), request.folder());
        String mimeType = storageService.getContentType(key);

        MediaFile mediaFile = mediaFileRepository.findByFileKey(key).orElseGet(MediaFile::new);
        mediaFile.setFileKey(key);
        mediaFile.setOriginalFilename(request.file().getOriginalFilename());
        mediaFile.setSizeBytes(request.file().getSize());
        mediaFile.setMimeType(mimeType);
        mediaFile.setOwnerType(request.ownerType());
        mediaFile.setOwnerUuid(request.ownerUuid());
        mediaFile.setFileExists(true);
        mediaFileRepository.save(mediaFile);

        // New file is safely on disk before the old one goes; worst failure mode is a
        // temporary orphan, which the reconciliation sweep picks up.
        if (request.previousValue() != null && !request.previousValue().equals(key)) {
            delete(request.previousValue());
        }

        return new StoredMedia(key, request.file().getOriginalFilename(), request.file().getSize(), mimeType);
    }

    @Override
    @Transactional
    public void delete(String keyOrLegacyUrl) {
        String key = FileUrlResolver.toKey(keyOrLegacyUrl);
        if (key == null) {
            return;
        }
        try {
            storageService.delete(key);
        } catch (Exception e) {
            log.warn("Failed to delete stored file '{}': {}", key, e.getMessage());
        }
        mediaFileRepository.findByFileKey(key).ifPresent(mediaFileRepository::delete);
    }

    @Override
    @Transactional
    public void deleteAllForOwner(String ownerType, UUID ownerUuid) {
        for (MediaFile mediaFile : mediaFileRepository.findByOwnerTypeAndOwnerUuid(ownerType, ownerUuid)) {
            try {
                storageService.delete(mediaFile.getFileKey());
            } catch (Exception e) {
                log.warn("Failed to delete stored file '{}': {}", mediaFile.getFileKey(), e.getMessage());
            }
            mediaFileRepository.delete(mediaFile);
        }
    }
}
