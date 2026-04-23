package apps.sarafrika.elimika.shared.storage.service;

import apps.sarafrika.elimika.shared.storage.config.StorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileDocumentUploadService {

    private static final long MAX_DOCUMENT_SIZE = 50 * 1024 * 1024;
    private static final String PDF_CONTENT_TYPE = "application/pdf";

    private final StorageService storageService;
    private final StorageProperties storageProperties;

    public ProfileDocumentUploadResult upload(ProfileDocumentOwner owner, UUID ownerUuid, MultipartFile file, String title) {
        validateDocument(file, owner.documentLabel());

        String folder = storageProperties.getFolders().getProfileDocuments()
                + "/" + owner.folderName() + "/" + ownerUuid;
        String storedFilename = storageService.store(file, folder);
        String originalFilename = resolveOriginalFilename(file.getOriginalFilename(), storedFilename);
        String resolvedTitle = resolveTitle(title, originalFilename, owner.defaultTitle());

        return new ProfileDocumentUploadResult(
                originalFilename,
                storedFilename,
                storedFilename,
                file.getSize(),
                storageService.getContentType(storedFilename),
                resolvedTitle
        );
    }

    private void validateDocument(MultipartFile file, String documentLabel) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Document file cannot be empty");
        }
        if (!PDF_CONTENT_TYPE.equals(file.getContentType())) {
            throw new IllegalArgumentException("Only PDF files are allowed for " + documentLabel + " documents");
        }
        if (file.getSize() > MAX_DOCUMENT_SIZE) {
            throw new IllegalArgumentException(
                    String.format("Document file size cannot exceed %d MB", MAX_DOCUMENT_SIZE / (1024 * 1024))
            );
        }
    }

    private String resolveTitle(String title, String originalFilename, String defaultTitle) {
        if (title != null && !title.isBlank()) {
            return title;
        }
        return originalFilename != null && !originalFilename.isBlank() ? originalFilename : defaultTitle;
    }

    private String resolveOriginalFilename(String originalFilename, String storedFilename) {
        if (originalFilename != null && !originalFilename.isBlank()) {
            return originalFilename;
        }
        if (storedFilename == null || storedFilename.isBlank()) {
            return "document.pdf";
        }
        int lastSeparator = storedFilename.lastIndexOf('/');
        return lastSeparator >= 0 ? storedFilename.substring(lastSeparator + 1) : storedFilename;
    }

    public enum ProfileDocumentOwner {
        INSTRUCTOR("instructors", "instructor", "Instructor Document"),
        COURSE_CREATOR("course-creators", "course creator", "Course Creator Document");

        private final String folderName;
        private final String documentLabel;
        private final String defaultTitle;

        ProfileDocumentOwner(String folderName, String documentLabel, String defaultTitle) {
            this.folderName = folderName;
            this.documentLabel = documentLabel;
            this.defaultTitle = defaultTitle;
        }

        public String folderName() {
            return folderName;
        }

        public String documentLabel() {
            return documentLabel;
        }

        public String defaultTitle() {
            return defaultTitle;
        }
    }
}
