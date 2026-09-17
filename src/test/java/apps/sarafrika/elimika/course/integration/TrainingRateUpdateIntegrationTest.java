package apps.sarafrika.elimika.course.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The rate update workflow over HTTP against the real schema, from proposal to approval. */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("Training rate updates (end-to-end)")
class TrainingRateUpdateIntegrationTest {

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

    private static final String CREATOR = "keycloak-creator";
    private static final String INSTRUCTOR = "keycloak-instructor";
    private static final String OUTSIDER = "keycloak-outsider";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private JwtDecoder jwtDecoder;

    private UUID courseUuid;
    private UUID applicationUuid;

    @BeforeEach
    void seed() {
        jdbc.execute("TRUNCATE course_training_rate_updates, course_training_applications, courses, course_creators, "
                + "instructors, user_domain_mapping, users RESTART IDENTITY CASCADE");

        UUID creatorUser = user(CREATOR, "creator@test.local");
        UUID instructorUser = user(INSTRUCTOR, "instructor@test.local");
        user(OUTSIDER, "outsider@test.local");
        grantGlobalDomain(creatorUser, "course_creator");
        grantGlobalDomain(instructorUser, "instructor");

        UUID creatorUuid = UUID.randomUUID();
        jdbc.update("INSERT INTO course_creators (uuid, user_uuid, full_name, created_by) VALUES (?, ?, 'Creator', 'test')",
                creatorUuid, creatorUser);
        courseUuid = UUID.randomUUID();
        jdbc.update("INSERT INTO courses (uuid, name, course_creator_uuid, status, active, minimum_training_fee, created_by) "
                + "VALUES (?, 'Welding', ?, 'published', true, 2000, 'test')", courseUuid, creatorUuid);
        UUID instructorUuid = UUID.randomUUID();
        jdbc.update("INSERT INTO instructors (uuid, user_uuid, full_name, created_by) VALUES (?, ?, 'ignored', 'test')",
                instructorUuid, instructorUser);

        applicationUuid = UUID.randomUUID();
        jdbc.update("INSERT INTO course_training_applications (uuid, course_uuid, applicant_type, applicant_uuid, status, "
                + "rate_currency, group_online_hourly_rate, group_online_session_rate, group_online_daily_rate, reviewed_at, "
                + "created_by) VALUES (?, ?, 'instructor', ?, 'approved', 'KES', 2500, 4000, 9000, now(), 'instructor@test.local')",
                applicationUuid, courseUuid, instructorUuid);
    }

    @Test
    @DisplayName("an instructor proposes, the creator approves, and the application carries the new card")
    void proposeThenApprove() throws Exception {
        String created = mockMvc.perform(post(updatesUrl()).with(jwt(INSTRUCTOR))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(proposal("3000")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("pending"))
                .andExpect(jsonPath("$.data.application_type").value("course"))
                .andExpect(jsonPath("$.data.current_rate_card.group_online_hourly_rate").value(2500.0))
                .andExpect(jsonPath("$.data.proposed_rate_card.group_online_hourly_rate").value(3000.0))
                .andExpect(jsonPath("$.data.proposed_rate_card.private_online_hourly_rate").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        String updateUuid = objectMapper.readTree(created).at("/data/uuid").asText();

        mockMvc.perform(post(updatesUrl()).with(jwt(INSTRUCTOR)).contentType(MediaType.APPLICATION_JSON).content(proposal("3100")))
                .andExpect(status().isConflict());

        mockMvc.perform(get(applicationUrl()).with(jwt(INSTRUCTOR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pending_rate_update_uuid").value(updateUuid));
        mockMvc.perform(get(updatesUrl()).with(jwt(OUTSIDER)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/courses/" + courseUuid + "/training-rate-updates?status=pending").with(jwt(CREATOR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1));
        mockMvc.perform(post(updatesUrl() + "/" + updateUuid + "?action=approve").with(jwt(INSTRUCTOR)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post(updatesUrl() + "/" + updateUuid + "?action=approve").with(jwt(CREATOR))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"review_notes\":\"Fair\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("approved"))
                .andExpect(jsonPath("$.data.review_notes").value("Fair"));

        Map<String, Object> row = jdbc.queryForMap(
                "SELECT status, group_online_hourly_rate, group_online_daily_rate FROM course_training_applications WHERE uuid = ?",
                applicationUuid);
        assertThat(row.get("status")).isEqualTo("approved");
        assertThat((BigDecimal) row.get("group_online_hourly_rate")).isEqualByComparingTo("3000");
        assertThat((BigDecimal) row.get("group_online_daily_rate")).isEqualByComparingTo("3000");

        JsonNode application = objectMapper.readTree(mockMvc.perform(get(applicationUrl()).with(jwt(CREATOR)))
                .andReturn().getResponse().getContentAsString());
        assertThat(application.at("/data/pending_rate_update_uuid").isNull()).isTrue();

        mockMvc.perform(delete(updatesUrl() + "/" + updateUuid).with(jwt(INSTRUCTOR)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("a rejected or withdrawn proposal leaves the card as it was")
    void rejectAndWithdraw() throws Exception {
        String first = objectMapper.readTree(mockMvc.perform(post(updatesUrl()).with(jwt(INSTRUCTOR))
                        .contentType(MediaType.APPLICATION_JSON).content(proposal("3000")))
                .andReturn().getResponse().getContentAsString()).at("/data/uuid").asText();
        mockMvc.perform(post(updatesUrl() + "/" + first + "?action=reject").with(jwt(CREATOR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("rejected"));

        String second = objectMapper.readTree(mockMvc.perform(post(updatesUrl()).with(jwt(INSTRUCTOR))
                        .contentType(MediaType.APPLICATION_JSON).content(proposal("3300")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).at("/data/uuid").asText();
        mockMvc.perform(delete(updatesUrl() + "/" + second).with(jwt(CREATOR)))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete(updatesUrl() + "/" + second).with(jwt(INSTRUCTOR)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(updatesUrl()).with(jwt(CREATOR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].status").value("withdrawn"))
                .andExpect(jsonPath("$.data[1].status").value("rejected"));
        BigDecimal rate = jdbc.queryForObject(
                "SELECT group_online_hourly_rate FROM course_training_applications WHERE uuid = ?", BigDecimal.class, applicationUuid);
        assertThat(rate).isEqualByComparingTo("2500");
    }

    @Test
    @DisplayName("a proposal below the course minimum is refused")
    void belowTheFloorIsRefused() throws Exception {
        mockMvc.perform(post(updatesUrl()).with(jwt(INSTRUCTOR)).contentType(MediaType.APPLICATION_JSON).content(proposal("1500")))
                .andExpect(status().isBadRequest());
    }

    private String proposal(String rate) {
        return """
                {"rate_card": {"currency": "KES",
                  "group_online_hourly_rate": %s, "group_online_session_rate": %s, "group_online_daily_rate": %s},
                 "note": "Costs rose"}
                """.formatted(rate, rate, rate);
    }

    private String applicationUrl() {
        return "/api/v1/courses/" + courseUuid + "/training-applications/" + applicationUuid;
    }

    private String updatesUrl() {
        return applicationUrl() + "/rate-updates";
    }

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

    private void grantGlobalDomain(UUID userUuid, String domainName) {
        jdbc.update("INSERT INTO user_domain_mapping (user_uuid, domain_uuid) "
                + "VALUES (?, (SELECT uuid FROM user_domain WHERE domain_name = ?))", userUuid, domainName);
    }
}
