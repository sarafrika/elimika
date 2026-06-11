package apps.sarafrika.elimika.classes.internal;

import apps.sarafrika.elimika.shared.storage.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ClassMediaValidationService {

    private static final long MAX_THUMBNAIL_SIZE = 5 * 1024 * 1024;
    private static final long MAX_PROMOTIONAL_VIDEO_SIZE = 100 * 1024 * 1024;

    private final StorageService storageService;

    public void validateThumbnail(MultipartFile file) {
        validateFileNotEmpty(file);
        validateFileSize(file, MAX_THUMBNAIL_SIZE, "Thumbnail");
        validateImageFile(file, "Thumbnail");
    }

    public void validatePromotionalVideo(MultipartFile file) {
        validateFileNotEmpty(file);
        validateFileSize(file, MAX_PROMOTIONAL_VIDEO_SIZE, "Promotional video");
        validateVideoFile(file, "Promotional video");
    }

    private void validateFileNotEmpty(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }
    }

    private void validateFileSize(MultipartFile file, long maxSize, String fileType) {
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException(
                    String.format("%s file size cannot exceed %d MB", fileType, maxSize / (1024 * 1024))
            );
        }
    }

    private void validateImageFile(MultipartFile file, String fileType) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !storageService.isImage(originalFilename)) {
            throw new IllegalArgumentException(
                    String.format("%s must be an image file (JPG, PNG, GIF, WebP)", fileType)
            );
        }
    }

    private void validateVideoFile(MultipartFile file, String fileType) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !storageService.isVideo(originalFilename)) {
            throw new IllegalArgumentException(
                    String.format("%s must be a video file (MP4, WebM, MOV, AVI)", fileType)
            );
        }
    }
}
