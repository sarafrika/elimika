package apps.sarafrika.elimika.shared.storage.integration;

import apps.sarafrika.elimika.shared.storage.internal.MediaReconciliationService;
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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The media a course version points at must outlive the live course that once pointed at it.
 * <p>
 * The orphan sweeper decides what to delete by walking the domain columns that hold a file
 * reference. A course version snapshot is not a domain column — it is JSONB — so a file whose only
 * remaining reference lived inside an approved version looked like an orphan and was deleted. The
 * version survived as a record and rendered as broken images, which is the one failure a version
 * history cannot recover from: the row is intact and the bytes are gone.
 * <p>
 * This is written as an integration test rather than a unit test because the retention query is
 * {@code jsonb_path_query} against a real Postgres. Mocking the JDBC template would assert the
 * string I wrote rather than the behaviour it produces.
 */
// The container dies with this class; a cached context must not outlive it.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest
@Testcontainers
@DisplayName("Snapshot media retention")
class SnapshotMediaRetentionIntegrationTest {

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
    }

    /** Referenced only by the snapshot. Deleting this is the bug. */
    private static final String ARCHIVED_LESSON_VIDEO = "courses/lesson-one-v1.mp4";
    /** Referenced only by the snapshot, on the course rather than a lesson. */
    private static final String ARCHIVED_THUMBNAIL = "courses/thumb-v1.png";
    /** Referenced by nothing at all. Deleting this is the sweeper working. */
    private static final String GENUINE_ORPHAN = "courses/nobody-references-this.png";

    @Autowired private MediaReconciliationService reconciliation;
    @Autowired private JdbcTemplate jdbc;

    @MockBean private StorageService storageService;
    @MockBean private JwtDecoder jwtDecoder;

    @BeforeEach
    void seed() {
        jdbc.execute("TRUNCATE course_version_snapshots, courses, course_creators, users RESTART IDENTITY CASCADE");

        UUID userUuid = UUID.randomUUID();
        jdbc.update("INSERT INTO users (uuid, user_no, first_name, last_name, email, created_by) "
                + "VALUES (?, ?, 'Ver', 'Sion', 'versions@test.local', 'test')",
                userUuid, String.format("%09d", Math.abs(userUuid.hashCode()) % 1000000000));
        UUID creatorUuid = UUID.randomUUID();
        jdbc.update("INSERT INTO course_creators (uuid, user_uuid, full_name, created_by) "
                + "VALUES (?, ?, 'Ver Sion', 'test')", creatorUuid, userUuid);

        UUID courseUuid = UUID.randomUUID();
        // The live course has moved on: it points at neither archived file any more.
        jdbc.update("INSERT INTO courses (uuid, name, course_creator_uuid, status, active, "
                + "thumbnail_url, created_by) "
                + "VALUES (?, 'Piano Foundations', ?, 'published', true, 'courses/thumb-v2.png', 'test')",
                courseUuid, creatorUuid);

        // Version 1 still points at both, the video nested two levels down inside a lesson.
        String snapshot = """
                {
                  "schema_version": 3,
                  "course": { "thumbnail_url": "%s", "banner_url": null, "intro_video_url": null },
                  "lessons": [
                    { "title": "Lesson one",
                      "content": [ { "title": "Posture", "file_url": "%s" } ],
                      "assignments": [] }
                  ]
                }
                """.formatted(ARCHIVED_THUMBNAIL, ARCHIVED_LESSON_VIDEO);

        jdbc.update("INSERT INTO course_version_snapshots (uuid, course_uuid, version_number, snapshot, created_by) "
                + "VALUES (?, ?, 1, ?::jsonb, 'test')", UUID.randomUUID(), courseUuid, snapshot);
    }

    @Test
    @DisplayName("A file only an old version still points at is not swept")
    void snapshotMediaSurvivesTheSweep() {
        when(storageService.listAllKeys()).thenReturn(
                List.of(ARCHIVED_LESSON_VIDEO, ARCHIVED_THUMBNAIL, "courses/thumb-v2.png", GENUINE_ORPHAN));

        MediaReconciliationService.SweepReport report = reconciliation.sweep(true);

        assertThat(report.orphanKeys())
                .as("only the file nothing references at all is an orphan")
                .containsExactly(GENUINE_ORPHAN);

        verify(storageService, never()).delete(ARCHIVED_LESSON_VIDEO);
        verify(storageService, never()).delete(ARCHIVED_THUMBNAIL);
        verify(storageService, times(1)).delete(GENUINE_ORPHAN);
    }

    @Test
    @DisplayName("With no snapshots, the sweeper still deletes what nothing references")
    void controlCaseStillSweeps() {
        jdbc.execute("DELETE FROM course_version_snapshots");
        when(storageService.listAllKeys()).thenReturn(List.of(ARCHIVED_LESSON_VIDEO, GENUINE_ORPHAN));

        MediaReconciliationService.SweepReport report = reconciliation.sweep(true);

        assertThat(report.orphanKeys())
                .as("without the snapshot the archived video really is unreferenced")
                .containsExactlyInAnyOrder(ARCHIVED_LESSON_VIDEO, GENUINE_ORPHAN);
        verify(storageService, times(2)).delete(anyString());
    }
}
