package apps.sarafrika.elimika.shared.storage.service;

import apps.sarafrika.elimika.shared.storage.config.StorageProperties;
import apps.sarafrika.elimika.shared.storage.service.ProfileDocumentUploadService.ProfileDocumentOwner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileDocumentUploadServiceTest {

    @Mock
    private StorageService storageService;

    private ProfileDocumentUploadService uploadService;

    @BeforeEach
    void setUp() {
        StorageProperties storageProperties = new StorageProperties();
        StorageProperties.Folders folders = new StorageProperties.Folders();
        folders.setProfileDocuments("profile_documents");
        storageProperties.setFolders(folders);

        uploadService = new ProfileDocumentUploadService(storageService, storageProperties);
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

        when(storageService.store(file, folder)).thenReturn(storedPath);
        when(storageService.getContentType(storedPath)).thenReturn(MediaType.APPLICATION_PDF_VALUE);

        UUID documentTypeUuid = UUID.randomUUID();
        UUID educationUuid = UUID.randomUUID();
        LocalDate expiryDate = LocalDate.of(2027, 6, 15);

        ProfileDocumentUploadResult result = uploadService.upload(new ProfileDocumentUploadRequest(
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

        when(storageService.store(file, folder)).thenReturn(storedPath);
        when(storageService.getContentType(storedPath)).thenReturn(MediaType.APPLICATION_PDF_VALUE);

        ProfileDocumentUploadResult result = uploadService.upload(new ProfileDocumentUploadRequest(
                ProfileDocumentOwner.COURSE_CREATOR,
                courseCreatorUuid,
                file,
                UUID.randomUUID(),
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
    void uploadRejectsNonPdfWithOwnerSpecificMessage() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "credential.png",
                MediaType.IMAGE_PNG_VALUE,
                "png".getBytes()
        );

        assertThatThrownBy(() -> uploadService.upload(new ProfileDocumentUploadRequest(
                ProfileDocumentOwner.COURSE_CREATOR,
                UUID.randomUUID(),
                file,
                UUID.randomUUID(),
                null,
                null,
                null,
                null,
                null,
                null
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only PDF files are allowed for course creator documents");
    }

    @Test
    void uploadRejectsMissingDocumentType() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "credential.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "pdf".getBytes()
        );

        assertThatThrownBy(() -> uploadService.upload(new ProfileDocumentUploadRequest(
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
}
