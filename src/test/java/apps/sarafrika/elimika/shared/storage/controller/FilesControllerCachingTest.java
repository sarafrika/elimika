package apps.sarafrika.elimika.shared.storage.controller;

import apps.sarafrika.elimika.shared.storage.config.StorageProperties;
import apps.sarafrika.elimika.shared.storage.internal.MediaReconciliationService;
import apps.sarafrika.elimika.shared.storage.service.MediaServeService;
import apps.sarafrika.elimika.shared.storage.service.impl.FileSystemStorageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Drives {@code GET /api/v1/files/**} against real files on a temp directory to
 * pin the cache headers, ETag revalidation and on-demand resized variants.
 */
class FilesControllerCachingTest {

    @TempDir
    Path storageRoot;

    private MockMvc mockMvc;
    private FileSystemStorageServiceImpl storageService;
    private MediaServeService mediaServeService;

    @BeforeEach
    void setUp() {
        StorageProperties properties = new StorageProperties();
        properties.setLocation(storageRoot.toString());
        storageService = new FileSystemStorageServiceImpl(properties);
        storageService.init();
        mediaServeService = new MediaServeService(storageService, properties);
        FilesController controller = new FilesController(
                mediaServeService, Mockito.mock(MediaReconciliationService.class));
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void publicFileIsImmutablePublicAndCarriesEtag() throws Exception {
        writeFile("course_thumbnails/a.txt", "hello".getBytes());

        mockMvc.perform(get("/api/v1/files/course_thumbnails/a.txt"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "max-age=31536000, public, immutable"))
                .andExpect(header().exists(HttpHeaders.ETAG))
                .andExpect(header().exists(HttpHeaders.LAST_MODIFIED))
                .andExpect(content().bytes("hello".getBytes()));
    }

    @Test
    void profileDocumentsAreNeverPubliclyCacheable() throws Exception {
        writeFile("profile_documents/id.pdf", "pdf".getBytes());

        mockMvc.perform(get("/api/v1/files/profile_documents/id.pdf"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "max-age=31536000, private, immutable"));
    }

    @Test
    void matchingIfNoneMatchReturnsNotModified() throws Exception {
        writeFile("course_thumbnails/a.txt", "hello".getBytes());
        String etag = mockMvc.perform(get("/api/v1/files/course_thumbnails/a.txt"))
                .andReturn().getResponse().getHeader(HttpHeaders.ETAG);

        mockMvc.perform(get("/api/v1/files/course_thumbnails/a.txt").header(HttpHeaders.IF_NONE_MATCH, etag))
                .andExpect(status().isNotModified())
                .andExpect(content().bytes(new byte[0]));
    }

    @Test
    void widthParameterServesDownscaledJpegAndCachesIt() throws Exception {
        writeFile("course_thumbnails/photo.png", png(2000, 1000, BufferedImage.TYPE_INT_RGB));

        MvcResult result = mockMvc.perform(get("/api/v1/files/course_thumbnails/photo.png").param("w", "640"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "image/jpeg"))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "max-age=31536000, public, immutable"))
                .andReturn();

        BufferedImage served = ImageIO.read(new ByteArrayInputStream(result.getResponse().getContentAsByteArray()));
        assertThat(served.getWidth()).isEqualTo(640);
        assertThat(served.getHeight()).isEqualTo(320);
        assertThat(storageRoot.resolve("variants/w640/course_thumbnails/photo.png.jpg")).exists();
        assertThat(storageService.listAllKeys()).containsExactly("course_thumbnails/photo.png");
    }

    @Test
    void transparentImagesStayPng() throws Exception {
        writeFile("organization_logos/logo.png", png(1000, 1000, BufferedImage.TYPE_INT_ARGB));

        MvcResult result = mockMvc.perform(get("/api/v1/files/organization_logos/logo.png").param("w", "320"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "image/png"))
                .andReturn();

        assertThat(ImageIO.read(new ByteArrayInputStream(result.getResponse().getContentAsByteArray())).getWidth())
                .isEqualTo(320);
    }

    @Test
    void smallImagesAreNeverUpscaled() throws Exception {
        byte[] original = png(200, 100, BufferedImage.TYPE_INT_RGB);
        writeFile("course_thumbnails/small.png", original);

        mockMvc.perform(get("/api/v1/files/course_thumbnails/small.png").param("w", "1280"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "image/png"))
                .andExpect(content().bytes(original));
    }

    @Test
    void narrowPortraitImagesAreNeverUpscaled() throws Exception {
        byte[] original = png(300, 2000, BufferedImage.TYPE_INT_RGB);
        writeFile("course_thumbnails/tall.png", original);

        mockMvc.perform(get("/api/v1/files/course_thumbnails/tall.png").param("w", "320"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "image/png"))
                .andExpect(content().bytes(original));
    }

    @Test
    void legacyServeIsPrivateSoSharedCachesNeverStoreAuthenticatedMedia() throws Exception {
        writeFile("assignments/submissions/work.pdf", "work".getBytes());

        ResponseEntity<Resource> response = mediaServeService.serve("assignments/submissions/work.pdf");

        assertThat(response.getHeaders().getCacheControl()).isEqualTo("max-age=31536000, private, immutable");
    }

    @Test
    void deletingAnOriginalRemovesItsVariants() throws Exception {
        writeFile("course_thumbnails/photo.png", png(2000, 1000, BufferedImage.TYPE_INT_RGB));
        mockMvc.perform(get("/api/v1/files/course_thumbnails/photo.png").param("w", "320"))
                .andExpect(status().isOk());
        assertThat(storageRoot.resolve("variants/w320/course_thumbnails/photo.png.jpg")).exists();

        storageService.delete("course_thumbnails/photo.png");

        assertThat(storageRoot.resolve("variants")).doesNotExist();
    }

    @Test
    void concurrentFirstRequestsShareOneVariant() throws Exception {
        writeFile("course_thumbnails/photo.png", png(2000, 1000, BufferedImage.TYPE_INT_RGB));
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            List<Future<ResponseEntity<Resource>>> calls = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                calls.add(pool.submit(() -> mediaServeService.serveSized("course_thumbnails/photo.png", 640)));
            }
            for (Future<ResponseEntity<Resource>> call : calls) {
                assertThat(call.get().getHeaders().getContentType()).hasToString("image/jpeg");
            }
        } finally {
            pool.shutdownNow();
        }
        try (var files = Files.list(storageRoot.resolve("variants/w640/course_thumbnails"))) {
            assertThat(files.toList()).hasSize(1);
        }
    }

    @Test
    void unsupportedWidthIsRejected() throws Exception {
        writeFile("course_thumbnails/photo.png", png(2000, 1000, BufferedImage.TYPE_INT_RGB));

        mockMvc.perform(get("/api/v1/files/course_thumbnails/photo.png").param("w", "500"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void profileDocumentsAreNeverResizedIntoThePublicVariantsFolder() throws Exception {
        byte[] original = png(2000, 1000, BufferedImage.TYPE_INT_RGB);
        writeFile("profile_documents/id.png", original);

        mockMvc.perform(get("/api/v1/files/profile_documents/id.png").param("w", "640"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(original));
        assertThat(storageRoot.resolve("variants")).doesNotExist();
    }

    private void writeFile(String key, byte[] bytes) throws Exception {
        Path target = storageRoot.resolve(key);
        Files.createDirectories(target.getParent());
        Files.write(target, bytes);
    }

    private static byte[] png(int width, int height, int type) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(width, height, type), "png", out);
        return out.toByteArray();
    }
}
