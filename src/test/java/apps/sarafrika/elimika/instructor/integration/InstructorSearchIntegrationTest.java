package apps.sarafrika.elimika.instructor.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import apps.sarafrika.elimika.instructor.search.InstructorSearchSource;
import apps.sarafrika.elimika.search.internal.sync.SearchIndexRebuilder;
import apps.sarafrika.elimika.shared.search.SearchGateway;
import apps.sarafrika.elimika.shared.search.SearchRequest;
import apps.sarafrika.elimika.shared.search.SearchScope;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BooleanSupplier;
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
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * The {@code instructors} index end to end: documents built from the instructor tables, the scope
 * that hides unverified profiles from everyone but platform admins, {@code q} routing on the list
 * endpoints, and a skill added through the API reaching the index through its JPA trigger.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("Instructor search (end-to-end)")
class InstructorSearchIntegrationTest {

    private static final String MASTER_KEY = "integration-test-master-key-0123456789";
    private static final String ADMIN_SUBJECT = "keycloak-admin";
    private static final String OUTSIDER_SUBJECT = "keycloak-outsider";
    private static final String VERIFIED_SUBJECT = "keycloak-verified";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Container
    static GenericContainer<?> meilisearch = new GenericContainer<>(DockerImageName.parse("getmeili/meilisearch:v1.54.1"))
            .withEnv("MEILI_MASTER_KEY", MASTER_KEY)
            .withEnv("MEILI_NO_ANALYTICS", "true")
            .withExposedPorts(7700)
            .waitingFor(Wait.forHttp("/health").forStatusCode(200));

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

        registry.add("search.enabled", () -> "true");
        registry.add("search.read-enabled.instructors", () -> "true");
        registry.add("search.meilisearch.host",
                () -> "http://" + meilisearch.getHost() + ":" + meilisearch.getMappedPort(7700));
        registry.add("search.meilisearch.api-key", () -> MASTER_KEY);
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private SearchIndexRebuilder rebuilder;
    @Autowired private SearchGateway gateway;

    /** Never invoked: the jwt() post-processor sets the SecurityContext directly. */
    @MockBean private JwtDecoder jwtDecoder;

    private UUID verifiedInstructorUuid;
    private UUID unverifiedInstructorUuid;

    @BeforeEach
    void seed() {
        jdbc.execute("TRUNCATE instructor_skills, instructor_experience, instructor_reviews, instructors, "
                + "user_domain_mapping, users RESTART IDENTITY CASCADE");

        UUID adminUserUuid = user(ADMIN_SUBJECT, "admin@test.local", "Platform", "Admin");
        user(OUTSIDER_SUBJECT, "outsider@test.local", "Passing", "Stranger");
        UUID verifiedUserUuid = user(VERIFIED_SUBJECT, "amina@test.local", "Amina", "Otieno");
        UUID unverifiedUserUuid = user("keycloak-unverified", "brian@test.local", "Brian", "Kimani");
        jdbc.update("INSERT INTO user_domain_mapping (user_uuid, domain_uuid) "
                + "VALUES (?, (SELECT uuid FROM user_domain WHERE domain_name = 'admin'))", adminUserUuid);

        verifiedInstructorUuid = instructor(verifiedUserUuid, "Kisumu", true);
        unverifiedInstructorUuid = instructor(unverifiedUserUuid, "Nakuru", false);
        skill(verifiedInstructorUuid, "Kubernetes", "EXPERT");
        skill(unverifiedInstructorUuid, "Kubernetes", "BEGINNER");
        jdbc.update("INSERT INTO instructor_experience (uuid, instructor_uuid, position, organization_name, created_by) "
                + "VALUES (?, ?, 'Platform Engineer', 'Safaricom', 'test')", UUID.randomUUID(), verifiedInstructorUuid);

        rebuilder.rebuild(InstructorSearchSource.INDEX);
    }

