package apps.sarafrika.elimika.course.integration;

import apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService;
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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Proves {@code GET /api/v1/courses/{uuid}/content} over real HTTP, against a real database,
 * through the real security filter chain.
 * <p>
 * The load-bearing assertion is the negative one. An instructor whose application to train is still
 * pending must receive a payload that <em>contains</em> no lesson content — not a payload the client
 * is trusted to hide. A unit test on the assembler could only prove the DTO's fields are null;
 * only serialising the real response and reading the bytes proves that nothing of the course's
 * teaching material crossed the wire, and that the lesson identifiers which would fetch it one
 * request at a time did not cross either.
 * <p>
 * The positive case is here for the same reason a control is: without it, an endpoint that returned
 * an empty outline to everybody would pass the negative assertions perfectly.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("Course content access (end-to-end)")
class CourseContentAccessIntegrationTest {

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

    private static final String CREATOR_SUBJECT = "keycloak-creator";
    private static final String PENDING_INSTRUCTOR_SUBJECT = "keycloak-pending-instructor";

    /** Nothing but this string identifies the withheld material, so it is the leak detector. */
    private static final String SECRET_CONTENT_TITLE = "Module one: the whole syllabus";
    private static final String SECRET_CONTENT_BODY = "Everything an approved trainer is allowed to read.";

    /** Work in progress. Nobody without full access has any business seeing it named. */
    private static final String DRAFT_LESSON_TITLE = "Lesson two, still being written";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    @MockBean private ClassDefinitionLookupService classDefinitionLookupService;
    /** Never invoked: the jwt() post-processor sets the SecurityContext directly. Mocked only so
     *  the resource server does not fetch Keycloak's JWKS at boot. */
    @MockBean private JwtDecoder jwtDecoder;

    private UUID courseUuid;
    private UUID lessonUuid;

    @BeforeEach
    void seed() {
        clean();

        UUID creatorUserUuid = user(CREATOR_SUBJECT, "creator@test.local");
        grantDomain(creatorUserUuid, "course_creator");
        UUID courseCreatorUuid = courseCreator(creatorUserUuid);

        courseUuid = course("Course with a syllabus worth guarding", courseCreatorUuid);
        lessonUuid = lesson(courseUuid, 1, "Lesson one", "published", true);
        lessonContent(lessonUuid, SECRET_CONTENT_TITLE, SECRET_CONTENT_BODY, 1);
        lessonContent(lessonUuid, "Module one: exercises", "Worked examples.", 2);
        lesson(courseUuid, 2, DRAFT_LESSON_TITLE, "draft", false);

        UUID instructorUserUuid = user(PENDING_INSTRUCTOR_SUBJECT, "pending@test.local");
        grantDomain(instructorUserUuid, "instructor");
        UUID instructorUuid = instructor(instructorUserUuid, "Hopeful Trainer");
        application(courseUuid, instructorUuid, "pending");
    }

    // ===== THE RULE: NOT APPROVED MEANS NOT TRANSMITTED =====

