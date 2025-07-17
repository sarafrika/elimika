package apps.sarafrika.elimika.course.internal;

import apps.sarafrika.elimika.shared.storage.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service for validating course media files before upload
 */
@Service
@RequiredArgsConstructor
public class CourseMediaValidationService {

    private final StorageService storageService;

    // File size limits in bytes
    private static final long MAX_THUMBNAIL_SIZE = 5 * 1024 * 1024; // 5MB
    private static final long MAX_BANNER_SIZE = 10 * 1024 * 1024; // 10MB
    private static final long MAX_VIDEO_SIZE = 100 * 1024 * 1024; // 100MB

    /**
     * Validates thumbnail image files
     */
    public void validateThumbnail(MultipartFile file) {
        validateFileNotEmpty(file);
        validateFileSize(file, MAX_THUMBNAIL_SIZE, "Thumbnail");
        validateImageFile(file, "Thumbnail");
    }

    /**
     * Validates banner image files
     */
    public void validateBanner(MultipartFile file) {
        validateFileNotEmpty(file);
        validateFileSize(file, MAX_BANNER_SIZE, "Banner");
        validateImageFile(file, "Banner");
    }

    /**
     * Validates intro video files
     */
    public void validateIntroVideo(MultipartFile file) {
        validateFileNotEmpty(file);
        validateFileSize(file, MAX_VIDEO_SIZE, "Intro video");
        validateVideoFile(file, "Intro video");
    }

    private void validateFileNotEmpty(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }
    }

    private void validateFileSize(MultipartFile file, long maxSize, String fileType) {
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException(
                    String.format("%s file size cannot exceed %d MB",
                            fileType, maxSize / (1024 * 1024))
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