package apps.sarafrika.elimika.shared.storage.service;

import apps.sarafrika.elimika.shared.storage.util.MediaCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Consolidated upload validation: one rule set per {@link MediaCategory}.
 */
@Service
@RequiredArgsConstructor
public class MediaValidationService {

    private static final String PDF_CONTENT_TYPE = "application/pdf";

    private final StorageService storageService;

    public void validate(MultipartFile file, MediaCategory category) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(category.label() + " file cannot be empty");
        }
        if (file.getSize() > category.maxSizeBytes()) {
            throw new IllegalArgumentException(
                    String.format("%s file size cannot exceed %d MB", category.label(), category.maxSizeMb()));
        }

        String fileName = file.getOriginalFilename();
        boolean allowed = switch (category.kind()) {
            case IMAGE -> storageService.isImage(fileName);
            case VIDEO -> storageService.isVideo(fileName);
            case DOCUMENT -> storageService.isDocument(fileName)
                    || storageService.isImage(fileName)
                    || storageService.isVideo(fileName)
                    || storageService.isAudio(fileName);
            case PDF -> PDF_CONTENT_TYPE.equals(file.getContentType())
                    || "pdf".equals(storageService.getFileExtension(fileName));
        };
        if (!allowed) {
            throw new IllegalArgumentException(switch (category.kind()) {
                case IMAGE -> String.format("%s must be an image file (JPEG, PNG, GIF, or WebP)", category.label());
                case VIDEO -> String.format("%s must be a video file (MP4, WebM, MOV, or AVI)", category.label());
                case DOCUMENT -> String.format("%s file type is not supported", category.label());
                case PDF -> String.format("Only PDF files are allowed for %s uploads", category.label().toLowerCase());
            });
        }
    }
}
