package apps.sarafrika.elimika.course.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Proves that {@code GET /courses/{uuid}/stats} hands each caller only the blocks they have earned,
 * over real HTTP, against a real database, through the real security filter chain.
 * <p>
 * The point of this test is what is <em>missing</em> from a response. A unit test on the service can
 * assert a null field; only serialising the thing shows whether the key reaches the wire. An
 * approved trainer must not receive an {@code owner} key at all — not an owner block of zeros, and
 * not one the client is trusted to ignore — because a key that is transmitted has already leaked
 * regardless of what any client does with it.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("Course statistics access (end-to-end)")
class CourseStatsAccessIntegrationTest {

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

    private static final String CREATOR_SUBJECT = "keycloak-stats-creator";
    private static final String APPROVED_SUBJECT = "keycloak-stats-approved-instructor";
    private static final String PENDING_SUBJECT = "keycloak-stats-pending-instructor";
    private static final String OUTSIDER_SUBJECT = "keycloak-stats-outsider";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    /** Never invoked: the jwt() post-processor sets the SecurityContext directly. Mocked only so
     *  the resource server does not fetch Keycloak's JWKS at boot. */
    @MockBean private JwtDecoder jwtDecoder;

    private UUID courseUuid;

    @BeforeEach
    void seed() {
        clean();

        UUID creatorUserUuid = user(CREATOR_SUBJECT, "stats-creator@test.local");
        grantDomain(creatorUserUuid, "course_creator");
        UUID courseCreatorUuid = courseCreator(creatorUserUuid);
        courseUuid = course("Statistics course", courseCreatorUuid);

        UUID approvedUserUuid = user(APPROVED_SUBJECT, "stats-approved@test.local");
        grantDomain(approvedUserUuid, "instructor");
        UUID approvedInstructorUuid = instructor(approvedUserUuid, "Approved Trainer");
        trainingApplication(courseUuid, approvedInstructorUuid, "approved");

        // One class still running with two of its ten seats taken, and one closed class that trained
        // a third learner. Learners trained counts all three; classes running and seat fill see only
        // the open class.
        UUID runningClass = classDefinition(courseUuid, approvedInstructorUuid, "Running class", 10, true);
        UUID closedClass = classDefinition(courseUuid, approvedInstructorUuid, "Finished class", 10, false);
        enrol(session(runningClass, approvedInstructorUuid), learner("Learner One", "learner-one"));
        enrol(session(runningClass, approvedInstructorUuid), learner("Learner Two", "learner-two"));
        enrol(session(closedClass, approvedInstructorUuid), learner("Learner Three", "learner-three"));

        UUID pendingUserUuid = user(PENDING_SUBJECT, "stats-pending@test.local");
        grantDomain(pendingUserUuid, "instructor");
        trainingApplication(courseUuid, instructor(pendingUserUuid, "Hopeful Trainer"), "pending");

        UUID outsiderUserUuid = user(OUTSIDER_SUBJECT, "stats-outsider@test.local");
        grantDomain(outsiderUserUuid, "student");
    }

    // ===== THE BLOCK AN APPROVED TRAINER MUST NOT RECEIVE =====

