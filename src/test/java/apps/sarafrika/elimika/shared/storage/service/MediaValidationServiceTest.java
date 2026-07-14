package apps.sarafrika.elimika.shared.storage.service;

import apps.sarafrika.elimika.shared.storage.config.StorageProperties;
import apps.sarafrika.elimika.shared.storage.service.impl.FileSystemStorageServiceImpl;
import apps.sarafrika.elimika.shared.storage.util.MediaCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MediaValidationServiceTest {

    @TempDir
    Path tempDir;

    private MediaValidationService validationService;

    @BeforeEach
    void setUp() {
        StorageProperties properties = new StorageProperties();
        properties.setLocation(tempDir.toString());
        validationService = new MediaValidationService(new FileSystemStorageServiceImpl(properties));
    }

    private MockMultipartFile file(String name, String contentType, int size) {
        return new MockMultipartFile("file", name, contentType, new byte[size]);
    }

    @Test
    void acceptsValidFilesPerCategory() {
        assertThatCode(() -> {
            validationService.validate(file("a.png", "image/png", 1024), MediaCategory.PROFILE_IMAGE);
            validationService.validate(file("a.jpg", "image/jpeg", 1024), MediaCategory.THUMBNAIL);
            validationService.validate(file("a.webp", "image/webp", 1024), MediaCategory.BANNER);
            validationService.validate(file("a.mp4", "video/mp4", 1024), MediaCategory.VIDEO);
            validationService.validate(file("a.pdf", "application/pdf", 1024), MediaCategory.PDF_DOCUMENT);
            validationService.validate(file("a.docx", "application/octet-stream", 1024), MediaCategory.DOCUMENT);
            validationService.validate(file("a.mp3", "audio/mpeg", 1024), MediaCategory.ANY);
        }).doesNotThrowAnyException();
    }

    @Test
    void rejectsEmptyFile() {
        assertThatThrownBy(() -> validationService.validate(
                new MockMultipartFile("file", "a.png", "image/png", new byte[0]), MediaCategory.THUMBNAIL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be empty");
        assertThatThrownBy(() -> validationService.validate(null, MediaCategory.THUMBNAIL))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsOversizedFile() {
        assertThatThrownBy(() -> validationService.validate(
                file("a.png", "image/png", 6 * 1024 * 1024), MediaCategory.THUMBNAIL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot exceed 5 MB");
    }

    @Test
    void rejectsWrongKind() {
        assertThatThrownBy(() -> validationService.validate(
                file("a.mp4", "video/mp4", 1024), MediaCategory.PROFILE_IMAGE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be an image");
        assertThatThrownBy(() -> validationService.validate(
                file("a.png", "image/png", 1024), MediaCategory.VIDEO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be a video");
        assertThatThrownBy(() -> validationService.validate(
                file("a.png", "image/png", 1024), MediaCategory.PDF_DOCUMENT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only PDF files");
    }
}
