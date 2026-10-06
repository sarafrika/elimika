package apps.sarafrika.elimika.tenancy.integration;

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
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** One onboarding API across domains: shared steps reused, submission queued for admins, approval mirrored. */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("Onboarding API (end-to-end)")
class OnboardingApiIntegrationTest {

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

    @MockBean private JwtDecoder jwtDecoder;

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    private String ownerSubject;
    private String adminSubject;
    private UUID ownerUuid;
    private UUID instructorUuid;
    private UUID courseCreatorUuid;

    @BeforeEach
    void seed() {
        String run = UUID.randomUUID().toString().substring(0, 8);
        ownerSubject = "owner-" + run;
        adminSubject = "admin-" + run;
        ownerUuid = user(ownerSubject);
        domain(ownerUuid, "instructor", "PENDING");
        domain(ownerUuid, "course_creator", "PENDING");
        instructorUuid = UUID.randomUUID();
        jdbc.update("INSERT INTO instructors (uuid, user_uuid, full_name, created_by) VALUES (?, ?, 'Owner', 'test')",
                instructorUuid, ownerUuid);
        courseCreatorUuid = UUID.randomUUID();
        jdbc.update("INSERT INTO course_creators (uuid, user_uuid, full_name, created_by) VALUES (?, ?, 'Owner', 'test')",
                courseCreatorUuid, ownerUuid);
        domain(user(adminSubject), "admin", "APPROVED");
    }

    @Test
    @DisplayName("profile work done once completes the shared steps of both domains; each domain submits on its own")
    void multiDomainOnboarding() throws Exception {
        mockMvc.perform(get("/api/v1/onboarding/instructor").with(jwt(ownerSubject)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("not_started"))
                .andExpect(jsonPath("$.data.steps[0].key").value("account"))
                .andExpect(jsonPath("$.data.steps[0].complete").value(true));
        mockMvc.perform(post("/api/v1/onboarding/instructor/submit").with(jwt(ownerSubject)))
                .andExpect(status().isConflict());

        mockMvc.perform(put("/api/v1/me/profile").with(jwt(ownerSubject)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bio\":\"Builds robots with learners\",\"professional_headline\":\"Robotics trainer\","
                                + "\"location_name\":\"Kisumu\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/me/profile/skills").with(jwt(ownerSubject)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skill_name\":\"Robotics\",\"proficiency_level\":\"ADVANCED\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/onboarding/course_creator").with(jwt(ownerSubject)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("in_progress"))
                .andExpect(jsonPath("$.data.steps[1].key").value("professional_profile"))
                .andExpect(jsonPath("$.data.steps[1].shared").value(true))
                .andExpect(jsonPath("$.data.steps[1].complete").value(true))
                .andExpect(jsonPath("$.data.steps[2].counts.skills").value(1))
                .andExpect(jsonPath("$.data.steps[3].key").value("categories"))
                .andExpect(jsonPath("$.data.steps[3].complete").value(false))
                .andExpect(jsonPath("$.data.ready_for_submission").value(false));

        mockMvc.perform(post("/api/v1/onboarding/instructor/submit").with(jwt(ownerSubject)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("submitted"))
                .andExpect(jsonPath("$.data.submitted_at").isNotEmpty());
        mockMvc.perform(post("/api/v1/onboarding/instructor/submit").with(jwt(ownerSubject)))
                .andExpect(status().isConflict());

        UUID category = UUID.randomUUID();
        jdbc.update("INSERT INTO course_categories (uuid, name, created_by) VALUES (?, ?, 'test')", category,
                "Robotics " + category);
        mockMvc.perform(put("/api/v1/course-creators/me/onboarding/categories").with(jwt(ownerSubject))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category_uuids\":[\"" + category + "\"]}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/course-creators/me/onboarding/submit").with(jwt(ownerSubject)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.verification_status").value("SUBMITTED"));

        mockMvc.perform(get("/api/v1/onboarding").with(jwt(ownerSubject)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.domain=='course_creator')].status").value(hasItem("submitted")))
                .andExpect(jsonPath("$.data[?(@.domain=='instructor')].status").value(hasItem("submitted")));

        mockMvc.perform(get("/api/v1/admin/registrations").param("submitted", "true").with(jwt(adminSubject)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.user_uuid=='" + ownerUuid + "')].domain").value(hasItem("instructor")));
        mockMvc.perform(get("/api/v1/admin/registrations").param("submitted", "false").with(jwt(adminSubject)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].user_uuid").value(not(hasItem(ownerUuid.toString()))));

        mockMvc.perform(post("/api/v1/admin/users/{user}/domains/instructor/moderate", ownerUuid)
                        .param("action", "approve").with(jwt(adminSubject)))
                .andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT admin_verified FROM instructors WHERE uuid = ?", Boolean.class,
                instructorUuid)).isTrue();
        mockMvc.perform(get("/api/v1/onboarding/instructor").with(jwt(ownerSubject)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("approved"))
                .andExpect(jsonPath("$.data.active").value(true));
    }

    @Test
    @DisplayName("the admin domain has no onboarding and an unknown domain is rejected")
    void badDomains() throws Exception {
        mockMvc.perform(get("/api/v1/onboarding/admin").with(jwt(ownerSubject))).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/onboarding/wizard").with(jwt(ownerSubject))).andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/onboarding/organisation_user/submit").with(jwt(ownerSubject)))
                .andExpect(status().isNotFound());
    }

    private RequestPostProcessor jwt(String subject) {
        return SecurityMockMvcRequestPostProcessors.jwt().jwt(builder -> builder.subject(subject).claim("sub", subject));
    }

    private UUID user(String keycloakId) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO users (uuid, user_no, first_name, last_name, email, phone_number, keycloak_id, created_by) "
                        + "VALUES (?, ?, 'Test', 'User', ?, '+254700000000', ?, 'test')",
                uuid, String.format("%09d", Math.abs(uuid.hashCode()) % 1000000000), keycloakId + "@test.local",
                keycloakId);
        return uuid;
    }

    private void domain(UUID userUuid, String domainName, String status) {
        jdbc.update("INSERT INTO user_domain_mapping (user_uuid, domain_uuid, status) "
                + "VALUES (?, (SELECT uuid FROM user_domain WHERE domain_name = ?), ?)", userUuid, domainName, status);
    }
}
