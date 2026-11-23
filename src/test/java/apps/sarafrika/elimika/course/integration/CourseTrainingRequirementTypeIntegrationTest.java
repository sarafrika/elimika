package apps.sarafrika.elimika.course.integration;

import apps.sarafrika.elimika.testsupport.SharedPostgres;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code POST /api/v1/courses/{uuid}/training-requirements} accepts the instructor {@code education} type,
 * and an unknown type answers as a field validation error naming the accepted values rather than as
 * malformed JSON.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Course training requirement types (end-to-end)")
class CourseTrainingRequirementTypeIntegrationTest {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        SharedPostgres.register(registry);
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

    private static final String OWNER = "keycloak-training-req-owner";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @MockitoBean private JwtDecoder jwtDecoder;

    private UUID course;

    @BeforeEach
    void seed() {
        jdbc.execute("TRUNCATE course_training_requirements, course_pending_edits, course_version_snapshots, courses, "
                + "course_creators, user_domain_mapping, users RESTART IDENTITY CASCADE");
        UUID owner = user(OWNER);
        grantDomain(owner, "course_creator");
        course = course(courseCreator(owner), "draft", false);
    }

    @Test
    @DisplayName("an instructor education requirement is saved")
    void educationRequirementIsSaved() throws Exception {
        postRequirement("education")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.requirement_type").value("education"))
                .andExpect(jsonPath("$.data.provided_by").value("instructor"));

        assertThat(jdbc.queryForObject("SELECT requirement_type FROM course_training_requirements WHERE course_uuid = ?",
                String.class, course)).isEqualTo("education");
    }

    @Test
    @DisplayName("an unknown type names the field and the accepted values")
    void unknownTypeIsAFieldError() throws Exception {
        postRequirement("teleporter")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.error.requirement_type")
                        .value("requirement_type must be one of material, equipment, facility, other, education"));
    }

    @Test
    @DisplayName("genuinely malformed JSON keeps the generic message")
    void malformedJsonKeepsTheGenericMessage() throws Exception {
        mockMvc.perform(post("/api/v1/courses/" + course + "/training-requirements").with(jwt(OWNER))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Request body contains invalid or malformed JSON"));
    }

    private ResultActions postRequirement(String type) throws Exception {
        String json = "{\"course_uuid\": \"" + course + "\", \"requirement_type\": \"" + type + "\", "
                + "\"name\": \"Teaching licence\", \"quantity\": 1, \"unit\": \"license\", "
                + "\"provided_by\": \"instructor\", \"is_mandatory\": true}";
        return mockMvc.perform(post("/api/v1/courses/" + course + "/training-requirements").with(jwt(OWNER))
                .contentType(MediaType.APPLICATION_JSON).content(json));
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
