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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Proves the course catalogue endpoints hand each caller only the courses they may see: the public
 * catalogue for everyone, unpublished work to its author and the people tied to it, shadow drafts to
 * nobody but their author and platform admins.
 */
// The container dies with this class; a cached context must not outlive it.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("Course catalogue visibility (end-to-end)")
class CourseCatalogueVisibilityIntegrationTest {

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

    private static final String CREATOR_SUBJECT = "keycloak-visibility-creator";
    private static final String TRAINER_SUBJECT = "keycloak-visibility-trainer";
    private static final String OUTSIDER_SUBJECT = "keycloak-visibility-outsider";
    private static final String ADMIN_SUBJECT = "keycloak-visibility-admin";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    @MockBean private JwtDecoder jwtDecoder;

    private UUID liveCourse;
    private UUID draftCourse;
    private UUID inReviewCourse;
    private UUID unapprovedCourse;
    private UUID archivedCourse;
    private UUID shadowDraft;
    private UUID courseCreatorUuid;
    private UUID trainerInstructorUuid;



    @BeforeEach
    void seed() {
        jdbc.execute("TRUNCATE course_training_applications, courses, course_creators, instructors, students, "
                + "user_domain_mapping, users RESTART IDENTITY CASCADE");

        UUID creatorUserUuid = user(CREATOR_SUBJECT, "visibility-creator@test.local");
        grantDomain(creatorUserUuid, "course_creator");
        courseCreatorUuid = courseCreator(creatorUserUuid);

        liveCourse = course("Live", "published", true, true, null);
        draftCourse = course("Draft", "draft", false, false, null);
        inReviewCourse = course("In review", "in_review", false, false, null);
        unapprovedCourse = course("Published, unapproved", "published", true, false, null);
        archivedCourse = course("Archived", "archived", false, true, null);
        shadowDraft = course("Live (edit)", "draft", false, false, liveCourse);

        UUID trainerUserUuid = user(TRAINER_SUBJECT, "visibility-trainer@test.local");
        grantDomain(trainerUserUuid, "instructor");
        trainerInstructorUuid = instructor(trainerUserUuid);
        trainingApplication(draftCourse, trainerInstructorUuid);
        trainingApplication(liveCourse, trainerInstructorUuid);

        UUID outsiderUserUuid = user(OUTSIDER_SUBJECT, "visibility-outsider@test.local");
        grantDomain(outsiderUserUuid, "student");

        grantDomain(user(ADMIN_SUBJECT, "visibility-admin@test.local"), "admin");
    }

    // ===== LISTINGS =====

