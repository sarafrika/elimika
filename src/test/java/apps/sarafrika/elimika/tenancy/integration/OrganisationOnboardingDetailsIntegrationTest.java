package apps.sarafrika.elimika.tenancy.integration;

import apps.sarafrika.elimika.testsupport.SharedPostgres;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Organisation onboarding details over real HTTP against the Flyway schema: organisation type and capabilities
 * round-trip through create and update, and one that trains is asked for the M-Pesa number to be paid on.
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
                .andExpect(jsonPath("$.data.capabilities[0]").value("TRAIN"))
                .andExpect(jsonPath("$.data.capabilities[1]").value("HIRE"))
                .andReturn().getResponse().getContentAsString();
        String uuid = JsonPath.read(created, "$.data.uuid");

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
    @DisplayName("the database refuses capabilities outside the known set")
    void databaseGuardsCapabilities() {
        assertThatThrownBy(() -> jdbc.update("INSERT INTO organisation (uuid, name, capabilities, created_by) "
                + "VALUES (?, 'Raw Insert', ARRAY['TRADE'], 'test')", UUID.randomUUID()))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("chk_organisation_capabilities");
    }

    @Test
    @DisplayName("an organisation that trains gets a payout step, completed once its M-Pesa number is saved")
    void payoutStepFollowsSavedNumber() throws Exception {
        String uuid = createTrainingOrganisation();
        String url = "/api/v1/organisations/" + uuid + "/payout-details";

        mockMvc.perform(get("/api/v1/onboarding/organisation_user").with(owner()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.steps[?(@.key == 'organisation_payout')].missing[0]")
                        .value(hasItem("payout_details")));
        mockMvc.perform(get(url).with(owner())).andExpect(status().isNotFound());

        mockMvc.perform(put(url).with(owner()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mpesa_number\":\"0712 345 678\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mpesa_number").value("254712345678"));

        mockMvc.perform(get("/api/v1/onboarding/organisation_user").with(owner()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.steps[?(@.key == 'organisation_payout')].complete").value(hasItem(true)));

        // Saving again replaces the number.
        mockMvc.perform(put(url).with(owner()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mpesa_number\":\"+254 110 000 111\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get(url).with(owner()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mpesa_number").value("254110000111"));
    }

    @Test
    @DisplayName("an M-Pesa number must be a Kenyan mobile number and is kept from other users")
    void payoutNumberIsValidatedAndPrivate() throws Exception {
        String url = "/api/v1/organisations/" + createTrainingOrganisation() + "/payout-details";

        for (String number : new String[]{"", "522522", "0812345678", "+256712345678"}) {
            mockMvc.perform(put(url).with(owner()).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"mpesa_number\":\"" + number + "\"}"))
                    .andExpect(status().isBadRequest());
        }

        mockMvc.perform(put(url).with(jwt().jwt(builder -> builder.subject("stranger").claim("sub", "stranger")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mpesa_number\":\"0712345678\"}"))
                .andExpect(status().isForbidden());
    }

    private String createTrainingOrganisation() throws Exception {
        String created = mockMvc.perform(post("/api/v1/organisations").with(owner())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Payout Academy %s\",\"country\":\"Kenya\",\"active\":true,\"capabilities\":[\"TRAIN\"]}"
                                .formatted(subject)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(created, "$.data.uuid");
    }

    private RequestPostProcessor owner() {
        return jwt().jwt(builder -> builder.subject(subject).claim("sub", subject));
    }
}
