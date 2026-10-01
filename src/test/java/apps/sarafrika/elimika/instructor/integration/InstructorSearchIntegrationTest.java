package apps.sarafrika.elimika.instructor.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import apps.sarafrika.elimika.instructor.search.InstructorSearchSource;
import apps.sarafrika.elimika.search.internal.sync.SearchIndexRebuilder;
import apps.sarafrika.elimika.shared.search.SearchGateway;
import apps.sarafrika.elimika.shared.search.SearchRequest;
import apps.sarafrika.elimika.shared.search.SearchScope;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.regex.Pattern;
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
    @Autowired private apps.sarafrika.elimika.search.config.SearchProperties searchProperties;

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
    @DisplayName("List and search rows carry rating_avg and review_count")
    void rowsCarryRatings() throws Exception {
        // Reviews reference students and enrolments this test does not need: skip the foreign keys.
        jdbc.execute("SET session_replication_role = replica; "
                + "INSERT INTO instructor_reviews (uuid, instructor_uuid, student_uuid, enrollment_uuid, rating, created_by) VALUES "
                + "('" + UUID.randomUUID() + "', '" + verifiedInstructorUuid + "', '" + UUID.randomUUID() + "', '"
                + UUID.randomUUID() + "', 4, 'test'), "
                + "('" + UUID.randomUUID() + "', '" + verifiedInstructorUuid + "', '" + UUID.randomUUID() + "', '"
                + UUID.randomUUID() + "', 5, 'test'); "
                + "SET session_replication_role = origin");

        for (String url : List.of("/api/v1/instructors", "/api/v1/instructors?q=kubernetes")) {
            JsonNode content = list(url, ADMIN_SUBJECT);
            Map<String, JsonNode> byUuid = new java.util.HashMap<>();
            content.forEach(row -> byUuid.put(row.path("uuid").asText(), row));
            JsonNode reviewed = byUuid.get(verifiedInstructorUuid.toString());
            assertThat(reviewed.path("rating_avg").asDouble()).isEqualTo(4.5);
            assertThat(reviewed.path("review_count").asLong()).isEqualTo(2);
            JsonNode unreviewed = byUuid.get(unverifiedInstructorUuid.toString());
            assertThat(unreviewed.path("review_count").asLong()).isZero();
            assertThat(unreviewed.has("rating_avg")).isFalse();
        }
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

    @Test
    @DisplayName("q is served only by the index: 503 with reads off, 400 for a removed text operator")
    void noDatabaseFallback() throws Exception {
        searchProperties.getReadEnabled().put(InstructorSearchSource.INDEX, false);
        try {
            mockMvc.perform(get("/api/v1/instructors/search?q=kubernetes").with(jwt(ADMIN_SUBJECT)))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.message").value("Search is unavailable"));
            // Without q the relational filters still answer from the database.
            assertThat(uuids(list("/api/v1/instructors/search?admin_verified=true", ADMIN_SUBJECT)))
                    .containsExactly(verifiedInstructorUuid.toString());
        } finally {
            searchProperties.getReadEnabled().put(InstructorSearchSource.INDEX, true);
        }
        mockMvc.perform(get("/api/v1/instructors/search?fullName_like=amina").with(jwt(ADMIN_SUBJECT)))
                .andExpect(status().isBadRequest());
    }

    // ===== Near me =====

    private static final String NAIROBI_SUBJECT = "keycloak-nairobi";
    /** More than two decimals on purpose: the index and every response must round it. */
    private static final String NAIROBI_LAT = "-1.292066";
    private static final String NAIROBI_LNG = "36.821946";
    private static final Pattern FINE_NUMBER = Pattern.compile("-?\\d+\\.\\d{3,}");

    @Test
    @DisplayName("An opted-out instructor never appears in near-me results, even verified and right next door")
    void optedOutInstructorNeverAppears() throws Exception {
        // The seeded verified instructor sits at -0.0917, 34.7679 and never opted in.
        for (String subject : List.of(OUTSIDER_SUBJECT, ADMIN_SUBJECT, VERIFIED_SUBJECT)) {
            assertThat(uuids(list("/api/v1/instructors?near=-0.09,34.77&radius_km=100", subject))).isEmpty();
            assertThat(uuids(list("/api/v1/instructors?q=kubernetes&near=-0.09,34.77", subject))).isEmpty();
        }
        assertThat(hits("/api/v1/search/instructors?near=-0.09,34.77&radius_km=100", ADMIN_SUBJECT)).isEmpty();
    }

    @Test
    @DisplayName("An opted-in verified instructor appears within the radius with a distance band, and not outside it")
    void optedInInstructorWithinRadius() throws Exception {
        UUID nairobi = optedInNairobiInstructor();
        // An unverified instructor who opted in at the same spot stays out.
        jdbc.update("UPDATE instructors SET location_search_opt_in = TRUE, lat = ?, long = ? WHERE uuid = ?",
                new BigDecimal(NAIROBI_LAT), new BigDecimal(NAIROBI_LNG), unverifiedInstructorUuid);
        rebuilder.rebuild(InstructorSearchSource.INDEX);

        String body = body("/api/v1/instructors?near=-1.30,36.83&radius_km=5", OUTSIDER_SUBJECT);
        JsonNode content = objectMapper.readTree(body).path("data").path("content");
        assertThat(uuids(content)).containsExactly(nairobi.toString());
        assertThat(content.get(0).path("distance_band").asText()).isEqualTo("<2 km");
        assertThat(content.get(0).path("latitude").decimalValue()).isEqualByComparingTo("-1.29");
        assertThat(content.get(0).path("longitude").decimalValue()).isEqualByComparingTo("36.82");
        assertThat(content.get(0).has("location_search_opt_in")).isFalse();
        assertNoPreciseLocation(body);

        // About 153 km east: outside the radius.
        assertThat(uuids(list("/api/v1/instructors?near=-1.29,38.20&radius_km=5", OUTSIDER_SUBJECT))).isEmpty();
        // With q the match must satisfy both the text and the radius.
        assertThat(uuids(list("/api/v1/instructors?q=kubernetes&near=-1.30,36.83", OUTSIDER_SUBJECT)))
                .containsExactly(nairobi.toString());
        assertThat(uuids(list("/api/v1/instructors?q=astrophysics&near=-1.30,36.83", OUTSIDER_SUBJECT))).isEmpty();

        // The owner's own row carries the opt-in flag and is still rounded.
        String ownBody = body("/api/v1/instructors?near=-1.30,36.83", NAIROBI_SUBJECT);
        assertThat(objectMapper.readTree(ownBody).path("data").path("content").get(0)
                .path("location_search_opt_in").asBoolean()).isTrue();
        assertNoPreciseLocation(ownBody);

        // Global search: same rule, a band, and no coordinates at all.
        String globalBody = body("/api/v1/search/instructors?near=-1.30,36.83&radius_km=5", OUTSIDER_SUBJECT);
        JsonNode globalHits = objectMapper.readTree(globalBody).path("data").path("content");
        assertThat(uuids(globalHits)).containsExactly(nairobi.toString());
        assertThat(globalHits.get(0).path("distance_band").asText()).isEqualTo("<2 km");
        assertNoPreciseLocation(globalBody);
        assertThat(globalBody).doesNotContain("\"latitude\"", "\"lat\"", "\"lng\"");

        // Opting out drops the profile from near-me.
        mockMvc.perform(put("/api/v1/instructors/{uuid}/location-search", nairobi).with(jwt(NAIROBI_SUBJECT))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.location_search_opt_in").value(false));
        awaitTrue(() -> {
            try {
                return uuids(list("/api/v1/instructors?near=-1.30,36.83", OUTSIDER_SUBJECT)).isEmpty();
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        });
    }

    @Test
    @DisplayName("The radius is clamped to 2-100 km")
    void radiusIsClamped() throws Exception {
        UUID nairobi = optedInNairobiInstructor();

        // About 1.1 km away: a 0.5 km radius is raised to 2 km, so the instructor is found.
        assertThat(uuids(list("/api/v1/instructors?near=-1.29,36.83&radius_km=0.5", OUTSIDER_SUBJECT)))
                .containsExactly(nairobi.toString());
        // About 44 km away: within the 100 km cap.
        assertThat(uuids(list("/api/v1/instructors?near=-1.29,37.22&radius_km=5000", OUTSIDER_SUBJECT)))
                .containsExactly(nairobi.toString());
        // About 153 km away: a 5000 km radius is capped at 100 km.
        assertThat(uuids(list("/api/v1/instructors?near=-1.29,38.20&radius_km=5000", OUTSIDER_SUBJECT))).isEmpty();
        // The default is 10 km.
        assertThat(uuids(list("/api/v1/instructors?near=-1.29,37.22", OUTSIDER_SUBJECT))).isEmpty();
    }

    @Test
    @DisplayName("near is redacted in the audit log, only the owner may opt in, and near-me needs search")
    void nearIsRedactedAndGuarded() throws Exception {
        UUID nairobi = optedInNairobiInstructor();
        list("/api/v1/instructors?near=-1.2987,36.8321&radius_km=5", OUTSIDER_SUBJECT);
        awaitTrue(() -> !jdbc.queryForList("SELECT query_string FROM request_audit_log "
                + "WHERE request_uri = '/api/v1/instructors' AND query_string LIKE '%radius_km=5%'", String.class).isEmpty());
        List<String> logged = jdbc.queryForList("SELECT query_string FROM request_audit_log "
                + "WHERE request_uri = '/api/v1/instructors' AND query_string LIKE '%radius_km=5%'", String.class);
        assertThat(logged).allSatisfy(query -> assertThat(query)
                .contains("near=[redacted:")
                .doesNotContain("1.29", "36.8"));

        // Only the owner toggles the opt-in - not even a platform admin.
        mockMvc.perform(put("/api/v1/instructors/{uuid}/location-search", nairobi).with(jwt(ADMIN_SUBJECT))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false}"))
                .andExpect(status().isForbidden());
        // The flag is on the owner's own profile only.
        mockMvc.perform(get("/api/v1/instructors/{uuid}", nairobi).with(jwt(NAIROBI_SUBJECT)))
                .andExpect(jsonPath("$.data.location_search_opt_in").value(true));
        mockMvc.perform(get("/api/v1/instructors/{uuid}", nairobi).with(jwt(OUTSIDER_SUBJECT)))
                .andExpect(jsonPath("$.data.location_search_opt_in").doesNotExist());
        mockMvc.perform(get("/api/v1/instructors/{uuid}", nairobi).with(jwt(ADMIN_SUBJECT)))
                .andExpect(jsonPath("$.data.location_search_opt_in").doesNotExist());

        // The people index never takes near; a malformed near is a 400.
        mockMvc.perform(get("/api/v1/search/people?near=-1.29,36.82").with(jwt(ADMIN_SUBJECT)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/instructors?near=somewhere").with(jwt(OUTSIDER_SUBJECT)))
                .andExpect(status().isBadRequest());

        searchProperties.getReadEnabled().put(InstructorSearchSource.INDEX, false);
        try {
            mockMvc.perform(get("/api/v1/instructors?near=-1.29,36.82").with(jwt(OUTSIDER_SUBJECT)))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.message").value("Search is unavailable"));
        } finally {
            searchProperties.getReadEnabled().put(InstructorSearchSource.INDEX, true);
        }
    }

    /** A verified Nairobi instructor who opts in through the API as the owner. */
    private UUID optedInNairobiInstructor() throws Exception {
        UUID userUuid = user(NAIROBI_SUBJECT, "wanjiku@test.local", "Wanjiku", "Mwangi");
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO instructors (uuid, user_uuid, full_name, location_name, lat, long, "
                        + "admin_verified, bio, professional_headline, created_by) "
                        + "VALUES (?, ?, 'ignored', 'Nairobi', ?, ?, TRUE, 'Teaches cloud', 'Cloud trainer', 'test')",
                uuid, userUuid, new BigDecimal(NAIROBI_LAT), new BigDecimal(NAIROBI_LNG));
        skill(uuid, "Kubernetes", "EXPERT");
        mockMvc.perform(put("/api/v1/instructors/{uuid}/location-search", uuid).with(jwt(NAIROBI_SUBJECT))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.location_search_opt_in").value(true));
        rebuilder.rebuild(InstructorSearchSource.INDEX);
        return uuid;
    }

    /**
     * No JSON number finer than 2 decimals, the stored coordinates nowhere in the text, and no raw
     * distance or engine geo field.
     */
    private void assertNoPreciseLocation(String body) throws Exception {
        List<String> fine = new ArrayList<>();
        collectFineNumbers(objectMapper.readTree(body), fine);
        assertThat(fine).as("numbers with 3+ decimals in %s", body).isEmpty();
        assertThat(body).doesNotContain("1.292066", "36.821946", "1.2920", "36.8219");
        assertThat(body).doesNotContain("_geo", "distance_m", "\"distance\"", "geo_distance");
    }

    private static void collectFineNumbers(JsonNode node, List<String> fine) {
        if (node.isNumber() && FINE_NUMBER.matcher(node.decimalValue().toPlainString()).matches()) {
            fine.add(node.asText());
        }
        node.forEach(child -> collectFineNumbers(child, fine));
    }

    private String body(String url, String subject) throws Exception {
        return mockMvc.perform(get(url).with(jwt(subject)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private JsonNode hits(String url, String subject) throws Exception {
        return objectMapper.readTree(body(url, subject)).path("data").path("content");
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
