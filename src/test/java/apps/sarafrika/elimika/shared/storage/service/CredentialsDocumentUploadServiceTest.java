package apps.sarafrika.elimika.shared.storage.service;

import apps.sarafrika.elimika.shared.model.DocumentType;
import apps.sarafrika.elimika.shared.repository.DocumentTypeRepository;
import apps.sarafrika.elimika.shared.storage.config.StorageProperties;
import apps.sarafrika.elimika.shared.storage.service.ProfileDocumentUploadService.ProfileDocumentOwner;
import apps.sarafrika.elimika.shared.storage.util.MediaCategory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CredentialsDocumentUploadServiceTest {

    @Mock
    private MediaStorageService mediaStorageService;

    @Mock
    private DocumentTypeRepository documentTypeRepository;

    private ProfileDocumentUploadService uploadService;

    @BeforeEach
    void setUp() {
        StorageProperties storageProperties = new StorageProperties();
        StorageProperties.Folders folders = new StorageProperties.Folders();
        folders.setProfileDocuments("profile_documents");
        storageProperties.setFolders(folders);

        uploadService = new ProfileDocumentUploadService(
                mediaStorageService,
                storageProperties,
                documentTypeRepository,
                new ObjectMapper());
    }

    @Test
    void uploadStoresInstructorPdfUnderProfileDocumentFolder() {
        UUID instructorUuid = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "credential.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "pdf".getBytes()
        );
        String folder = "profile_documents/instructors/" + instructorUuid;
        String storedPath = folder + "/credential.pdf";

        when(mediaStorageService.store(any(MediaUploadRequest.class)))
                .thenReturn(new StoredMedia(storedPath, "credential.pdf", file.getSize(), MediaType.APPLICATION_PDF_VALUE));

        UUID documentTypeUuid = UUID.randomUUID();
        when(documentTypeRepository.findByUuid(documentTypeUuid))
                .thenReturn(Optional.of(documentType(documentTypeUuid, "[\"pdf\",\"jpg\",\"jpeg\",\"png\"]", 10)));
        UUID educationUuid = UUID.randomUUID();
        LocalDate expiryDate = LocalDate.of(2027, 6, 15);

        ProfileDocumentUploadResult result = uploadService.upload(new CredentialsDocumentUploadRequest(
                ProfileDocumentOwner.INSTRUCTOR,
                instructorUuid,
                file,
                documentTypeUuid,
                "Credential",
                "Credential description",
                educationUuid,
                null,
                null,
                expiryDate
        ));

        assertThat(result.owner()).isEqualTo(ProfileDocumentOwner.INSTRUCTOR);
        assertThat(result.ownerUuid()).isEqualTo(instructorUuid);
        assertThat(result.documentTypeUuid()).isEqualTo(documentTypeUuid);
        assertThat(result.educationUuid()).isEqualTo(educationUuid);
        assertThat(result.originalFilename()).isEqualTo("credential.pdf");
        assertThat(result.storedFilename()).isEqualTo(storedPath);
        assertThat(result.filePath()).isEqualTo(storedPath);
        assertThat(result.fileSizeBytes()).isEqualTo(file.getSize());
        assertThat(result.mimeType()).isEqualTo(MediaType.APPLICATION_PDF_VALUE);
        assertThat(result.resolvedTitle()).isEqualTo("Credential");
        assertThat(result.description()).isEqualTo("Credential description");
        assertThat(result.expiryDate()).isEqualTo(expiryDate);
    }

    @Test
    void uploadUsesCourseCreatorDefaultsForBlankTitle() {
        UUID courseCreatorUuid = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "portfolio.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "pdf".getBytes()
        );
        String folder = "profile_documents/course-creators/" + courseCreatorUuid;
        String storedPath = folder + "/portfolio.pdf";
        UUID documentTypeUuid = UUID.randomUUID();

        when(documentTypeRepository.findByUuid(documentTypeUuid))
                .thenReturn(Optional.of(documentType(documentTypeUuid, "[\"pdf\"]", 10)));
        when(mediaStorageService.store(any(MediaUploadRequest.class)))
                .thenReturn(new StoredMedia(storedPath, "portfolio.pdf", file.getSize(), MediaType.APPLICATION_PDF_VALUE));

        ProfileDocumentUploadResult result = uploadService.upload(new CredentialsDocumentUploadRequest(
                ProfileDocumentOwner.COURSE_CREATOR,
                courseCreatorUuid,
                file,
                documentTypeUuid,
                " ",
                null,
                null,
                null,
                null,
                null
        ));

        assertThat(result.resolvedTitle()).isEqualTo("portfolio.pdf");
    }

    @Test
    void uploadRoutesThroughFacadeWithDocumentCategory() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "credential.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "pdf".getBytes()
        );
        UUID documentTypeUuid = UUID.randomUUID();
        when(documentTypeRepository.findByUuid(documentTypeUuid))
                .thenReturn(Optional.of(documentType(documentTypeUuid, "[\"pdf\"]", 10)));
        when(mediaStorageService.store(any(MediaUploadRequest.class)))
                .thenAnswer(invocation -> {
                    MediaUploadRequest request = invocation.getArgument(0);
                    assertThat(request.category()).isEqualTo(MediaCategory.DOCUMENT);
                    return new StoredMedia("profile_documents/x.pdf", "credential.pdf", 3, MediaType.APPLICATION_PDF_VALUE);
                });

        uploadService.upload(new CredentialsDocumentUploadRequest(
                ProfileDocumentOwner.COURSE_CREATOR,
                UUID.randomUUID(),
                file,
                documentTypeUuid,
                null,
                null,
                null,
                null,
                null,
                null
        ));
    }

    @Test
    void uploadAllowsImageWhenDocumentTypeAllowsIt() {
        UUID courseCreatorUuid = UUID.randomUUID();
        UUID documentTypeUuid = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "certificate.png",
                MediaType.IMAGE_PNG_VALUE,
                "png".getBytes()
        );
        String folder = "profile_documents/course-creators/" + courseCreatorUuid;
        String storedPath = folder + "/certificate.png";

        when(documentTypeRepository.findByUuid(documentTypeUuid))
                .thenReturn(Optional.of(documentType(documentTypeUuid, "[\"pdf\",\"jpg\",\"jpeg\",\"png\"]", 10)));
        when(mediaStorageService.store(any(MediaUploadRequest.class)))
                .thenAnswer(invocation -> {
                    MediaUploadRequest request = invocation.getArgument(0);
                    assertThat(request.category()).isEqualTo(MediaCategory.DOCUMENT);
                    return new StoredMedia(storedPath, "certificate.png", file.getSize(), MediaType.IMAGE_PNG_VALUE);
                });

        ProfileDocumentUploadResult result = uploadService.upload(new CredentialsDocumentUploadRequest(
                ProfileDocumentOwner.COURSE_CREATOR,
                courseCreatorUuid,
                file,
                documentTypeUuid,
                "Certificate",
                null,
                null,
                null,
                null,
                null
        ));

        assertThat(result.storedFilename()).isEqualTo(storedPath);
        assertThat(result.mimeType()).isEqualTo(MediaType.IMAGE_PNG_VALUE);
    }

    @Test
    void uploadRejectsExtensionNotAllowedForDocumentType() {
        UUID documentTypeUuid = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "certificate.png",
                MediaType.IMAGE_PNG_VALUE,
                "png".getBytes()
        );

        when(documentTypeRepository.findByUuid(documentTypeUuid))
                .thenReturn(Optional.of(documentType(documentTypeUuid, "[\"pdf\"]", 10)));

        assertThatThrownBy(() -> uploadService.upload(new CredentialsDocumentUploadRequest(
                ProfileDocumentOwner.INSTRUCTOR,
                UUID.randomUUID(),
                file,
                documentTypeUuid,
                null,
                null,
                null,
                null,
                null,
                null
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only PDF files are allowed");
    }

    @Test
    void uploadRejectsFilesOverDocumentTypeLimit() {
        UUID documentTypeUuid = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "certificate.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                new byte[2 * 1024 * 1024]
        );

        when(documentTypeRepository.findByUuid(documentTypeUuid))
                .thenReturn(Optional.of(documentType(documentTypeUuid, "[\"pdf\"]", 1)));

        assertThatThrownBy(() -> uploadService.upload(new CredentialsDocumentUploadRequest(
                ProfileDocumentOwner.INSTRUCTOR,
                UUID.randomUUID(),
                file,
                documentTypeUuid,
                null,
                null,
                null,
                null,
                null,
                null
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot exceed 1 MB");
    }

    @Test
    void uploadRejectsMissingFile() {
        assertThatThrownBy(() -> uploadService.upload(new CredentialsDocumentUploadRequest(
                ProfileDocumentOwner.INSTRUCTOR,
                UUID.randomUUID(),
                null,
                UUID.randomUUID(),
                null,
                null,
                null,
                null,
                null,
                null
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Profile document file is required");
    }

    @Test
    void uploadRejectsMissingDocumentType() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "credential.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "pdf".getBytes()
        );

        assertThatThrownBy(() -> uploadService.upload(new CredentialsDocumentUploadRequest(
                ProfileDocumentOwner.INSTRUCTOR,
                UUID.randomUUID(),
                file,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Document type UUID is required");
    }

    private DocumentType documentType(UUID uuid, String allowedExtensions, Integer maxFileSizeMb) {
        DocumentType documentType = new DocumentType();
        documentType.setUuid(uuid);
        documentType.setName("CERTIFICATE");
        documentType.setAllowedExtensions(allowedExtensions);
        documentType.setMaxFileSizeMb(maxFileSizeMb);
        return documentType;
    }
}
