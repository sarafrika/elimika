package apps.sarafrika.elimika.shared.storage.service;

import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.model.DocumentType;
import apps.sarafrika.elimika.shared.repository.DocumentTypeRepository;
import apps.sarafrika.elimika.shared.storage.config.StorageProperties;
import apps.sarafrika.elimika.shared.storage.util.MediaCategory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ProfileDocumentUploadService {

    private final MediaStorageService mediaStorageService;
    private final StorageProperties storageProperties;
    private final DocumentTypeRepository documentTypeRepository;
    private final ObjectMapper objectMapper;

    public ProfileDocumentUploadResult upload(CredentialsDocumentUploadRequest request) {
        validateRequest(request);

        ProfileDocumentOwner owner = request.owner();
        MultipartFile file = request.file();
        DocumentType documentType = documentTypeRepository.findByUuid(request.documentTypeUuid())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Document type with ID " + request.documentTypeUuid() + " not found"));
        validateDocumentTypeRules(file, documentType, owner);

        String folder = storageProperties.getFolders().getProfileDocuments()
                + "/" + owner.folderName() + "/" + request.ownerUuid();
        StoredMedia storedMedia = mediaStorageService.store(new MediaUploadRequest(
                file, MediaCategory.DOCUMENT, folder, owner.mediaOwnerType(), request.ownerUuid(), null));
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
        if (request.file() == null || request.file().isEmpty()) {
            throw new IllegalArgumentException("Profile document file is required");
        }
        if (request.documentTypeUuid() == null) {
            throw new IllegalArgumentException("Document type UUID is required");
        }
    }

    private void validateDocumentTypeRules(MultipartFile file, DocumentType documentType, ProfileDocumentOwner owner) {
        Integer maxFileSizeMb = documentType.getMaxFileSizeMb();
        if (maxFileSizeMb != null && maxFileSizeMb > 0 && file.getSize() > maxFileSizeMb * 1024L * 1024L) {
            throw new IllegalArgumentException(String.format(
                    "%s file size cannot exceed %d MB", owner.documentLabel(), maxFileSizeMb));
        }

        List<String> allowedExtensions = parseAllowedExtensions(documentType.getAllowedExtensions());
        if (allowedExtensions.isEmpty()) {
            return;
        }

        String extension = getExtension(file.getOriginalFilename());
        boolean allowed = allowedExtensions.stream()
                .map(ProfileDocumentUploadService::normalizeExtension)
                .anyMatch(allowedExtension -> allowedExtension.equals(extension));
        if (!allowed) {
            throw new IllegalArgumentException(String.format(
                    "Only %s files are allowed for %s uploads",
                    formatExtensions(allowedExtensions),
                    owner.documentLabel()));
        }
    }

    private List<String> parseAllowedExtensions(String rawExtensions) {
        if (!StringUtils.hasText(rawExtensions)) {
            return List.of();
        }

        try {
            JsonNode node = objectMapper.readTree(rawExtensions);
            if (!node.isArray()) {
                return List.of();
            }

            List<String> extensions = new ArrayList<>();
            node.forEach(item -> {
                if (item.isTextual() && StringUtils.hasText(item.asText())) {
                    extensions.add(normalizeExtension(item.asText()));
                }
            });
            return List.copyOf(extensions);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Document type allowed extensions are not configured correctly", ex);
        }
    }

    private static String getExtension(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "";
        }
        int lastDot = filename.lastIndexOf('.');
        if (lastDot < 0 || lastDot == filename.length() - 1) {
            return "";
        }
        return normalizeExtension(filename.substring(lastDot + 1));
    }

    private static String normalizeExtension(String extension) {
        return extension == null ? "" : extension.replace(".", "").trim().toLowerCase(Locale.ROOT);
    }

    private static String formatExtensions(List<String> extensions) {
        return extensions.stream()
                .map(ProfileDocumentUploadService::normalizeExtension)
                .filter(StringUtils::hasText)
                .map(extension -> extension.toUpperCase(Locale.ROOT))
                .distinct()
                .sorted()
                .reduce((left, right) -> left + ", " + right)
                .orElse("configured");
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
