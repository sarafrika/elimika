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

        ProfileDocumentUploadResult result = uploadService.upload(
                ProfileDocumentOwner.INSTRUCTOR,
                instructorUuid,
                file,
                "Credential"
        );

        assertThat(result.originalFilename()).isEqualTo("credential.pdf");
        assertThat(result.storedFilename()).isEqualTo(storedPath);
        assertThat(result.filePath()).isEqualTo(storedPath);
        assertThat(result.fileSizeBytes()).isEqualTo(file.getSize());
        assertThat(result.mimeType()).isEqualTo(MediaType.APPLICATION_PDF_VALUE);
        assertThat(result.resolvedTitle()).isEqualTo("Credential");
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

        ProfileDocumentUploadResult result = uploadService.upload(
                ProfileDocumentOwner.COURSE_CREATOR,
                courseCreatorUuid,
                file,
                " "
        );

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

        assertThatThrownBy(() -> uploadService.upload(ProfileDocumentOwner.COURSE_CREATOR, UUID.randomUUID(), file, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only PDF files are allowed for course creator documents");
    }
}
