package apps.sarafrika.elimika.shared.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import apps.sarafrika.elimika.shared.tracking.discovery.DiscoveryImpression;
import apps.sarafrika.elimika.shared.tracking.discovery.DiscoveryTracker;
import apps.sarafrika.elimika.shared.tracking.service.DiscoveryEventPurgeJob;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("Discovery tracking (end-to-end)")
class DiscoveryEventIntegrationTest {

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

    private static final String LEARNER = "keycloak-discovery-learner";
    private static final String OTHER = "keycloak-discovery-other";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private DiscoveryTracker tracker;
    @Autowired private DiscoveryEventPurgeJob purgeJob;

    @MockBean private JwtDecoder jwtDecoder;

    private UUID learner;
    private UUID other;
    private final UUID recommendation = UUID.randomUUID();
    private final UUID course = UUID.randomUUID();

    @BeforeEach
    void seed() {
        jdbc.execute("TRUNCATE discovery_events RESTART IDENTITY");
        jdbc.update("DELETE FROM users WHERE keycloak_id IN (?, ?)", LEARNER, OTHER);
        learner = user(LEARNER, "discovery-learner@test.local");
        other = user(OTHER, "discovery-other@test.local");
        tracker.recordImpressions(learner, "course_recommendations", recommendation, "v1.0",
                List.of(new DiscoveryImpression("course", course, 0, List.of("SKILL_GAP", "POPULAR")),
                        new DiscoveryImpression("course", UUID.randomUUID(), 1, List.of())));
    }

    @Test
    @DisplayName("Impressions are recorded server-side with surface, reasons and model version")
    void recordsImpressions() {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT user_uuid, surface, item_type, position, event_type, array_to_string(reason_codes, ',') AS reasons, "
                        + "model_version, created_at FROM discovery_events ORDER BY position");

        assertThat(rows).hasSize(2);
        assertThat(rows.getFirst())
                .containsEntry("user_uuid", learner)
                .containsEntry("surface", "course_recommendations")
                .containsEntry("event_type", "IMPRESSION")
                .containsEntry("reasons", "SKILL_GAP,POPULAR")
                .containsEntry("model_version", "v1.0");
        assertThat(rows.getFirst().get("created_at")).isNotNull();
    }

    @Test
    @DisplayName("A click is accepted with 202 and inherits the impression's context; the user comes from the token")
    void recordsClick() throws Exception {
        mockMvc.perform(post("/api/v1/discovery/events").with(jwt(LEARNER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("CLICK", course)))
                .andExpect(status().isAccepted());

        Map<String, Object> click = jdbc.queryForMap(
                "SELECT user_uuid, surface, position, array_to_string(reason_codes, ',') AS reasons, model_version "
                        + "FROM discovery_events WHERE event_type = 'CLICK'");
        assertThat(click)
                .containsEntry("user_uuid", learner)
                .containsEntry("surface", "course_recommendations")
                .containsEntry("position", 0)
                .containsEntry("reasons", "SKILL_GAP,POPULAR")
                .containsEntry("model_version", "v1.0");
    }

    @Test
    @DisplayName("An event for an item the caller was never shown is accepted but not stored")
    void dropsUnmatchedEvents() throws Exception {
        mockMvc.perform(post("/api/v1/discovery/events").with(jwt(OTHER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("DISMISS", course)))
                .andExpect(status().isAccepted());
        mockMvc.perform(post("/api/v1/discovery/events").with(jwt(LEARNER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("DISMISS", UUID.randomUUID())))
                .andExpect(status().isAccepted());

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM discovery_events WHERE event_type <> 'IMPRESSION'",
                Long.class)).isZero();
    }

    @Test
    @DisplayName("Anonymous callers get 401; client-sent impressions and bad bodies get 400")
    void rejectsBadRequests() throws Exception {
        mockMvc.perform(post("/api/v1/discovery/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("CLICK", course)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/discovery/events").with(jwt(LEARNER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("IMPRESSION", course)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/discovery/events").with(jwt(LEARNER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recommendation_id\":\"" + recommendation + "\",\"event_type\":\"CLICK\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Tracking never throws at the recommender, even for invalid input")
    void impressionsNeverThrow() {
        tracker.recordImpressions(learner, "Not A Slug", UUID.randomUUID(), null,
                List.of(new DiscoveryImpression("course", UUID.randomUUID(), 0, List.of("free text query"))));

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM discovery_events", Long.class)).isEqualTo(2);
    }

    @Test
    @DisplayName("The purge removes rows older than 180 days and keeps the rest")
    void purgesOldRows() {
        jdbc.update("UPDATE discovery_events SET created_at = ? WHERE position = 1",
                LocalDateTime.now(ZoneOffset.UTC).minusDays(181));

        long deleted = purgeJob.purgeOlderThan(LocalDateTime.now(ZoneOffset.UTC).minusDays(180));

        assertThat(deleted).isEqualTo(1);
        assertThat(jdbc.queryForList("SELECT position FROM discovery_events", Integer.class)).containsExactly(0);
    }

    private String body(String eventType, UUID item) {
        return """
                {"recommendation_id":"%s","item_uuid":"%s","item_type":"course","event_type":"%s","position":0}
                """.formatted(recommendation, item, eventType);
    }

    private UUID user(String keycloakId, String email) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO users (uuid, user_no, first_name, last_name, email, keycloak_id, created_by) "
                        + "VALUES (?, ?, 'Test', 'User', ?, ?, 'test')",
                uuid, String.format("%09d", Math.abs(uuid.hashCode()) % 1000000000), email, keycloakId);
        return uuid;
    }

    private static RequestPostProcessor jwt(String subject) {
        return SecurityMockMvcRequestPostProcessors.jwt().jwt(builder -> builder.subject(subject).claim("sub", subject));
    }
}
