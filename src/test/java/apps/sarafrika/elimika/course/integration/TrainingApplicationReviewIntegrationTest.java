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

/** Reviewing training applications over HTTP against the real schema: rate updates, floor flags, history and offers. */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("Training application review (end-to-end)")
class TrainingApplicationReviewIntegrationTest {

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
    private static final String MANAGER = "keycloak-manager";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private JwtDecoder jwtDecoder;

    private UUID courseUuid;
    private UUID applicationUuid;

    @BeforeEach
    void seed() {
        jdbc.execute("TRUNCATE training_application_events, training_application_venues, "
                + "training_application_requirement_answers, course_training_rate_updates, course_training_applications, "
                + "course_training_requirements, organisation_resources, training_branches, courses, course_creators, "
                + "instructors, user_organisation_domain_mapping, user_domain_mapping, organisation, users RESTART IDENTITY CASCADE");

        UUID creatorUser = user(CREATOR, "creator@test.local", "Cyrus", "Waweru");
        UUID instructorUser = user(INSTRUCTOR, "instructor@test.local", "Amina", "Otieno");
        user(OUTSIDER, "outsider@test.local", "Passing", "Stranger");
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
    @DisplayName("the course creator sees floor flags on a legacy card below the minimum; the applicant does not")
    void floorFlagsForTheOwnerOnly() throws Exception {
        jdbc.update("UPDATE courses SET minimum_training_fee = 3000 WHERE uuid = ?", courseUuid);

        mockMvc.perform(get(applicationUrl()).with(jwt(CREATOR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rate_card.group_online_hourly_rate").value(2500.0))
                .andExpect(jsonPath("$.data.rate_floor_flags.minimum_training_fee").value(3000.0))
                .andExpect(jsonPath("$.data.rate_floor_flags.group_online_hourly_rate").value(true))
                .andExpect(jsonPath("$.data.rate_floor_flags.group_online_session_rate").value(false))
                .andExpect(jsonPath("$.data.rate_floor_flags.private_online_hourly_rate").value(false));
        mockMvc.perform(get("/api/v1/courses/" + courseUuid + "/training-applications").with(jwt(CREATOR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].rate_floor_flags.group_online_hourly_rate").value(true));

        mockMvc.perform(get(applicationUrl()).with(jwt(INSTRUCTOR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rate_card.group_online_hourly_rate").value(2500.0))
                .andExpect(jsonPath("$.data.rate_floor_flags").doesNotExist());
        String directory = mockMvc.perform(get("/api/v1/courses/" + courseUuid + "/training-applications").with(jwt(OUTSIDER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andReturn().getResponse().getContentAsString();
        assertThat(directory).doesNotContain("hourly_rate").contains("\"rate_floor_flags\":null");
    }

    @Test
    @DisplayName("the creator's first open is recorded once, and the history lists every step newest first")
    void firstOpenAndHistory() throws Exception {
        mockMvc.perform(get(applicationUrl()).with(jwt(INSTRUCTOR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.first_opened_at").doesNotExist());
        assertThat(openEvents()).as("the applicant's own read is not an open by the creator").isZero();

        mockMvc.perform(get(applicationUrl()).with(jwt(CREATOR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.first_opened_at").isNotEmpty());
        String firstOpenedAt = objectMapper.readTree(mockMvc.perform(get(applicationUrl()).with(jwt(CREATOR)))
                .andReturn().getResponse().getContentAsString()).at("/data/first_opened_at").asText();
        assertThat(openEvents()).isEqualTo(1);
        mockMvc.perform(get(applicationUrl()).with(jwt(INSTRUCTOR)))
                .andExpect(jsonPath("$.data.first_opened_at").value(firstOpenedAt));

        String updateUuid = objectMapper.readTree(mockMvc.perform(post(updatesUrl()).with(jwt(INSTRUCTOR))
                        .contentType(MediaType.APPLICATION_JSON).content(proposal("3000")))
                .andReturn().getResponse().getContentAsString()).at("/data/uuid").asText();
        mockMvc.perform(post(updatesUrl() + "/" + updateUuid + "?action=approve").with(jwt(CREATOR))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"review_notes\":\"Fair\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get(applicationUrl() + "/history").with(jwt(INSTRUCTOR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].event_type").value("rates_update_approved"))
                .andExpect(jsonPath("$.data[0].note").value("Fair"))
                .andExpect(jsonPath("$.data[0].actor_name").value("Cyrus Waweru"))
                .andExpect(jsonPath("$.data[0].application_type").value("course"))
                .andExpect(jsonPath("$.data[1].event_type").value("rates_update_submitted"))
                .andExpect(jsonPath("$.data[1].note").value("Costs rose"))
                .andExpect(jsonPath("$.data[2].event_type").value("opened_by_creator"))
                .andExpect(jsonPath("$.data[2].actor_uuid").isNotEmpty())
                .andExpect(jsonPath("$.data[2].created_date").isNotEmpty());
        mockMvc.perform(get(applicationUrl() + "/history").with(jwt(CREATOR)))
                .andExpect(status().isOk());
        mockMvc.perform(get(applicationUrl() + "/history").with(jwt(OUTSIDER)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("an organisation offers its venues and answers the requirements, and both round-trip")
    void venuesAndRequirementAnswersRoundTrip() throws Exception {
        UUID managerUser = user(MANAGER, "manager@test.local", "Njeri", "Kamau");
        UUID organisationUuid = organisation("Westlands Institute");
        grantOrganisationDomain(managerUser, organisationUuid, "organisation_user");
        UUID branchUuid = UUID.randomUUID();
        jdbc.update("INSERT INTO training_branches (uuid, organisation_uuid, branch_name, created_by) VALUES (?, ?, 'Westlands', 'test')",
                branchUuid, organisationUuid);
        UUID venueUuid = resource(organisationUuid, branchUuid, "VENUE", "Lab 1");
        UUID poolUuid = resource(organisationUuid, branchUuid, "EQUIPMENT_POOL", "Laptops");
        UUID foreignVenueUuid = resource(organisation("Elsewhere College"), null, "VENUE", "Hall");
        UUID requirementUuid = UUID.randomUUID();
        jdbc.update("INSERT INTO course_training_requirements (uuid, course_uuid, requirement_type, name, created_by) "
                + "VALUES (?, ?, 'equipment', 'Welding booth', 'test')", requirementUuid, courseUuid);
        String url = "/api/v1/courses/" + courseUuid + "/training-applications";

        mockMvc.perform(post(url).with(jwt(MANAGER)).contentType(MediaType.APPLICATION_JSON)
                        .content(organisationApplication(organisationUuid, foreignVenueUuid, requirementUuid, "\"hire\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("does not belong to the applicant organisation")));
        mockMvc.perform(post(url).with(jwt(MANAGER)).contentType(MediaType.APPLICATION_JSON)
                        .content(organisationApplication(organisationUuid, poolUuid, requirementUuid, "\"hire\"")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(url).with(jwt(MANAGER)).contentType(MediaType.APPLICATION_JSON)
                        .content(organisationApplication(organisationUuid, venueUuid, requirementUuid, "null")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(url).with(jwt(MANAGER)).contentType(MediaType.APPLICATION_JSON)
                        .content(organisationApplication(organisationUuid, venueUuid, UUID.randomUUID(), "\"hire\"")))
                .andExpect(status().isBadRequest());

        String created = mockMvc.perform(post(url).with(jwt(MANAGER)).contentType(MediaType.APPLICATION_JSON)
                        .content(organisationApplication(organisationUuid, venueUuid, requirementUuid, "\"hire\"")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.offered_venues.length()").value(1))
                .andExpect(jsonPath("$.data.offered_venues[0].resource_uuid").value(venueUuid.toString()))
                .andExpect(jsonPath("$.data.offered_venues[0].name").value("Lab 1"))
                .andExpect(jsonPath("$.data.offered_venues[0].seat_capacity").value(24))
                .andExpect(jsonPath("$.data.offered_venues[0].location_name").value("Block B"))
                .andExpect(jsonPath("$.data.offered_venues[0].branch_uuid").value(branchUuid.toString()))
                .andExpect(jsonPath("$.data.offered_venues[0].branch_name").value("Westlands"))
                .andExpect(jsonPath("$.data.requirement_answers[0].requirement_uuid").value(requirementUuid.toString()))
                .andExpect(jsonPath("$.data.requirement_answers[0].requirement_name").value("Welding booth"))
                .andExpect(jsonPath("$.data.requirement_answers[0].has_it").value(false))
                .andExpect(jsonPath("$.data.requirement_answers[0].acquisition").value("hire"))
                .andReturn().getResponse().getContentAsString();
        String applicationUrl = url + "/" + objectMapper.readTree(created).at("/data/uuid").asText();

        mockMvc.perform(get(applicationUrl).with(jwt(CREATOR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.offered_venues[0].name").value("Lab 1"))
                .andExpect(jsonPath("$.data.requirement_answers[0].acquisition").value("hire"));

        String keepOffers = """
                {"rate_card": {"group_online_hourly_rate": 2600, "group_online_session_rate": 2600, "group_online_daily_rate": 2600}}
                """;
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(applicationUrl)
                        .with(jwt(MANAGER)).contentType(MediaType.APPLICATION_JSON).content(keepOffers))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.offered_venues.length()").value(1))
                .andExpect(jsonPath("$.data.requirement_answers.length()").value(1));
        String clearOffers = """
                {"rate_card": {"group_online_hourly_rate": 2600, "group_online_session_rate": 2600, "group_online_daily_rate": 2600},
                 "offered_venue_uuids": [], "requirement_answers": []}
                """;
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(applicationUrl)
                        .with(jwt(MANAGER)).contentType(MediaType.APPLICATION_JSON).content(clearOffers))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.offered_venues.length()").value(0))
                .andExpect(jsonPath("$.data.requirement_answers.length()").value(0));
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

    private String organisationApplication(UUID organisationUuid, UUID venueUuid, UUID requirementUuid, String acquisition) {
        return """
                {"applicant_type": "organisation", "applicant_uuid": "%s",
                 "rate_card": {"group_online_hourly_rate": 2500, "group_online_session_rate": 2500, "group_online_daily_rate": 2500},
                 "offered_venue_uuids": ["%s"],
                 "requirement_answers": [{"requirement_uuid": "%s", "has_it": false, "acquisition": %s}]}
                """.formatted(organisationUuid, venueUuid, requirementUuid, acquisition);
    }

    private UUID organisation(String name) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO organisation (uuid, name, location, country, created_by) VALUES (?, ?, 'Nairobi', 'Kenya', 'test')",
                uuid, name);
        return uuid;
    }

    private UUID resource(UUID organisationUuid, UUID branchUuid, String type, String name) {
        UUID uuid = UUID.randomUUID();
        boolean venue = "VENUE".equals(type);
        jdbc.update("INSERT INTO organisation_resources (uuid, organisation_uuid, branch_uuid, resource_type, name, seat_capacity, "
                        + "total_quantity, location_name, created_by) VALUES (?, ?, ?, ?, ?, ?, ?, 'Block B', 'test')",
                uuid, organisationUuid, branchUuid, type, name, venue ? 24 : null, venue ? null : 10);
        return uuid;
    }

    private void grantOrganisationDomain(UUID userUuid, UUID organisationUuid, String domainName) {
        jdbc.update("INSERT INTO user_organisation_domain_mapping (uuid, user_uuid, organisation_uuid, domain_uuid, created_by) "
                        + "VALUES (?, ?, ?, (SELECT uuid FROM user_domain WHERE domain_name = ?), 'test')",
                UUID.randomUUID(), userUuid, organisationUuid, domainName);
    }

    private int openEvents() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM training_application_events WHERE application_uuid = ? "
                + "AND event_type = 'OPENED_BY_CREATOR'", Integer.class, applicationUuid);
    }

    private UUID user(String keycloakId, String email, String firstName, String lastName) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO users (uuid, user_no, first_name, last_name, email, keycloak_id, created_by) "
                        + "VALUES (?, ?, ?, ?, ?, ?, 'test')",
                uuid, String.format("%09d", Math.abs(uuid.hashCode()) % 1000000000), firstName, lastName, email, keycloakId);
        return uuid;
    }

    private void grantGlobalDomain(UUID userUuid, String domainName) {
        jdbc.update("INSERT INTO user_domain_mapping (user_uuid, domain_uuid) "
                + "VALUES (?, (SELECT uuid FROM user_domain WHERE domain_name = ?))", userUuid, domainName);
    }
}
