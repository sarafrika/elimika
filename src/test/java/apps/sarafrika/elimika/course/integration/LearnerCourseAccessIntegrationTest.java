package apps.sarafrika.elimika.course.integration;

import apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the learner-facing endpoints over real HTTP, against a real database, through the real
 * security filter chain.
 * <p>
 * Unit tests cannot cover this: {@code @PreAuthorize} expressions are SpEL strings resolved against
 * live beans at request time, so a typo in a bean name or a parameter reference compiles, passes
 * every unit test, and fails only in production. Standalone MockMvc does not evaluate them either.
 * This test is the only place the annotations themselves are proven.
 * <p>
 * The scenario mirrors the one observed on staging: a student enrolled in one course, with a second
 * course they are not enrolled in, published and draft material in each.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("Enrolled learner course access (end-to-end)")
class LearnerCourseAccessIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        // The resource server would otherwise try to fetch Keycloak's JWKS at boot.
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> "http://localhost/realms/test");
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", () -> "http://localhost/realms/test/certs");
        // Deployment-supplied placeholders the app declares with no default.
        registry.add("MAIL_SERVER", () -> "localhost");
        registry.add("MAIL_USERNAME", () -> "test");
        registry.add("MAIL_PASSWORD", () -> "test");
        registry.add("app.keycloak.admin.clientId", () -> "test-admin");
        registry.add("app.keycloak.admin.clientSecret", () -> "test-secret");
        registry.add("encryption.secret-key", () -> "0123456789abcdef0123456789abcdef");
        registry.add("encryption.salt", () -> "0123456789abcdef");
        // Flyway builds the real schema. Hibernate's validator additionally sees @Entity classes
        // declared inside other test classes, which have no table, so leave validation to Flyway.
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    private static final String STUDENT_SUBJECT = "keycloak-student";
    private static final String OUTSIDER_SUBJECT = "keycloak-outsider";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    @MockBean private ClassDefinitionLookupService classDefinitionLookupService;
    /** Never invoked: the jwt() post-processor sets the SecurityContext directly. Mocked only so
     *  the resource server does not fetch Keycloak's JWKS at boot. */
    @MockBean private JwtDecoder jwtDecoder;

    private UUID enrolledCourseUuid;
    private UUID foreignCourseUuid;
    private UUID publishedQuizUuid;
    private UUID draftQuizUuid;
    private UUID foreignQuizUuid;
    private UUID publishedAssignmentUuid;
    private UUID unpublishedAssignmentUuid;
    private UUID publishedLessonUuid;
    private UUID draftLessonUuid;

    @BeforeEach
    void seed() {
        clean();

        UUID studentUserUuid = user(STUDENT_SUBJECT, "student@test.local");
        user(OUTSIDER_SUBJECT, "outsider@test.local");
        grantDomain(studentUserUuid, "student");
        grantDomain(userUuidBySubject(OUTSIDER_SUBJECT), "student");

        UUID studentUuid = student(studentUserUuid, "Enrolled Learner");
        student(userUuidBySubject(OUTSIDER_SUBJECT), "Unenrolled Learner");

        UUID creatorUuid = courseCreator(user("keycloak-creator", "creator@test.local"));
        enrolledCourseUuid = course("Enrolled course", creatorUuid);
        foreignCourseUuid = course("Someone else's course", creatorUuid);
        enroll(studentUuid, enrolledCourseUuid, "active");

        publishedLessonUuid = lesson(enrolledCourseUuid, 1, "Published lesson", "published", true);
        draftLessonUuid = lesson(enrolledCourseUuid, 2, "Draft lesson", "draft", false);
        UUID foreignLessonUuid = lesson(foreignCourseUuid, 1, "Foreign lesson", "published", true);

        publishedQuizUuid = quiz(publishedLessonUuid, "Published quiz", "published", true);
        draftQuizUuid = quiz(draftLessonUuid, "Draft quiz", "draft", false);
        foreignQuizUuid = quiz(foreignLessonUuid, "Foreign quiz", "published", true);

        publishedAssignmentUuid = assignment(publishedLessonUuid, "Published assignment", true);
        unpublishedAssignmentUuid = assignment(publishedLessonUuid, "Unpublished assignment", false);
    }

    // ===== THE ENDPOINTS THAT WERE 403-ING ON STAGING =====

    @Test
    @DisplayName("GET /quizzes/{uuid} — a learner reads a published quiz on their own course")
    void learnerReadsQuizOnEnrolledCourse() throws Exception {
        mockMvc.perform(get("/api/v1/quizzes/" + publishedQuizUuid).with(student()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Published quiz"));
    }

    @Test
    @DisplayName("GET /quizzes/{uuid} — refused for a course the learner is not enrolled in")
    void learnerCannotReadQuizOnForeignCourse() throws Exception {
        mockMvc.perform(get("/api/v1/quizzes/" + foreignQuizUuid).with(student()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /quizzes/{uuid} — refused for a draft quiz even on an enrolled course")
    void learnerCannotReadDraftQuiz() throws Exception {
        mockMvc.perform(get("/api/v1/quizzes/" + draftQuizUuid).with(student()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /assignments/{uuid} — published is readable, unpublished is not")
    void learnerReadsOnlyPublishedAssignments() throws Exception {
        mockMvc.perform(get("/api/v1/assignments/" + publishedAssignmentUuid).with(student()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Published assignment"));

        mockMvc.perform(get("/api/v1/assignments/" + unpublishedAssignmentUuid).with(student()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /assignments/{uuid}/submissions — reachable, and empty rather than everyone's")
    void learnerReadsSubmissionListWithoutSeeingClassmates() throws Exception {
        mockMvc.perform(get("/api/v1/assignments/" + publishedAssignmentUuid + "/submissions").with(student()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("GET /quizzes/{uuid}/attempts — reachable by a learner")
    void learnerReadsOwnQuizAttempts() throws Exception {
        mockMvc.perform(get("/api/v1/quizzes/" + publishedQuizUuid + "/attempts").with(student()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /quizzes/search — reachable, and confined to the learner's own courses")
    void quizSearchIsConfinedToEnrolledCourses() throws Exception {
        mockMvc.perform(get("/api/v1/quizzes/search").with(student()))
                .andExpect(status().isOk())
                // Only the published quiz on the enrolled course: not the draft, not the foreign one.
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("Published quiz"));
    }

    @Test
    @DisplayName("GET /assignments/search — reachable, and confined to published material")
    void assignmentSearchIsConfinedToPublishedMaterial() throws Exception {
        mockMvc.perform(get("/api/v1/assignments/search").with(student()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("Published assignment"));
    }

    @Test
    @DisplayName("GET /assignments/submissions/search and /quizzes/attempts/search — reachable")
    void assessmentSearchesAreReachable() throws Exception {
        mockMvc.perform(get("/api/v1/assignments/submissions/search").with(student()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/quizzes/attempts/search").with(student()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /courses/{uuid}/lessons — drafts are no longer advertised to learners")
    void lessonListingHidesDrafts() throws Exception {
        mockMvc.perform(get("/api/v1/courses/" + enrolledCourseUuid + "/lessons").with(student()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("Published lesson"));
    }

    @Test
    @DisplayName("GET /courses/{uuid}/lessons/{uuid}/content — published lesson readable, draft is not")
    void lessonContentFollowsThePublishState() throws Exception {
        mockMvc.perform(get("/api/v1/courses/" + enrolledCourseUuid + "/lessons/" + publishedLessonUuid + "/content")
                        .with(student()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/courses/" + enrolledCourseUuid + "/lessons/" + draftLessonUuid + "/content")
                        .with(student()))
                .andExpect(status().isForbidden());
    }

    // ===== THE OTHER DIRECTION: A LEARNER ENROLLED IN NOTHING SEES NOTHING =====

    @Test
    @DisplayName("A student with no enrolments is refused, and their searches return empty")
    void anUnenrolledStudentSeesNothing() throws Exception {
        mockMvc.perform(get("/api/v1/quizzes/" + publishedQuizUuid).with(outsider()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/quizzes/search").with(outsider()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(0));

        mockMvc.perform(get("/api/v1/courses/" + enrolledCourseUuid + "/lessons").with(outsider()))
                .andExpect(status().isForbidden());
    }

    // ===== TEST PLUMBING =====

    private org.springframework.test.web.servlet.request.RequestPostProcessor student() {
        return jwt(STUDENT_SUBJECT);
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor outsider() {
        return jwt(OUTSIDER_SUBJECT);
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor jwt(String subject) {
        return org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                .jwt().jwt(builder -> builder.subject(subject).claim("sub", subject));
    }

    private void clean() {
        jdbc.execute("TRUNCATE assignment_submissions, quiz_attempts, assignments, quizzes, lessons, "
                + "course_enrollments, courses, course_creators, students, user_domain_mapping, users "
                + "RESTART IDENTITY CASCADE");
    }

    private UUID user(String keycloakId, String email) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO users (uuid, user_no, first_name, last_name, email, keycloak_id, created_by) "
                + "VALUES (?, ?, 'Test', 'User', ?, ?, 'test')",
                uuid, String.format("%09d", Math.abs(uuid.hashCode()) % 1000000000), email, keycloakId);
        return uuid;
    }

    private UUID userUuidBySubject(String keycloakId) {
        return jdbc.queryForObject("SELECT uuid FROM users WHERE keycloak_id = ?", UUID.class, keycloakId);
    }

    private void grantDomain(UUID userUuid, String domainName) {
        jdbc.update("INSERT INTO user_domain_mapping (user_uuid, domain_uuid) "
                + "VALUES (?, (SELECT uuid FROM user_domain WHERE domain_name = ?))", userUuid, domainName);
    }

    private UUID student(UUID userUuid, String fullName) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO students (uuid, user_uuid, full_name, created_by) VALUES (?, ?, ?, 'test')",
                uuid, userUuid, fullName);
        return uuid;
    }

    private UUID courseCreator(UUID userUuid) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO course_creators (uuid, user_uuid, full_name, created_by) "
                + "VALUES (?, ?, 'Course Creator', 'test')", uuid, userUuid);
        return uuid;
    }

    private UUID course(String name, UUID courseCreatorUuid) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO courses (uuid, name, course_creator_uuid, status, active, created_by) "
                + "VALUES (?, ?, ?, 'published', true, 'test')", uuid, name, courseCreatorUuid);
        return uuid;
    }

    private void enroll(UUID studentUuid, UUID courseUuid, String status) {
        jdbc.update("INSERT INTO course_enrollments (uuid, student_uuid, course_uuid, status, enrollment_date, created_by) "
                + "VALUES (?, ?, ?, ?::varchar, CURRENT_TIMESTAMP, 'test')",
                UUID.randomUUID(), studentUuid, courseUuid, status);
    }

    private UUID lesson(UUID courseUuid, int number, String title, String status, boolean active) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO lessons (uuid, course_uuid, lesson_number, title, status, active, created_by) "
                + "VALUES (?, ?, ?, ?, ?::varchar, ?, 'test')", uuid, courseUuid, number, title, status, active);
        return uuid;
    }

    private UUID quiz(UUID lessonUuid, String title, String status, boolean active) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO quizzes (uuid, lesson_uuid, title, attempts_allowed, passing_score, status, active, created_by) "
                + "VALUES (?, ?, ?, 3, 50.00, ?::varchar, ?, 'test')", uuid, lessonUuid, title, status, active);
        return uuid;
    }

    private UUID assignment(UUID lessonUuid, String title, boolean published) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO assignments (uuid, lesson_uuid, title, is_published, created_by) "
                + "VALUES (?, ?, ?, ?, 'test')", uuid, lessonUuid, title, published);
        return uuid;
    }
}
