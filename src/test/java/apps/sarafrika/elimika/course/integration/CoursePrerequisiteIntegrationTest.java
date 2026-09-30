package apps.sarafrika.elimika.course.integration;

import apps.sarafrika.elimika.course.service.CourseDraftService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code GET/PUT /api/v1/courses/{uuid}/prerequisites} end to end: owner-only writes, the validation
 * rules (self, cycle, foreign unpublished course) and the draft-over-live routing and promotion.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("Course prerequisites (end-to-end)")
class CoursePrerequisiteIntegrationTest {

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

    private static final String OWNER = "keycloak-prereq-owner";
    private static final String OTHER_CREATOR = "keycloak-prereq-other";
    private static final String LEARNER = "keycloak-prereq-learner";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private CourseDraftService courseDraftService;
    @MockBean private JwtDecoder jwtDecoder;

    private UUID ownerCreator;
    private UUID otherCreator;

    @BeforeEach
    void seed() {
        jdbc.execute("TRUNCATE course_prerequisites, course_pending_edits, course_version_snapshots, courses, "
                + "course_creators, students, user_domain_mapping, users RESTART IDENTITY CASCADE");
        UUID ownerUser = user(OWNER);
        grantDomain(ownerUser, "course_creator");
        ownerCreator = courseCreator(ownerUser);
        UUID otherUser = user(OTHER_CREATOR);
        grantDomain(otherUser, "course_creator");
        otherCreator = courseCreator(otherUser);
        grantDomain(user(LEARNER), "student");
    }

    @Test
    @DisplayName("the owner replaces the set; anyone who can read the course reads it")
    void ownerWritesAndOthersRead() throws Exception {
        UUID course = course(ownerCreator, "published", false);
        UUID mine = course(ownerCreator, "draft", false);
        UUID theirs = course(otherCreator, "published", false);

        putPrerequisites(OWNER, course, body(mine, true, theirs, false))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        mockMvc.perform(get(url(course)).with(jwt(LEARNER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].prerequisite_course_uuid").value(mine.toString()))
                .andExpect(jsonPath("$.data[0].is_mandatory").value(true))
                .andExpect(jsonPath("$.data[1].prerequisite_course_uuid").value(theirs.toString()))
                .andExpect(jsonPath("$.data[1].is_mandatory").value(false));

        // An empty list clears it.
        putPrerequisites(OWNER, course, "{\"prerequisites\": []}").andExpect(status().isOk());
        assertThat(count(course)).isZero();
    }

    @Test
    @DisplayName("writes are owner-only: another creator and a learner are refused")
    void writesAreOwnerOnly() throws Exception {
        UUID course = course(ownerCreator, "published", false);
        UUID prior = course(ownerCreator, "published", false);

        putPrerequisites(OTHER_CREATOR, course, body(prior, true)).andExpect(status().isForbidden());
        putPrerequisites(LEARNER, course, body(prior, true)).andExpect(status().isForbidden());
        mockMvc.perform(put(url(course)).contentType(MediaType.APPLICATION_JSON).content(body(prior, true)))
                .andExpect(status().isUnauthorized());
        assertThat(count(course)).isZero();
    }

    @Test
    @DisplayName("a cycle is rejected, directly or through a chain, as is a self-reference")
    void cyclesAreRejected() throws Exception {
        UUID a = course(ownerCreator, "published", false);
        UUID b = course(ownerCreator, "published", false);
        UUID c = course(ownerCreator, "published", false);

        putPrerequisites(OWNER, a, body(b, true)).andExpect(status().isOk());
        putPrerequisites(OWNER, b, body(c, true)).andExpect(status().isOk());

        putPrerequisites(OWNER, b, body(a, true)).andExpect(status().isBadRequest());
        putPrerequisites(OWNER, c, body(a, false)).andExpect(status().isBadRequest());
        putPrerequisites(OWNER, a, body(a, true)).andExpect(status().isBadRequest());

        assertThat(count(c)).isZero();
        assertThat(count(b)).isEqualTo(1);
    }

    @Test
    @DisplayName("another creator's unpublished course cannot be a prerequisite")
    void foreignUnpublishedCourseIsRefused() throws Exception {
        UUID course = course(ownerCreator, "published", false);
        UUID foreignDraft = course(otherCreator, "draft", false);

        putPrerequisites(OWNER, course, body(foreignDraft, true)).andExpect(status().isBadRequest());
        putPrerequisites(OWNER, course, body(UUID.randomUUID(), true)).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("on a live approved course the change lands on the draft and goes live on promotion")
    void liveCourseRoutesThroughTheDraft() throws Exception {
        UUID live = course(ownerCreator, "published", true);
        UUID prior = course(ownerCreator, "published", false);

        putPrerequisites(OWNER, live, body(prior, true))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].course_uuid").value(not(live.toString())));
        assertThat(count(live)).isZero();

        UUID pendingEdit = jdbc.queryForObject(
                "SELECT uuid FROM course_pending_edits WHERE course_uuid = ? AND status = 'pending'", UUID.class, live);
        courseDraftService.promote(live, pendingEdit);

        assertThat(count(live)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT prerequisite_course_uuid FROM course_prerequisites WHERE course_uuid = ?",
                UUID.class, live)).isEqualTo(prior);
        // The promoted version records the prerequisite.
        assertThat(jdbc.queryForObject("SELECT snapshot -> 'prerequisites' -> 0 ->> 'prerequisite_course_uuid' "
                + "FROM course_version_snapshots WHERE course_uuid = ?", String.class, live)).isEqualTo(prior.toString());
    }

    // ----------------------------------------------------------------- plumbing

    private ResultActions putPrerequisites(String subject, UUID course, String body) throws Exception {
        return mockMvc.perform(put(url(course)).with(jwt(subject)).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private static String body(Object... pairs) {
        StringBuilder json = new StringBuilder("{\"prerequisites\": [");
        for (int i = 0; i < pairs.length; i += 2) {
            if (i > 0) {
                json.append(',');
            }
            json.append("{\"prerequisite_course_uuid\": \"").append(pairs[i])
                    .append("\", \"is_mandatory\": ").append(pairs[i + 1]).append('}');
        }
        return json.append("]}").toString();
    }

    private long count(UUID course) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM course_prerequisites WHERE course_uuid = ?", Long.class, course);
    }

    private static String url(UUID course) {
        return "/api/v1/courses/" + course + "/prerequisites";
    }

    private RequestPostProcessor jwt(String subject) {
        return org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                .jwt().jwt(builder -> builder.subject(subject).claim("sub", subject));
    }

    private UUID user(String keycloakId) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO users (uuid, user_no, first_name, last_name, email, keycloak_id, created_by) "
                        + "VALUES (?, ?, 'Test', 'User', ?, ?, 'test')",
                uuid, String.format("%09d", Math.floorMod(uuid.hashCode(), 1_000_000_000)),
                keycloakId + "@t.io", keycloakId);
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

    private UUID course(UUID creator, String status, boolean adminApproved) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO courses (uuid, name, course_creator_uuid, status, active, admin_approved, "
                        + "creator_share_percentage, instructor_share_percentage, created_by) "
                        + "VALUES (?, ?, ?, ?, ?, ?, 30, 70, 'test')",
                uuid, "Course " + uuid.toString().substring(0, 8), creator, status, "published".equals(status),
                adminApproved);
        return uuid;
    }
}