    @Test
    @DisplayName("An approved instructor gets public and scoped, and no 'owner' key at all")
    void approvedInstructorNeverSeesTheOwnerBlock() throws Exception {
        mockMvc.perform(get(statsUrl()).with(jwt(APPROVED_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.public").exists())
                .andExpect(jsonPath("$.data.scoped.your_classes").value(1))
                .andExpect(jsonPath("$.data.scoped.your_learners").value(3))
                .andExpect(jsonPath("$.data.owner").doesNotExist());
    }

    @Test
    @DisplayName("A pending application unlocks nothing beyond the public block")
    void pendingApplicantGetsOnlyThePublicBlock() throws Exception {
        mockMvc.perform(get(statsUrl()).with(jwt(PENDING_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.public").exists())
                .andExpect(jsonPath("$.data.scoped").doesNotExist())
                .andExpect(jsonPath("$.data.owner").doesNotExist());
    }

    @Test
    @DisplayName("An unrelated signed-in caller gets the public block and nothing else")
    void outsiderGetsOnlyThePublicBlock() throws Exception {
        mockMvc.perform(get(statsUrl()).with(jwt(OUTSIDER_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.public").exists())
                .andExpect(jsonPath("$.data.scoped").doesNotExist())
                .andExpect(jsonPath("$.data.owner").doesNotExist());
    }

    @Test
    @DisplayName("The course creator gets the commercial block")
    void creatorSeesTheOwnerBlock() throws Exception {
        mockMvc.perform(get(statsUrl()).with(jwt(CREATOR_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.public").exists())
                .andExpect(jsonPath("$.data.owner").exists())
                .andExpect(jsonPath("$.data.owner.gross_sales").exists())
                .andExpect(jsonPath("$.data.owner.platform_fee").exists());
    }

    // ===== THE FIGURES BEHIND THE PUBLIC RATIO STAY BEHIND IT =====

    @Test
    @DisplayName("Seat fill is published as a percentage; the seat counts behind it are not")
    void seatCountsAreNeverPublished() throws Exception {
        mockMvc.perform(get(statsUrl()).with(jwt(OUTSIDER_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.public.average_class_fill").exists())
                .andExpect(jsonPath("$.data.public.filled_seats").doesNotExist())
                .andExpect(jsonPath("$.data.public.total_seats").doesNotExist())
                .andExpect(jsonPath("$.data.public.max_participants").doesNotExist());
    }

    @Test
    @DisplayName("Learners trained spans closed classes; classes running and fill see only open ones")
    void publicFiguresSplitClosedClassesFromRunningOnes() throws Exception {
        mockMvc.perform(get(statsUrl()).with(jwt(OUTSIDER_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.public.learners_trained").value(3))
                .andExpect(jsonPath("$.data.public.classes_running").value(1))
                // Two of the running class's ten seats: 20%, already on a five-point boundary.
                .andExpect(jsonPath("$.data.public.average_class_fill").value(20))
                .andExpect(jsonPath("$.data.public.approved_trainer_count").value(1));
    }

    @Test
    @DisplayName("An anonymous caller is refused: the route is authenticated")
    void anonymousCallerIsRefused() throws Exception {
        mockMvc.perform(get(statsUrl()))
                .andExpect(status().isUnauthorized());
    }

    // ===== TEST PLUMBING =====

    private String statsUrl() {
        return "/api/v1/courses/" + courseUuid + "/stats";
    }

    private RequestPostProcessor jwt(String subject) {
        return org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                .jwt().jwt(builder -> builder.subject(subject).claim("sub", subject));
    }

    private void clean() {
        jdbc.execute("TRUNCATE class_enrollments, scheduled_instances, course_training_applications, "
                + "class_definitions, courses, course_creators, instructors, students, "
                + "user_domain_mapping, users RESTART IDENTITY CASCADE");
    }

    private UUID user(String keycloakId, String email) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO users (uuid, user_no, first_name, last_name, email, keycloak_id, created_by) "
                + "VALUES (?, ?, 'Test', 'User', ?, ?, 'test')",
                uuid, String.format("%09d", Math.abs(uuid.hashCode()) % 1000000000), email, keycloakId);
        return uuid;
    }

    private void grantDomain(UUID userUuid, String domainName) {
        jdbc.update("INSERT INTO user_domain_mapping (user_uuid, domain_uuid) "
                + "VALUES (?, (SELECT uuid FROM user_domain WHERE domain_name = ?))", userUuid, domainName);
    }

    private UUID courseCreator(UUID userUuid) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO course_creators (uuid, user_uuid, full_name, created_by) "
                + "VALUES (?, ?, 'Course Creator', 'test')", uuid, userUuid);
        return uuid;
    }

    private UUID instructor(UUID userUuid, String fullName) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO instructors (uuid, user_uuid, full_name, admin_verified, created_by) "
                + "VALUES (?, ?, ?, true, 'test')", uuid, userUuid, fullName);
        return uuid;
    }

    private UUID course(String name, UUID courseCreatorUuid) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO courses (uuid, name, course_creator_uuid, status, active, created_by) "
                + "VALUES (?, ?, ?, 'published', true, 'test')", uuid, name, courseCreatorUuid);
        return uuid;
    }

    private UUID classDefinition(UUID courseUuid, UUID instructorUuid, String title, int seats, boolean active) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO class_definitions (uuid, title, default_instructor_uuid, course_uuid, "
                + "default_start_time, default_end_time, location_type, class_visibility, session_format, "
                + "max_participants, is_active, created_by) "
                + "VALUES (?, ?, ?, ?, TIMESTAMPTZ '2026-01-05 09:00:00+00', "
                + "TIMESTAMPTZ '2026-01-05 10:30:00+00', 'ONLINE', 'PUBLIC', 'GROUP', ?, ?, 'test')",
                uuid, title, instructorUuid, courseUuid, seats, active);
        return uuid;
    }

    private UUID session(UUID classDefinitionUuid, UUID instructorUuid) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO scheduled_instances (uuid, class_definition_uuid, instructor_uuid, "
                + "start_time, end_time, title, location_type, max_participants, status, created_by) "
                + "VALUES (?, ?, ?, NOW(), NOW() + INTERVAL '1 hour', 'Session', 'ONLINE', 10, "
                + "'SCHEDULED', 'test')",
                uuid, classDefinitionUuid, instructorUuid);
        return uuid;
    }

    private UUID learner(String fullName, String keycloakSuffix) {
        UUID userUuid = user("keycloak-stats-" + keycloakSuffix, keycloakSuffix + "@test.local");
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO students (uuid, user_uuid, full_name, created_by) VALUES (?, ?, ?, 'test')",
                uuid, userUuid, fullName);
        return uuid;
    }

    private void enrol(UUID scheduledInstanceUuid, UUID studentUuid) {
        jdbc.update("INSERT INTO class_enrollments (uuid, scheduled_instance_uuid, student_uuid, status, created_by) "
                + "VALUES (?, ?, ?, 'ENROLLED', 'test')",
                UUID.randomUUID(), scheduledInstanceUuid, studentUuid);
    }

    /**
     * Seeds an application the way the converters write one: the applicant type and status columns
     * hold the enum's lower-case wire value, not its Java constant name.
     */
    private void trainingApplication(UUID courseUuid, UUID instructorUuid, String status) {
        jdbc.update("INSERT INTO course_training_applications (uuid, course_uuid, applicant_type, applicant_uuid, "
                + "status, rate_currency, private_online_hourly_rate, private_inperson_hourly_rate, "
                + "group_online_hourly_rate, group_inperson_hourly_rate, created_by) "
                + "VALUES (?, ?, 'instructor', ?, ?, 'KES', 1000, 1000, 1000, 1000, 'test')",
                UUID.randomUUID(), courseUuid, instructorUuid, status);
    }
}
