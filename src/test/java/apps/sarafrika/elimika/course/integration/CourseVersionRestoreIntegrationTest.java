package apps.sarafrika.elimika.course.integration;

import apps.sarafrika.elimika.course.model.Course;
import apps.sarafrika.elimika.course.service.CourseDraftService;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Putting an old version of a course back.
 * <p>
 * The load-bearing behaviour is that restore lands in the <em>draft</em> and leaves the live course
 * alone. A creator restoring version 1 has not decided to ship version 1 — they have decided to look
 * at it. Writing straight to live would skip review and change what enrolled learners are reading
 * mid-course, on one click.
 * <p>
 * The second thing worth proving is the link back to live. A restored lesson that still exists on
 * the live course carries {@code source_lesson_uuid}, so promotion updates that row in place and the
 * progress recorded against it survives; a lesson deleted since the snapshot has no live row to
 * point at and comes back as an addition. Getting this backwards would orphan learner progress on
 * every restore.
 */
@SpringBootTest
@Testcontainers
@DisplayName("Course version restore")
class CourseVersionRestoreIntegrationTest {

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

    private static final String ORIGINAL_TITLE = "Sitting at the instrument";
    private static final String EDITED_TITLE = "Posture and hand position";
    private static final String DELETED_LESSON_TITLE = "Pedalling";

    @Autowired private CourseDraftService draftService;
    @Autowired private JdbcTemplate jdbc;

    @MockBean private JwtDecoder jwtDecoder;

    private UUID courseUuid;
    private UUID lessonOne;
    private UUID lessonTwo;

    @BeforeEach
    void seed() {
        jdbc.execute("TRUNCATE course_version_snapshots, courses, course_creators, users RESTART IDENTITY CASCADE");

        UUID creatorUser = UUID.randomUUID();
        jdbc.update("INSERT INTO users (uuid, user_no, first_name, last_name, email, created_by) "
                        + "VALUES (?, ?, 'Ver', 'Sion', 'restore@test.local', 'test')",
                creatorUser, String.format("%09d", Math.abs(creatorUser.hashCode()) % 1000000000));
        UUID creator = UUID.randomUUID();
        jdbc.update("INSERT INTO course_creators (uuid, user_uuid, full_name, created_by) "
                + "VALUES (?, ?, 'Ver Sion', 'test')", creator, creatorUser);

        courseUuid = UUID.randomUUID();
        jdbc.update("INSERT INTO courses (uuid, name, course_creator_uuid, status, active, admin_approved, created_by) "
                + "VALUES (?, 'Piano Foundations', ?, 'published', true, true, 'test')", courseUuid, creator);

        lessonOne = lesson(1, ORIGINAL_TITLE);
        lessonTwo = lesson(2, DELETED_LESSON_TITLE);

        UUID contentType = jdbc.queryForObject(
                "SELECT uuid FROM lesson_content_types LIMIT 1", UUID.class);
        jdbc.update("INSERT INTO lesson_contents (uuid, lesson_uuid, content_type_uuid, title, display_order, created_by) "
                + "VALUES (?, ?, ?, 'Where the wrists sit', 1, 'test')",
                UUID.randomUUID(), lessonOne, contentType);

        UUID quiz = UUID.randomUUID();
        jdbc.update("INSERT INTO quizzes (uuid, lesson_uuid, title, status, active, created_by) "
                + "VALUES (?, ?, 'Check yourself', 'published', true, 'test')", quiz, lessonOne);
        UUID question = UUID.randomUUID();
        jdbc.update("INSERT INTO quiz_questions (uuid, quiz_uuid, question_text, question_type, points, display_order, created_by) "
                + "VALUES (?, ?, 'Where do the wrists sit?', 'multiple_choice', 2.00, 1, 'test')", question, quiz);
        jdbc.update("INSERT INTO quiz_question_options (uuid, question_uuid, option_text, is_correct, display_order, created_by) "
                + "VALUES (?, ?, 'Level with the keys', true, 1, 'test')", UUID.randomUUID(), question);
        jdbc.update("INSERT INTO quiz_question_options (uuid, question_uuid, option_text, is_correct, display_order, created_by) "
                + "VALUES (?, ?, 'Below the keybed', false, 2, 'test')", UUID.randomUUID(), question);

        // Version 1 is the course exactly as seeded.
        JsonNode tree = draftService.snapshotTree(courseUuid);
        jdbc.update("INSERT INTO course_version_snapshots (uuid, course_uuid, version_number, snapshot, created_by) "
                + "VALUES (?, ?, 1, ?::jsonb, 'test')", UUID.randomUUID(), courseUuid, tree.toString());

        // The live course then moves on: lesson one is retitled, lesson two is deleted outright.
        jdbc.update("UPDATE lessons SET title = ? WHERE uuid = ?", EDITED_TITLE, lessonOne);
        jdbc.update("DELETE FROM lessons WHERE uuid = ?", lessonTwo);
    }

