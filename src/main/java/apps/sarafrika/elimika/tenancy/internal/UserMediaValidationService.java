package apps.sarafrika.elimika.tenancy.internal;

import apps.sarafrika.elimika.shared.storage.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service for validating user media files before upload
 */
@Service
@RequiredArgsConstructor
public class UserMediaValidationService {

    private final StorageService storageService;

    // File size limits in bytes
    private static final long MAX_PROFILE_IMAGE_SIZE = 5 * 1024 * 1024; // 5MB

    /**
     * Validates profile image files
     */
    public void validateProfileImage(MultipartFile file) {
        validateFileNotEmpty(file);
        validateFileSize(file, MAX_PROFILE_IMAGE_SIZE, "Profile image");
        validateImageFile(file, "Profile image");
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
}