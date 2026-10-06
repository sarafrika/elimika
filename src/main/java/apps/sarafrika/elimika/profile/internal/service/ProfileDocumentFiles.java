package apps.sarafrika.elimika.profile.internal.service;

import apps.sarafrika.elimika.profile.spi.ProfessionalProfileService;
import apps.sarafrika.elimika.profile.spi.UserDocumentDTO;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.storage.service.CredentialsDocumentUploadRequest;
import apps.sarafrika.elimika.shared.storage.service.MediaServeService;
import apps.sarafrika.elimika.shared.storage.service.ProfileDocumentUploadResult;
import apps.sarafrika.elimika.shared.storage.service.ProfileDocumentUploadService;
import apps.sarafrika.elimika.shared.storage.service.ProfileDocumentUploadService.ProfileDocumentOwner;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.UUID;

/** Stores an uploaded credential file under the user and files it as a profile document; streams it back. */
@Component
@RequiredArgsConstructor
public class ProfileDocumentFiles {

    private final ProfileDocumentUploadService uploadService;
    private final MediaServeService mediaServeService;
    private final ProfessionalProfileService profileService;

    public UserDocumentDTO upload(UUID userUuid, MultipartFile file, UUID documentTypeUuid, String title,
                                  String description, UUID educationUuid, UUID experienceUuid, UUID membershipUuid,
                                  LocalDate expiryDate) {
        ProfileDocumentUploadResult upload = uploadService.upload(new CredentialsDocumentUploadRequest(
                ProfileDocumentOwner.USER, userUuid, file, documentTypeUuid, title, description,
                educationUuid, experienceUuid, membershipUuid, expiryDate));
        return profileService.documents().create(userUuid, new UserDocumentDTO(null, userUuid,
                upload.documentTypeUuid(), upload.educationUuid(), upload.experienceUuid(), upload.membershipUuid(),
                upload.originalFilename(), upload.storedFilename(), upload.filePath(), upload.fileSizeBytes(),
                upload.mimeType(), null, upload.resolvedTitle(), upload.description(), null, null, null, null,
                null, null, upload.expiryDate(), null, null, null, null));
    }

    public ResponseEntity<Resource> serve(UUID userUuid, UUID documentUuid) {
        UserDocumentDTO document = profileService.documents().find(documentUuid)
                .filter(found -> userUuid.equals(found.userUuid()))
                .orElseThrow(() -> new ResourceNotFoundException("Profile document with ID " + documentUuid + " not found"));
        return mediaServeService.serve(document.filePath(), document.storedFilename());
    }
}
