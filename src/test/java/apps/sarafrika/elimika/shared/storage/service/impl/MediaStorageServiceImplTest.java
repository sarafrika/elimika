package apps.sarafrika.elimika.shared.storage.service.impl;

import apps.sarafrika.elimika.shared.storage.config.StorageProperties;
import apps.sarafrika.elimika.shared.storage.config.exception.StorageFileNotFoundException;
import apps.sarafrika.elimika.shared.storage.model.MediaFile;
import apps.sarafrika.elimika.shared.storage.repository.MediaFileRepository;
import apps.sarafrika.elimika.shared.storage.service.MediaUploadRequest;
import apps.sarafrika.elimika.shared.storage.service.MediaValidationService;
import apps.sarafrika.elimika.shared.storage.service.StoredMedia;
import apps.sarafrika.elimika.shared.storage.util.MediaCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Exercises the facade against a real {@link FileSystemStorageServiceImpl} on a temp
 * directory so store/replace/delete behavior is observed on actual files.
 */
@ExtendWith(MockitoExtension.class)
class MediaStorageServiceImplTest {

    @TempDir
    Path tempDir;

    @Mock
    private MediaFileRepository mediaFileRepository;

    private FileSystemStorageServiceImpl storageService;
    private MediaStorageServiceImpl mediaStorageService;

    @BeforeEach
    void setUp() {
        StorageProperties properties = new StorageProperties();
        properties.setLocation(tempDir.toString());
        storageService = new FileSystemStorageServiceImpl(properties);
        storageService.init();
        mediaStorageService = new MediaStorageServiceImpl(
                storageService, new MediaValidationService(storageService), mediaFileRepository);
    }

    @Test
    void storeWritesFileRegistersMetadataAndReturnsKey() {
        when(mediaFileRepository.findByFileKey(any())).thenReturn(Optional.empty());
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", "png".getBytes());

        StoredMedia stored = mediaStorageService.store(new MediaUploadRequest(
                file, MediaCategory.THUMBNAIL, "course_thumbnails", "COURSE_THUMBNAIL", UUID.randomUUID(), null));

        assertThat(stored.key()).startsWith("course_thumbnails/").endsWith(".png");
        assertThat(storageService.exists(stored.key())).isTrue();
        assertThat(stored.publicUrl()).isEqualTo("/api/v1/files/" + stored.key());
        verify(mediaFileRepository).save(any(MediaFile.class));
    }

    @Test
    void storeDeletesPreviousFileOnReplace() throws Exception {
        when(mediaFileRepository.findByFileKey(any())).thenReturn(Optional.empty());
        Path previousDir = tempDir.resolve("course_thumbnails");
        Files.createDirectories(previousDir);
        Path previous = previousDir.resolve("old.png");
        Files.writeString(previous, "old");

        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", "png".getBytes());
        mediaStorageService.store(new MediaUploadRequest(
                file, MediaCategory.THUMBNAIL, "course_thumbnails", "COURSE_THUMBNAIL", UUID.randomUUID(),
                "course_thumbnails/old.png"));

        assertThat(Files.exists(previous)).isFalse();
    }

    @Test
    void storeRejectsInvalidFileBeforeTouchingDisk() {
        MockMultipartFile file = new MockMultipartFile("file", "movie.mp4", "video/mp4", "x".getBytes());

        assertThatThrownBy(() -> mediaStorageService.store(new MediaUploadRequest(
                file, MediaCategory.THUMBNAIL, "course_thumbnails", "COURSE_THUMBNAIL", null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be an image");
        assertThat(storageService.listAllKeys()).isEmpty();
    }

    @Test
    void deleteIsIdempotentAndExternalSafe() {
        mediaStorageService.delete(null);
        mediaStorageService.delete("https://cdn.example.com/x.mp4");
        mediaStorageService.delete("course_thumbnails/missing.png");
    }

    @Test
    void deleteRemovesFileAndRegistryEntryFromLegacyUrl() throws Exception {
        Path dir = tempDir.resolve("certificates");
        Files.createDirectories(dir);
        Path fileOnDisk = dir.resolve("cert.pdf");
        Files.writeString(fileOnDisk, "pdf");
        MediaFile registryRow = new MediaFile();
        registryRow.setFileKey("certificates/cert.pdf");
        when(mediaFileRepository.findByFileKey("certificates/cert.pdf")).thenReturn(Optional.of(registryRow));

        mediaStorageService.delete("/api/v1/certificates/files/certificates/cert.pdf");

        assertThat(Files.exists(fileOnDisk)).isFalse();
        verify(mediaFileRepository).delete(registryRow);
    }

    @Test
    void listAllKeysExcludesTempFolder() throws Exception {
        Files.createDirectories(tempDir.resolve("temp"));
        Files.writeString(tempDir.resolve("temp").resolve("scratch.bin"), "x");
        Files.createDirectories(tempDir.resolve("assignments"));
        Files.writeString(tempDir.resolve("assignments").resolve("a.pdf"), "x");

        assertThat(storageService.listAllKeys()).containsExactly("assignments/a.pdf");
    }

    @Test
    void deleteRejectsPathTraversal() {
        assertThatThrownBy(() -> storageService.delete("../outside.txt"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void deletePrunesEmptyParentDirectories() throws Exception {
        Path nested = tempDir.resolve("assignments/a1/attachments");
        Files.createDirectories(nested);
        Path file = nested.resolve("f.pdf");
        Files.writeString(file, "x");

        storageService.delete("assignments/a1/attachments/f.pdf");

        assertThat(Files.exists(file)).isFalse();
        assertThat(Files.exists(tempDir.resolve("assignments"))).isFalse();
    }

    @Test
    void loadMissingFileThrowsNotFound() {
        assertThatThrownBy(() -> storageService.load("nope/missing.png"))
                .isInstanceOf(StorageFileNotFoundException.class);
    }

    @Test
    void deleteAllForOwnerRemovesEveryRegisteredFile() throws Exception {
        UUID owner = UUID.randomUUID();
        Path dir = tempDir.resolve("class_thumbnails");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("a.png"), "a");
        Files.writeString(dir.resolve("b.png"), "b");
        MediaFile a = new MediaFile();
        a.setFileKey("class_thumbnails/a.png");
        MediaFile b = new MediaFile();
        b.setFileKey("class_thumbnails/b.png");
        when(mediaFileRepository.findByOwnerTypeAndOwnerUuid("CLASS_THUMBNAIL", owner)).thenReturn(List.of(a, b));

        mediaStorageService.deleteAllForOwner("CLASS_THUMBNAIL", owner);

        assertThat(storageService.listAllKeys()).isEmpty();
        verify(mediaFileRepository).delete(a);
        verify(mediaFileRepository).delete(b);
    }
}