    @Test
    @DisplayName("Restoring puts the old content in the draft and leaves the live course alone")
    void restoreLandsInTheDraftOnly() {
        Course draft = draftService.restore(courseUuid, 1);

        assertThat(draft.getParentCourseUuid())
                .as("a restore produces a draft, not a new live course")
                .isEqualTo(courseUuid);

        String draftTitle = jdbc.queryForObject(
                "SELECT title FROM lessons WHERE course_uuid = ? AND lesson_number = 1", String.class, draft.getUuid());
        assertThat(draftTitle).isEqualTo(ORIGINAL_TITLE);

        String liveTitle = jdbc.queryForObject(
                "SELECT title FROM lessons WHERE uuid = ?", String.class, lessonOne);
        assertThat(liveTitle)
                .as("the live course keeps serving what it served before the restore")
                .isEqualTo(EDITED_TITLE);
    }

    @Test
    @DisplayName("A restored lesson still on the live course is linked back to it")
    void survivingLessonKeepsItsLiveLink() {
        Course draft = draftService.restore(courseUuid, 1);

        UUID source = jdbc.queryForObject(
                "SELECT source_lesson_uuid FROM lessons WHERE course_uuid = ? AND lesson_number = 1",
                UUID.class, draft.getUuid());
        assertThat(source)
                .as("promotion must update the live lesson in place so progress against it survives")
                .isEqualTo(lessonOne);
    }

    @Test
    @DisplayName("A lesson deleted since the snapshot comes back as an addition")
    void deletedLessonReturnsUnlinked() {
        Course draft = draftService.restore(courseUuid, 1);

        String title = jdbc.queryForObject(
                "SELECT title FROM lessons WHERE course_uuid = ? AND lesson_number = 2",
                String.class, draft.getUuid());
        assertThat(title).isEqualTo(DELETED_LESSON_TITLE);

        UUID source = jdbc.queryForObject(
                "SELECT source_lesson_uuid FROM lessons WHERE course_uuid = ? AND lesson_number = 2",
                UUID.class, draft.getUuid());
        assertThat(source)
                .as("there is no live row left to promote onto, so this is new content")
                .isNull();
    }

    @Test
    @DisplayName("The answer key survives the round trip")
    void quizAnswersAreRestored() {
        Course draft = draftService.restore(courseUuid, 1);

        Boolean correct = jdbc.queryForObject("""
                SELECT o.is_correct FROM quiz_question_options o
                JOIN quiz_questions q ON q.uuid = o.question_uuid
                JOIN quizzes z ON z.uuid = q.quiz_uuid
                JOIN lessons l ON l.uuid = z.lesson_uuid
                WHERE l.course_uuid = ? AND o.option_text = 'Level with the keys'
                """, Boolean.class, draft.getUuid());

        assertThat(correct)
                .as("a restored quiz that forgot which answer was right would be worse than no restore")
                .isTrue();
    }

    @Test
    @DisplayName("Restoring over an open edit is refused, not silently applied")
    void refusesToClobberAnOpenEdit() {
        draftService.restore(courseUuid, 1);

        assertThatThrownBy(() -> draftService.restore(courseUuid, 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already has an open edit");
    }

    private UUID lesson(int number, String title) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO lessons (uuid, course_uuid, lesson_number, title, status, active, created_by) "
                + "VALUES (?, ?, ?, ?, 'published', true, 'test')", uuid, courseUuid, number, title);
        return uuid;
    }
}
