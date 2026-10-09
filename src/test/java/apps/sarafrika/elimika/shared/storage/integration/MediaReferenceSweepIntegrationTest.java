package apps.sarafrika.elimika.shared.storage.integration;

import apps.sarafrika.elimika.shared.search.SearchIndexRequests;
import apps.sarafrika.elimika.shared.storage.internal.MediaReferenceSweep;
import apps.sarafrika.elimika.shared.storage.service.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Media columns pointing at files that are gone are cleared once, and only those; the search
 * documents that embed them are re-synced.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest
@Testcontainers
@DisplayName("Dangling media reference sweep")
class MediaReferenceSweepIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> "http://localhost/realms/test");
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", () -> "http://localhost/realms/test/certs");
        registry.add("MAIL_SERVER", () -> "localhost");
        registry.add("MAIL_USERNAME", () -> "test");
        registry.add("MAIL_PASSWORD", () -> "test");
        registry.add("app.keycloak.admin.clientId", () -> "test-admin");
        registry.add("app.keycloak.admin.clientSecret", () -> "test-secret");
        registry.add("encryption.secret-key", () -> "0123456789abcdef0123456789abcdef");
        registry.add("encryption.salt", () -> "0123456789abcdef");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("app.media.sweep-dangling-refs", () -> "true");
    }

    private static final String LIVE_THUMB = "course_thumbnails/live.jpeg";
    private static final String LIVE_BANNER = "course_banners/live.png";
    private static final String LIVE_PROGRAM_BANNER = "course_banners/program-live.png";
    private static final String LOST_THUMB = "course_thumbnails/111d2a7f-604f-419f-b176-a111e4d46393.jpeg";
    private static final String LOST_BANNER = "course_banners/c80696a4-719f-424a-960a-aba99045e520.jpeg";
    private static final String EXTERNAL = "https://images.example.org/cover.png";

    @Autowired private MediaReferenceSweep sweep;
    @Autowired private JdbcTemplate jdbc;

    @MockBean private StorageService storageService;
    @MockBean private SearchIndexRequests searchIndexRequests;
    @MockBean private JwtDecoder jwtDecoder;

    private UUID liveCourse;
    private UUID lostCourse;
    private UUID program;

    @BeforeEach
    void seed() {
        jdbc.execute("TRUNCATE training_programs, courses, course_creators, users RESTART IDENTITY CASCADE");
        UUID userUuid = UUID.randomUUID();
        jdbc.update("INSERT INTO users (uuid, user_no, first_name, last_name, email, created_by) "
                        + "VALUES (?, ?, 'Me', 'Dia', 'media@test.local', 'test')",
                userUuid, String.format("%09d", Math.abs(userUuid.hashCode()) % 1000000000));
        UUID creatorUuid = UUID.randomUUID();
        jdbc.update("INSERT INTO course_creators (uuid, user_uuid, full_name, created_by) "
                + "VALUES (?, ?, 'Me Dia', 'test')", creatorUuid, userUuid);

        liveCourse = course(creatorUuid, LIVE_THUMB, LIVE_BANNER);
        // The legacy public-URL form must reduce to the same storage key.
        lostCourse = course(creatorUuid, "/api/v1/files/" + LOST_THUMB, LOST_BANNER);
        course(creatorUuid, EXTERNAL, LIVE_BANNER);

        program = UUID.randomUUID();
        jdbc.update("INSERT INTO training_programs (uuid, title, course_creator_uuid, thumbnail_url, banner_url, "
                + "status, created_by) VALUES (?, 'Programme', ?, ?, ?, 'draft', 'test')",
                program, creatorUuid, LOST_THUMB, LIVE_PROGRAM_BANNER);

        when(storageService.exists(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0);
            return key.equals(LIVE_THUMB) || key.equals(LIVE_BANNER) || key.equals(LIVE_PROGRAM_BANNER);
        });
        clearInvocations(searchIndexRequests);
    }

    @Test
    @DisplayName("Only references to missing files are nulled, and a second run changes nothing")
    void clearsMissingReferencesOnly() {
        assertThat(sweep.sweep()).isEqualTo(3);

        assertThat(column("courses", "thumbnail_url", liveCourse)).isEqualTo(LIVE_THUMB);
        assertThat(column("courses", "banner_url", liveCourse)).isEqualTo(LIVE_BANNER);
        assertThat(column("courses", "thumbnail_url", lostCourse)).isNull();
        assertThat(column("courses", "banner_url", lostCourse)).isNull();
        assertThat(column("training_programs", "thumbnail_url", program)).isNull();
        assertThat(column("training_programs", "banner_url", program)).isEqualTo(LIVE_PROGRAM_BANNER);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM courses WHERE thumbnail_url = ?",
                Integer.class, EXTERNAL)).isEqualTo(1);

        verify(searchIndexRequests, atLeastOnce()).enqueue("courses", lostCourse);
        verify(searchIndexRequests, atLeastOnce()).enqueueFanOut("programs", "course:" + lostCourse);
        verify(searchIndexRequests).enqueue("programs", program);
        verify(searchIndexRequests, never()).enqueue("courses", liveCourse);

        assertThat(sweep.sweep()).isZero();
    }

    @Test
    @DisplayName("An empty or unmounted volume, where everything looks missing, clears nothing")
    void refusesWhenEverythingLooksMissing() {
        when(storageService.exists(anyString())).thenReturn(false);

        assertThat(sweep.sweep()).isZero();
        assertThat(column("courses", "thumbnail_url", liveCourse)).isEqualTo(LIVE_THUMB);
        assertThat(column("training_programs", "thumbnail_url", program)).isEqualTo(LOST_THUMB);
    }

    private UUID course(UUID creatorUuid, String thumbnail, String banner) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO courses (uuid, name, course_creator_uuid, status, active, thumbnail_url, banner_url, "
                        + "created_by) VALUES (?, ?, ?, 'published', true, ?, ?, 'test')",
                uuid, "Course " + uuid, creatorUuid, thumbnail, banner);
        return uuid;
    }

    private String column(String table, String column, UUID uuid) {
        return jdbc.queryForObject("SELECT " + column + " FROM " + table + " WHERE uuid = ?", String.class, uuid);
    }

}