    @Test
    @DisplayName("A misspelt skill finds the instructor, and the document carries no private fields")
    void typoInSkillFindsInstructor() throws Exception {
        JsonNode content = list("/api/v1/instructors/search?q=kubernets&skill_levels=EXPERT", ADMIN_SUBJECT);
        assertThat(uuids(content)).containsExactly(verifiedInstructorUuid.toString());
        assertThat(content.get(0).path("full_name").asText()).isEqualTo("Amina Otieno");

        Map<String, Object> document = gateway.search(SearchRequest.of(InstructorSearchSource.INDEX, "kubernetes",
                null, SearchScope.unrestricted("test"), 0, 10)).hits().stream()
                .filter(hit -> hit.uuid().equals(verifiedInstructorUuid)).findFirst().orElseThrow().document();
        assertThat(document).containsEntry("experience_organisations", List.of("Safaricom"))
                .containsEntry("skill_levels", List.of("EXPERT"))
                .doesNotContainKeys("user_uuid", "lat", "long", "latitude", "longitude", "email");
    }

    @Test
    @DisplayName("An unverified instructor is hidden from a non-admin and visible to a platform admin")
    void unverifiedInstructorHiddenFromNonAdmin() throws Exception {
        assertThat(uuids(list("/api/v1/instructors?q=kubernetes", OUTSIDER_SUBJECT)))
                .containsExactly(verifiedInstructorUuid.toString());
        assertThat(uuids(list("/api/v1/instructors?q=kubernetes", ADMIN_SUBJECT)))
                .containsExactlyInAnyOrder(verifiedInstructorUuid.toString(), unverifiedInstructorUuid.toString());
    }

    @Test
    @DisplayName("Adding a skill through the API re-indexes its instructor")
    void addingSkillReindexes() throws Exception {
        assertThat(uuids(list("/api/v1/instructors/search?q=terraform", ADMIN_SUBJECT))).isEmpty();

        mockMvc.perform(post("/api/v1/instructors/{uuid}/skills", verifiedInstructorUuid)
                        .with(jwt(VERIFIED_SUBJECT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"instructor_uuid\":\"" + verifiedInstructorUuid
                                + "\",\"skill_name\":\"Terraform\",\"proficiency_level\":\"ADVANCED\"}"))
                .andExpect(status().isCreated());

        awaitTrue(() -> {
            try {
                return uuids(list("/api/v1/instructors/search?q=terraform", OUTSIDER_SUBJECT))
                        .contains(verifiedInstructorUuid.toString());
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        });
    }

    // ===== Helpers =====

    private JsonNode list(String url, String subject) throws Exception {
        String body = mockMvc.perform(get(url).with(jwt(subject)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("data").path("content");
    }

    private static List<String> uuids(JsonNode content) {
        List<String> values = new ArrayList<>();
        content.forEach(node -> values.add(node.path("uuid").asText()));
        return values;
    }

    private static RequestPostProcessor jwt(String subject) {
        return SecurityMockMvcRequestPostProcessors.jwt().jwt(builder -> builder.subject(subject).claim("sub", subject));
    }

    private UUID user(String keycloakId, String email, String firstName, String lastName) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO users (uuid, user_no, first_name, last_name, email, keycloak_id, created_by) "
                        + "VALUES (?, ?, ?, ?, ?, ?, 'test')",
                uuid, String.format("%09d", Math.abs(uuid.hashCode()) % 1000000000),
                firstName, lastName, email, keycloakId);
        return uuid;
    }

    private UUID instructor(UUID userUuid, String locationName, boolean adminVerified) {
        UUID uuid = UUID.randomUUID();
        // full_name is derived from the user row by a database trigger.
        jdbc.update("INSERT INTO instructors (uuid, user_uuid, full_name, location_name, lat, long, "
                        + "admin_verified, bio, professional_headline, created_by) "
                        + "VALUES (?, ?, 'ignored', ?, -0.091700, 34.767900, ?, 'Teaches cloud', 'Cloud trainer', 'test')",
                uuid, userUuid, locationName, adminVerified);
        return uuid;
    }

    private void skill(UUID instructorUuid, String name, String level) {
        jdbc.update("INSERT INTO instructor_skills (uuid, instructor_uuid, skill_name, proficiency_level, created_by) "
                + "VALUES (?, ?, ?, ?, 'test')", UUID.randomUUID(), instructorUuid, name, level);
    }

    private static void awaitTrue(BooleanSupplier condition) {
        long deadline = System.nanoTime() + Duration.ofSeconds(20).toNanos();
        while (System.nanoTime() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new AssertionError(ex);
            }
        }
        throw new AssertionError("Condition not met within 20s");
    }
}