    @Test
    @DisplayName("An unrelated caller's search sees only the public catalogue")
    void outsiderSearchSeesOnlyTheCatalogue() throws Exception {
        mockMvc.perform(get("/api/v1/courses/search").with(jwt(OUTSIDER_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].uuid").value(containsInAnyOrder(liveCourse.toString())));
    }

    @Test
    @DisplayName("An explicit status filter cannot widen an unrelated caller's view")
    void outsiderCannotFilterIntoDrafts() throws Exception {
        mockMvc.perform(get("/api/v1/courses/search").param("status", "DRAFT").with(jwt(OUTSIDER_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(0));
    }

    @Test
    @DisplayName("The author lists all their own courses, but never the shadow draft")
    void authorSeesOwnWorkButNotShadowDraft() throws Exception {
        mockMvc.perform(get("/api/v1/courses/search")
                        .param("course_creator_uuid_eq", courseCreatorUuid.toString())
                        .with(jwt(CREATOR_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].uuid").value(containsInAnyOrder(
                        liveCourse.toString(), draftCourse.toString(), inReviewCourse.toString(),
                        unapprovedCourse.toString(), archivedCourse.toString())));
    }

    @Test
    @DisplayName("A trainer approved on an unpublished course still finds it")
    void approvedTrainerSeesTheirUnpublishedCourse() throws Exception {
        mockMvc.perform(get("/api/v1/courses").with(jwt(TRAINER_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].uuid").value(containsInAnyOrder(
                        liveCourse.toString(), draftCourse.toString())));
    }

    @Test
    @DisplayName("Active courses are published ones only")
    void activeListingIsPublishedOnly() throws Exception {
        mockMvc.perform(get("/api/v1/courses/active").with(jwt(CREATOR_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].uuid").value(containsInAnyOrder(liveCourse.toString())));
    }

    @Test
    @DisplayName("A platform admin's search is unrestricted")
    void adminSearchIsUnrestricted() throws Exception {
        mockMvc.perform(get("/api/v1/courses/search").with(jwt(ADMIN_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(6));
    }

    @Test
    @DisplayName("The admin pending-approval queue still lists unapproved courses")
    void adminPendingQueueIsUnaffected() throws Exception {
        mockMvc.perform(get("/api/v1/admin/courses/pending").with(jwt(ADMIN_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].uuid").value(containsInAnyOrder(
                        inReviewCourse.toString(), unapprovedCourse.toString())));
    }

    @Test
    @DisplayName("An instructor's courses are the ones they are approved to deliver, as the caller may see them")
    void instructorCoursesAreTheirApprovedCourses() throws Exception {
        String url = "/api/v1/courses/instructor/" + trainerInstructorUuid;
        mockMvc.perform(get(url).with(jwt(TRAINER_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].uuid").value(containsInAnyOrder(
                        liveCourse.toString(), draftCourse.toString())));
        mockMvc.perform(get(url).with(jwt(OUTSIDER_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].uuid").value(containsInAnyOrder(liveCourse.toString())));
    }

    // ===== SINGLE COURSE =====

    @Test
    @DisplayName("Unpublished courses and shadow drafts are not found for an unrelated caller")
    void outsiderCannotReadUnpublishedCourses() throws Exception {
        for (UUID hidden : new UUID[]{draftCourse, inReviewCourse, shadowDraft}) {
            mockMvc.perform(get("/api/v1/courses/" + hidden).with(jwt(OUTSIDER_SUBJECT)))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    @DisplayName("Published and archived courses stay readable by anyone")
    void publishedAndArchivedStayReadable() throws Exception {
        for (UUID visible : new UUID[]{liveCourse, unapprovedCourse, archivedCourse}) {
            mockMvc.perform(get("/api/v1/courses/" + visible).with(jwt(OUTSIDER_SUBJECT)))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("The author reads their draft and shadow draft; an approved trainer only the draft")
    void relatedCallersReadWhatTheyAreTiedTo() throws Exception {
        mockMvc.perform(get("/api/v1/courses/" + draftCourse).with(jwt(CREATOR_SUBJECT)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/courses/" + shadowDraft).with(jwt(CREATOR_SUBJECT)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/courses/" + draftCourse).with(jwt(TRAINER_SUBJECT)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/courses/" + shadowDraft).with(jwt(TRAINER_SUBJECT)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/courses/" + shadowDraft).with(jwt(ADMIN_SUBJECT)))
                .andExpect(status().isOk());
    }

    // ===== ANONYMOUS CATALOGUE =====

    @Test
    @DisplayName("An anonymous visitor reads a public course, its prerequisites and skills, without its revenue terms")
    void anonymousReadsAPublicCourse() throws Exception {
        jdbc.update("UPDATE courses SET minimum_training_fee = 500, revenue_share_notes = 'private' WHERE uuid = ?",
                liveCourse);
        mockMvc.perform(get("/api/v1/courses/" + liveCourse))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.uuid").value(liveCourse.toString()))
                .andExpect(jsonPath("$.data.name").value("Live"))
                .andExpect(jsonPath("$.data.minimum_training_fee").value(nullValue()))
                .andExpect(jsonPath("$.data.creator_share_percentage").value(nullValue()))
                .andExpect(jsonPath("$.data.instructor_share_percentage").value(nullValue()))
                .andExpect(jsonPath("$.data.revenue_share_notes").value(nullValue()))
                .andExpect(jsonPath("$.data.created_by").value(nullValue()));
        mockMvc.perform(get("/api/v1/courses/" + liveCourse + "/prerequisites"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/courses/" + liveCourse + "/skills"))
                .andExpect(status().isOk());

        // A signed-in caller still gets the full record.
        mockMvc.perform(get("/api/v1/courses/" + liveCourse).with(jwt(OUTSIDER_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.revenue_share_notes").value("private"));
    }

    @Test
    @DisplayName("An anonymous visitor gets 404 for drafts, shadow drafts, unapproved and archived courses")
    void anonymousCannotReadNonPublicCourses() throws Exception {
        for (UUID hidden : new UUID[]{draftCourse, inReviewCourse, shadowDraft, unapprovedCourse, archivedCourse}) {
            mockMvc.perform(get("/api/v1/courses/" + hidden)).andExpect(status().isNotFound());
            mockMvc.perform(get("/api/v1/courses/" + hidden + "/prerequisites")).andExpect(status().isNotFound());
            mockMvc.perform(get("/api/v1/courses/" + hidden + "/skills")).andExpect(status().isNotFound());
        }
    }

    @Test
    @DisplayName("The anonymous course read does not open the sibling listings")
    void anonymousListingsStayAuthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/courses/search")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/courses/active")).andExpect(status().isUnauthorized());
    }

    // ===== TEST PLUMBING =====

    private RequestPostProcessor jwt(String subject) {
        return org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                .jwt().jwt(builder -> builder.subject(subject).claim("sub", subject));
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

    private UUID instructor(UUID userUuid) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO instructors (uuid, user_uuid, full_name, admin_verified, created_by) "
                + "VALUES (?, ?, 'Trainer', true, 'test')", uuid, userUuid);
        return uuid;
    }

    private UUID course(String name, String status, boolean active, boolean adminApproved, UUID parentCourseUuid) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO courses (uuid, name, course_creator_uuid, status, active, admin_approved, "
                + "parent_course_uuid, created_by) VALUES (?, ?, ?, ?, ?, ?, ?, 'test')",
                uuid, name, courseCreatorUuid, status, active, adminApproved, parentCourseUuid);
        return uuid;
    }

    private void trainingApplication(UUID courseUuid, UUID instructorUuid) {
        jdbc.update("INSERT INTO course_training_applications (uuid, course_uuid, applicant_type, applicant_uuid, "
                + "status, rate_currency, private_online_hourly_rate, private_inperson_hourly_rate, "
                + "group_online_hourly_rate, group_inperson_hourly_rate, created_by) "
                + "VALUES (?, ?, 'instructor', ?, 'approved', 'KES', 1000, 1000, 1000, 1000, 'test')",
                UUID.randomUUID(), courseUuid, instructorUuid);
    }
}