    @Test
    @DisplayName("A pending applicant's payload carries no content items and no lesson uuid")
    void pendingApplicantIsSentNothingToHide() throws Exception {
        String body = mockMvc.perform(get(contentUrl()).with(jwt(PENDING_INSTRUCTOR_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.access").value("pending"))
                .andExpect(jsonPath("$.data.full_access").value(false))
                // The draft lesson is not counted, because it is not shown.
                .andExpect(jsonPath("$.data.total_lessons").value(1))
                // The outline still arrives — it is what the applicant decides on.
                .andExpect(jsonPath("$.data.lessons[0].title").value("Lesson one"))
                .andExpect(jsonPath("$.data.lessons[0].content_count").value(2))
                // Neither the content items nor the key that would fetch them separately.
                .andExpect(jsonPath("$.data.lessons[0].contents").doesNotExist())
                .andExpect(jsonPath("$.data.lessons[0].uuid").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        // Read the bytes, not the DTO: this is the assertion a client-side filter would fail.
        assertThat(body)
                .doesNotContain(SECRET_CONTENT_TITLE)
                .doesNotContain(SECRET_CONTENT_BODY)
                .doesNotContain(lessonUuid.toString())
                .doesNotContain(DRAFT_LESSON_TITLE);
    }

    @Test
    @DisplayName("An anonymous browser is sent the course itself, so a public course page can render")
    void anonymousBrowserIsSentTheCourseProfile() throws Exception {
        String body = mockMvc.perform(get(contentUrl()))
                .andExpect(status().isOk())
                // Following a link out of the catalogue used to 404 for a logged-out visitor: the
                // page needed the course record, and that endpoint is authenticated.
                .andExpect(jsonPath("$.data.course.name").value("Course with a syllabus worth guarding"))
                .andExpect(jsonPath("$.data.course.published").value(true))
                .andReturn().getResponse().getContentAsString();

        // The profile is public, so it must carry none of the commercial terms that made the course
        // record authenticated in the first place.
        assertThat(body)
                .doesNotContain("minimum_training_fee")
                .doesNotContain("creator_share_percentage")
                .doesNotContain("instructor_share_percentage")
                .doesNotContain("revenue_share_notes");
    }

    @Test
    @DisplayName("An anonymous browser resolves to prospect and is sent the same outline")
    void anonymousBrowserIsAProspect() throws Exception {
        String body = mockMvc.perform(get(contentUrl()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.access").value("prospect"))
                .andExpect(jsonPath("$.data.full_access").value(false))
                .andExpect(jsonPath("$.data.lessons[0].contents").doesNotExist())
                .andExpect(jsonPath("$.data.lessons[0].uuid").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        assertThat(body)
                .doesNotContain(SECRET_CONTENT_TITLE)
                .doesNotContain(lessonUuid.toString())
                .doesNotContain(DRAFT_LESSON_TITLE);
    }

    // ===== THE CONTROL: THE ENDPOINT DOES SERVE CONTENT TO SOMEBODY =====

    @Test
    @DisplayName("The course creator reads the whole thing, so the negative cases are not vacuous")
    void creatorReadsEverything() throws Exception {
        String body = mockMvc.perform(get(contentUrl()).with(jwt(CREATOR_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.access").value("creator"))
                .andExpect(jsonPath("$.data.full_access").value(true))
                .andExpect(jsonPath("$.data.lessons[0].uuid").value(lessonUuid.toString()))
                .andExpect(jsonPath("$.data.lessons[0].contents.length()").value(2))
                // Drafting is the creator's job, so their own unfinished lesson still shows.
                .andExpect(jsonPath("$.data.total_lessons").value(2))
                .andExpect(jsonPath("$.data.lessons[1].title").value(DRAFT_LESSON_TITLE))
                .andReturn().getResponse().getContentAsString();

        assertThat(body).contains(SECRET_CONTENT_TITLE).contains(SECRET_CONTENT_BODY);
    }

    // ===== TEST PLUMBING =====

    private String contentUrl() {
        return "/api/v1/courses/" + courseUuid + "/content";
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor jwt(String subject) {
        return org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                .jwt().jwt(builder -> builder.subject(subject).claim("sub", subject));
    }

    private void clean() {
        jdbc.execute("TRUNCATE course_training_applications, lesson_contents, lessons, course_reviews, "
                + "courses, course_creators, instructors, user_domain_mapping, users "
                + "RESTART IDENTITY CASCADE");
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
        jdbc.update("INSERT INTO instructors (uuid, user_uuid, full_name, created_by, updated_by) "
                + "VALUES (?, ?, ?, 'test', 'test')", uuid, userUuid, fullName);
        return uuid;
    }

    private UUID course(String name, UUID courseCreatorUuid) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO courses (uuid, name, course_creator_uuid, status, active, created_by) "
                + "VALUES (?, ?, ?, 'published', true, 'test')", uuid, name, courseCreatorUuid);
        return uuid;
    }

    private UUID lesson(UUID courseUuid, int number, String title, String status, boolean active) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO lessons (uuid, course_uuid, lesson_number, title, status, active, created_by) "
                + "VALUES (?, ?, ?, ?, ?::varchar, ?, 'test')", uuid, courseUuid, number, title, status, active);
        return uuid;
    }

    private void lessonContent(UUID lessonUuid, String title, String text, int order) {
        jdbc.update("INSERT INTO lesson_contents "
                + "(uuid, lesson_uuid, content_type_uuid, title, content_text, display_order, created_by) "
                + "VALUES (?, ?, (SELECT uuid FROM lesson_content_types WHERE name = 'Text'), ?, ?, ?, 'test')",
                UUID.randomUUID(), lessonUuid, title, text, order);
    }

    /**
     * The applicant type and status are written lower-case because that is what the entity's
     * {@code AttributeConverter} writes, and therefore what its queries bind.
     */
    private void application(UUID courseUuid, UUID instructorUuid, String status) {
        jdbc.update("INSERT INTO course_training_applications "
                + "(uuid, course_uuid, applicant_type, applicant_uuid, status, rate_currency, "
                + " private_online_hourly_rate, private_inperson_hourly_rate, "
                + " group_online_hourly_rate, group_inperson_hourly_rate, created_by) "
                + "VALUES (?, ?, 'instructor', ?, ?, 'KES', 100, 100, 100, 100, 'test')",
                UUID.randomUUID(), courseUuid, instructorUuid, status);
    }
}
