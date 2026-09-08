package apps.sarafrika.elimika.course.integration;

import apps.sarafrika.elimika.course.dto.CourseEnrollmentDTO;
import apps.sarafrika.elimika.course.service.CourseEnrollmentService;
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

/**
 * An enrolment records which version of the course it was sold against.
 * <p>
 * Course content is promoted onto the live rows in place, so without this a learner who paid for
 * one syllabus can find a different one the next morning and nothing anywhere says which one they
 * bought. {@code course_version_snapshots} keeps what each version contained; this is the pointer
 * that makes those snapshots reachable from a learner.
 * <p>
 * The null case is a real case, not an absence of one: a course that has never had an edit promoted
 * has no version to name, and an enrolment on it follows the live course because the live course is
 * the only content there has ever been.
 */
@SpringBootTest
@Testcontainers
@DisplayName("Enrolment version pinning")
class EnrolmentVersionPinIntegrationTest {

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

    @Autowired private CourseEnrollmentService enrollmentService;
    @Autowired private JdbcTemplate jdbc;

    @MockBean private JwtDecoder jwtDecoder;

    private UUID versionedCourse;
    private UUID neverEditedCourse;
    private UUID studentUuid;

    @BeforeEach
    void seed() {
        jdbc.execute("TRUNCATE course_enrollments, course_version_snapshots, courses, course_creators, "
                + "students, users RESTART IDENTITY CASCADE");

        UUID creatorUser = user("creator@test.local");
        UUID creator = UUID.randomUUID();
        jdbc.update("INSERT INTO course_creators (uuid, user_uuid, full_name, created_by) "
                + "VALUES (?, ?, 'Ver Sion', 'test')", creator, creatorUser);

        UUID studentUser = user("student@test.local");
        studentUuid = UUID.randomUUID();
        jdbc.update("INSERT INTO students (uuid, user_uuid, created_by) VALUES (?, ?, 'test')",
                studentUuid, studentUser);

        versionedCourse = course("Piano Foundations", creator);
        neverEditedCourse = course("Choir Basics", creator);

        // Two promoted edits: the course is on version 2.
        snapshot(versionedCourse, 1);
        snapshot(versionedCourse, 2);
    }

    @Test
    @DisplayName("A new enrolment is pinned to the course's current version")
    void enrolmentRecordsTheVersionItWasSold() {
        CourseEnrollmentDTO created = enrollmentService.createCourseEnrollment(enrolment(versionedCourse));

        assertThat(created.courseVersion())
                .as("the learner bought version 2, not whatever the course becomes next")
                .isEqualTo(2);

        Integer stored = jdbc.queryForObject(
                "SELECT course_version FROM course_enrollments WHERE uuid = ?", Integer.class, created.uuid());
        assertThat(stored).isEqualTo(2);
    }

    @Test
    @DisplayName("A later promotion does not move an enrolment already sold")
    void promotingAgainLeavesEarlierEnrolmentsAlone() {
        CourseEnrollmentDTO early = enrollmentService.createCourseEnrollment(enrolment(versionedCourse));
        snapshot(versionedCourse, 3);

        Integer stored = jdbc.queryForObject(
                "SELECT course_version FROM course_enrollments WHERE uuid = ?", Integer.class, early.uuid());
        assertThat(stored)
                .as("version 3 is what the catalogue now sells; this learner still holds version 2")
                .isEqualTo(2);
    }

    @Test
    @DisplayName("A course with no promoted version pins nothing, and that is correct")
    void courseWithoutVersionsFollowsLive() {
        CourseEnrollmentDTO created = enrollmentService.createCourseEnrollment(enrolment(neverEditedCourse));

        assertThat(created.courseVersion())
                .as("there is no version to name, so the enrolment follows the live course")
                .isNull();
    }

    // ===== seeding =====

    private CourseEnrollmentDTO enrolment(UUID courseUuid) {
        return new CourseEnrollmentDTO(
                null, studentUuid, courseUuid, null, null, null, null, null, null, null, null, null, null);
    }

    private UUID user(String email) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO users (uuid, user_no, first_name, last_name, email, created_by) "
                        + "VALUES (?, ?, 'Test', 'User', ?, 'test')",
                uuid, String.format("%09d", Math.abs(uuid.hashCode()) % 1000000000), email);
        return uuid;
    }

    private UUID course(String name, UUID creatorUuid) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO courses (uuid, name, course_creator_uuid, status, active, admin_approved, created_by) "
                + "VALUES (?, ?, ?, 'published', true, true, 'test')", uuid, name, creatorUuid);
        return uuid;
    }

    private void snapshot(UUID courseUuid, int version) {
        jdbc.update("INSERT INTO course_version_snapshots (uuid, course_uuid, version_number, snapshot, created_by) "
                        + "VALUES (?, ?, ?, '{\"schema_version\": 3}'::jsonb, 'test')",
                UUID.randomUUID(), courseUuid, version);
    }
}
