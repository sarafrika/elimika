package apps.sarafrika.elimika.tenancy.integration;

import apps.sarafrika.elimika.testsupport.SharedPostgres;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Organisation onboarding details over real HTTP against the Flyway schema: organisation type and capabilities
 * round-trip through create and update.
 */
// Each class gets its own database; a cached context must not outlive it.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Organisation onboarding details (end-to-end)")
class OrganisationOnboardingDetailsIntegrationTest {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        SharedPostgres.register(registry);
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
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    /** Never invoked: the jwt() post-processor sets the SecurityContext directly. */
    @MockitoBean private JwtDecoder jwtDecoder;

    private String subject;

    @BeforeEach
    void seed() {
        subject = "org-owner-" + UUID.randomUUID().toString().substring(0, 8);
        jdbc.update("INSERT INTO users (uuid, user_no, first_name, last_name, email, keycloak_id, created_by) "
                        + "VALUES (?, ?, 'Org', 'Owner', ?, ?, 'test')",
                UUID.randomUUID(), String.format("%09d", Math.abs(subject.hashCode()) % 1000000000),
                subject + "@test.local", subject);
    }

    @Test
    @DisplayName("type and capabilities are saved on create, kept when an update omits them, and replaced when sent")
    void typeAndCapabilitiesRoundTrip() throws Exception {
        String created = mockMvc.perform(post("/api/v1/organisations").with(owner())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Acme Academy %s","country":"Kenya","active":true,
                                 "organisation_type":"school_or_training_institution",
                                 "capabilities":["HIRE","TRAIN","TRAIN"]}
                                """.formatted(subject)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.organisation_type").value("SCHOOL_OR_TRAINING_INSTITUTION"))
                .andExpect(jsonPath("$.data.capabilities.length()").value(2))
                .andExpect(jsonPath("$.data.capabilities[0]").value("HIRE"))
                .andExpect(jsonPath("$.data.capabilities[1]").value("TRAIN"))
                .andReturn().getResponse().getContentAsString();
        String uuid = JsonPath.read(created, "$.data.uuid");
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject(
                "SELECT array_to_string(capabilities, ',') FROM organisation WHERE uuid = ?::uuid", String.class, uuid))
                .isEqualTo("HIRE,TRAIN");

        // An update that omits both keeps them (the onboarding form saves other fields separately).
        mockMvc.perform(put("/api/v1/organisations/" + uuid).with(owner())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Acme Academy " + subject + "\",\"active\":true,\"location\":\"Nairobi\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.organisation_type").value("SCHOOL_OR_TRAINING_INSTITUTION"))
                .andExpect(jsonPath("$.data.capabilities.length()").value(2));

        mockMvc.perform(put("/api/v1/organisations/" + uuid).with(owner())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Acme Academy %s","active":true,
                                 "organisation_type":"FOUNDATION_OR_NGO","capabilities":["FUND"]}
                                """.formatted(subject)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.organisation_type").value("FOUNDATION_OR_NGO"))
                .andExpect(jsonPath("$.data.capabilities.length()").value(1))
                .andExpect(jsonPath("$.data.capabilities[0]").value("FUND"));

        mockMvc.perform(put("/api/v1/organisations/" + uuid).with(owner())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Acme Academy " + subject + "\",\"active\":true,\"capabilities\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.capabilities.length()").value(0));

        mockMvc.perform(get("/api/v1/organisations/" + uuid).with(owner()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.organisation_type").value("FOUNDATION_OR_NGO"))
                .andExpect(jsonPath("$.data.capabilities.length()").value(0));
    }

    @Test
    @DisplayName("unknown type or capability values are rejected with 400")
    void unknownValuesAreRejected() throws Exception {
        mockMvc.perform(post("/api/v1/organisations").with(owner())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Bad Type\",\"active\":true,\"organisation_type\":\"CHURCH\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/organisations").with(owner())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Bad Capability\",\"active\":true,\"capabilities\":[\"TRADE\"]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("capabilities written directly to the TEXT[] column read back, and a retired type reads as null")
    void storageAndRetiredType() throws Exception {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO organisation (uuid, name, organisation_type, capabilities, created_by) "
                + "VALUES (?, 'Retired Type', 'CHURCH', ARRAY['FUND', 'HIRE'], 'test')", uuid);

        mockMvc.perform(get("/api/v1/organisations/" + uuid).with(owner()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.organisation_type").doesNotExist())
                .andExpect(jsonPath("$.data.capabilities.length()").value(2))
                .andExpect(jsonPath("$.data.capabilities[0]").value("FUND"));
    }

    private RequestPostProcessor owner() {
        return jwt().jwt(builder -> builder.subject(subject).claim("sub", subject));
    }
}
