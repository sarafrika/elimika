package apps.sarafrika.elimika.course.internal;

import apps.sarafrika.elimika.shared.storage.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service for validating lesson media files (documents, images, audio, video) before upload.
 *
 * Reuses the shared {@link StorageService} helpers to enforce allowed extensions and basic size limits.
 */
@Service
@RequiredArgsConstructor
public class LessonMediaValidationService {

    private final StorageService storageService;

    // Size limits in bytes – can be tuned per environment
    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024;    // 10MB
    private static final long MAX_DOCUMENT_SIZE = 50 * 1024 * 1024; // 50MB
    private static final long MAX_AUDIO_SIZE = 100 * 1024 * 1024;   // 100MB
    private static final long MAX_VIDEO_SIZE = 500 * 1024 * 1024;   // 500MB

    public void validateForLessonContent(MultipartFile file) {
        validateFileNotEmpty(file);

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("Uploaded file must have a name");
        }

        if (storageService.isImage(originalFilename)) {
            validateFileSize(file, MAX_IMAGE_SIZE, "Image");
            return;
        }

        if (storageService.isVideo(originalFilename)) {
            validateFileSize(file, MAX_VIDEO_SIZE, "Video");
            return;
        }

        if (storageService.isAudio(originalFilename)) {
            validateFileSize(file, MAX_AUDIO_SIZE, "Audio");
            return;
        }

        if (storageService.isDocument(originalFilename)) {
            validateFileSize(file, MAX_DOCUMENT_SIZE, "Document");
            return;
        }

        throw new IllegalArgumentException("Unsupported file type for lesson content: " + originalFilename);
    }

    public void validateImageForEditor(MultipartFile file) {
        validateFileNotEmpty(file);
        validateFileSize(file, MAX_IMAGE_SIZE, "Editor image");

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !storageService.isImage(originalFilename)) {
            throw new IllegalArgumentException(
                    "Editor uploads must be image files (JPG, PNG, GIF, WebP)"
            );
        }
    }

    private void validateFileNotEmpty(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }
    }

    private void validateFileSize(MultipartFile file, long maxSize, String label) {
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException(
                    String.format("%s file size cannot exceed %d MB", label, maxSize / (1024 * 1024))
            );
        }
    }
}

