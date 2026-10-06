package apps.sarafrika.elimika.profile.integration;

import com.jayway.jsonpath.JsonPath;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Self, viewer and admin profile APIs end to end, and the legacy routes reading what they wrote. */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("Professional profile API (end-to-end)")
class ProfessionalProfileApiIntegrationTest {

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
    private String outsiderSubject;
    private UUID ownerUuid;
    private UUID instructorUuid;
    private UUID courseCreatorUuid;

    @BeforeEach
    void seed() {
        String run = UUID.randomUUID().toString().substring(0, 8);
        ownerSubject = "owner-" + run;
        adminSubject = "admin-" + run;
        outsiderSubject = "outsider-" + run;

        ownerUuid = user(ownerSubject);
        grantDomain(ownerUuid, "instructor");
        grantDomain(ownerUuid, "course_creator");
        instructorUuid = UUID.randomUUID();
        jdbc.update("INSERT INTO instructors (uuid, user_uuid, full_name, created_by) VALUES (?, ?, 'Owner', 'test')",
                instructorUuid, ownerUuid);
        courseCreatorUuid = UUID.randomUUID();
        jdbc.update("INSERT INTO course_creators (uuid, user_uuid, full_name, created_by) VALUES (?, ?, 'Owner', 'test')",
                courseCreatorUuid, ownerUuid);

        grantDomain(user(adminSubject), "admin");
        grantDomain(user(outsiderSubject), "instructor");
    }

    @Test
    @DisplayName("basics saved once show on the instructor and course creator profiles")
    void basicsAreShared() throws Exception {
        mockMvc.perform(put("/api/v1/me/profile").with(jwt(ownerSubject)).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bio\":\"Builds robots with learners\",\"professional_headline\":\"Robotics trainer\","
                                + "\"location_name\":\"Kisumu\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.basics_complete").value(true));

        mockMvc.perform(get("/api/v1/instructors/{uuid}", instructorUuid).with(jwt(ownerSubject)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.professional_headline").value("Robotics trainer"));
        mockMvc.perform(get("/api/v1/course-creators/{uuid}", courseCreatorUuid).with(jwt(ownerSubject)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bio").value("Builds robots with learners"));
    }

    @Test
    @DisplayName("a skill added once is listed by both domains, verified once by an admin, reset when its claim changes")
    void skillLifecycle() throws Exception {
        String created = mockMvc.perform(post("/api/v1/me/profile/skills").with(jwt(ownerSubject))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skill_name\":\"Robotics\",\"proficiency_level\":\"ADVANCED\",\"evidence\":\"github\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.verification_status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();
        String skillUuid = JsonPath.read(created, "$.data.uuid");

        mockMvc.perform(get("/api/v1/instructors/{uuid}/skills", instructorUuid).with(jwt(ownerSubject)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].uuid").value(skillUuid))
                .andExpect(jsonPath("$.data.content[0].instructor_uuid").value(instructorUuid.toString()));
        mockMvc.perform(get("/api/v1/me/profile").with(jwt(ownerSubject)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.section_counts.skills").value(1));

        String verification = "/api/v1/admin/users/{user}/profile/skills/{item}/verification";
        mockMvc.perform(post(verification, ownerUuid, skillUuid).with(jwt(outsiderSubject))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"VERIFIED\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(verification, ownerUuid, skillUuid).with(jwt(adminSubject))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"VERIFIED\",\"notes\":\"seen\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/course-creators/{uuid}/skills", courseCreatorUuid).with(jwt(ownerSubject)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].verification_status").value("VERIFIED"));

        mockMvc.perform(put("/api/v1/me/profile/skills/{item}", skillUuid).with(jwt(ownerSubject))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skill_name\":\"Robotics\",\"proficiency_level\":\"EXPERT\",\"evidence\":\"new demo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.verification_status").value("PENDING"));
    }

    @Test
    @DisplayName("another user's profile is read by its owner and admins, not by an unrelated user")
    void viewerRule() throws Exception {
        mockMvc.perform(get("/api/v1/users/{user}/profile", ownerUuid).with(jwt(outsiderSubject)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/users/{user}/profile/education", ownerUuid).with(jwt(outsiderSubject)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/users/{user}/profile", ownerUuid).with(jwt(ownerSubject)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/users/{user}/profile/skills", ownerUuid).with(jwt(adminSubject)))
                .andExpect(status().isOk());
    }

    private RequestPostProcessor jwt(String subject) {
        return SecurityMockMvcRequestPostProcessors.jwt().jwt(builder -> builder.subject(subject).claim("sub", subject));
    }

    private UUID user(String keycloakId) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO users (uuid, user_no, first_name, last_name, email, keycloak_id, created_by) "
                        + "VALUES (?, ?, 'Test', 'User', ?, ?, 'test')",
                uuid, String.format("%09d", Math.abs(uuid.hashCode()) % 1000000000), keycloakId + "@test.local",
                keycloakId);
        return uuid;
    }

    private void grantDomain(UUID userUuid, String domainName) {
        jdbc.update("INSERT INTO user_domain_mapping (user_uuid, domain_uuid) "
                + "VALUES (?, (SELECT uuid FROM user_domain WHERE domain_name = ?))", userUuid, domainName);
    }
}
