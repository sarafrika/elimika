package apps.sarafrika.elimika.shared.storage.service;

import apps.sarafrika.elimika.shared.storage.config.StorageProperties;
import apps.sarafrika.elimika.shared.storage.util.MediaCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ProfileDocumentUploadService {

    private final MediaStorageService mediaStorageService;
    private final StorageProperties storageProperties;

    public ProfileDocumentUploadResult upload(CredentialsDocumentUploadRequest request) {
        validateRequest(request);

        ProfileDocumentOwner owner = request.owner();
        MultipartFile file = request.file();

        String folder = storageProperties.getFolders().getProfileDocuments()
                + "/" + owner.folderName() + "/" + request.ownerUuid();
        StoredMedia storedMedia = mediaStorageService.store(new MediaUploadRequest(
                file, MediaCategory.PDF_DOCUMENT, folder, owner.mediaOwnerType(), request.ownerUuid(), null));
        String storedFilename = storedMedia.key();
        String originalFilename = resolveOriginalFilename(file.getOriginalFilename(), storedFilename);
        String resolvedTitle = resolveTitle(request.title(), originalFilename, owner.defaultTitle());

        return new ProfileDocumentUploadResult(
                owner,
                request.ownerUuid(),
                request.documentTypeUuid(),
                request.educationUuid(),
                request.experienceUuid(),
                request.membershipUuid(),
                originalFilename,
                storedFilename,
                storedFilename,
                storedMedia.sizeBytes(),
                storedMedia.mimeType(),
                resolvedTitle,
                request.description(),
                request.expiryDate()
        );
    }

    private void validateRequest(CredentialsDocumentUploadRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Profile document upload request is required");
        }
        if (request.owner() == null) {
            throw new IllegalArgumentException("Profile document owner is required");
        }
        if (request.ownerUuid() == null) {
            throw new IllegalArgumentException("Profile document owner UUID is required");
        }
        if (request.documentTypeUuid() == null) {
            throw new IllegalArgumentException("Document type UUID is required");
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
        INSTRUCTOR("instructors", "instructor", "Instructor Document",
                apps.sarafrika.elimika.shared.storage.util.MediaOwnerType.INSTRUCTOR_DOCUMENT),
        COURSE_CREATOR("course-creators", "course creator", "Course Creator Document",
                apps.sarafrika.elimika.shared.storage.util.MediaOwnerType.COURSE_CREATOR_DOCUMENT);

        private final String folderName;
        private final String documentLabel;
        private final String defaultTitle;
        private final String mediaOwnerType;

        ProfileDocumentOwner(String folderName, String documentLabel, String defaultTitle, String mediaOwnerType) {
            this.folderName = folderName;
            this.documentLabel = documentLabel;
            this.defaultTitle = defaultTitle;
            this.mediaOwnerType = mediaOwnerType;
        }

        public String mediaOwnerType() {
            return mediaOwnerType;
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
