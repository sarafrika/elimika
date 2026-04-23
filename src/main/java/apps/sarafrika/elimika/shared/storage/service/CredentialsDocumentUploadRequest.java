package apps.sarafrika.elimika.shared.storage.service;

import apps.sarafrika.elimika.shared.storage.service.ProfileDocumentUploadService.ProfileDocumentOwner;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.UUID;

public record CredentialsDocumentUploadRequest(
        ProfileDocumentOwner owner,
        UUID ownerUuid,
        MultipartFile file,
        UUID documentTypeUuid,
        String title,
        String description,
        UUID educationUuid,
        UUID experienceUuid,
        UUID membershipUuid,
        LocalDate expiryDate
) {
}
