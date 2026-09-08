package apps.sarafrika.elimika.course.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises {@code GET /api/v1/courses/{uuid}/trainers} over real HTTP against a real database.
 * <p>
 * The claim under test is not "the rate card is blank for the wrong caller" but "the rate card is
 * <em>not there</em>". Those are different guarantees and only the second one survives contact with
 * a client that iterates keys, a proxy that logs bodies, or a future field added next to the
 * blanked one. So the central assertion reads the raw response body and requires the string
 * {@code rate_card} to appear nowhere in it — a JSON-path assertion on one row would pass even if a
 * second trainer leaked.
 * <p>
 * The other half is the sort surface. An endpoint that will order by any column it is asked for
 * hands back the values it refuses to print, one comparison at a time, so the allow-list is tested
 * as a rejection rather than as a silent no-op.
 */
// The container dies with this class; a cached context must not outlive it.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("Course trainer directory (end-to-end)")
class CourseTrainerDirectoryIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
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
        // Flyway builds the real schema. Hibernate's validator additionally sees @Entity classes
        // declared inside other test classes, which have no table, so leave validation to Flyway.
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    private static final String CREATOR_SUBJECT = "keycloak-creator";
    private static final String ORG_MEMBER_SUBJECT = "keycloak-org-member";
    private static final String ADMIN_SUBJECT = "keycloak-admin";
    private static final String OUTSIDER_SUBJECT = "keycloak-outsider";

    private static final String ORGANISATION_NAME = "Westlands Training Institute";
    private static final String INSTRUCTOR_NAME = "Amina Otieno";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    /** Never invoked: the jwt() post-processor sets the SecurityContext directly. Mocked only so
     *  the resource server does not fetch Keycloak's JWKS at boot. */
    @MockBean private JwtDecoder jwtDecoder;

    private UUID courseUuid;
    private UUID organisationUuid;
    private UUID approvedInstructorUuid;

    @BeforeEach
    void seed() {
        clean();

        UUID creatorUserUuid = user(CREATOR_SUBJECT, "creator@test.local", "Cyrus", "Waweru");
        UUID orgMemberUserUuid = user(ORG_MEMBER_SUBJECT, "orgmember@test.local", "Njeri", "Kamau");
        UUID adminUserUuid = user(ADMIN_SUBJECT, "admin@test.local", "Platform", "Admin");
        user(OUTSIDER_SUBJECT, "outsider@test.local", "Passing", "Stranger");

        UUID instructorUserUuid = user("keycloak-instructor", "instructor@test.local", "Amina", "Otieno");
        UUID pendingUserUuid = user("keycloak-pending", "pending@test.local", "Brian", "Kimani");

        grantGlobalDomain(adminUserUuid, "admin");
        grantGlobalDomain(orgMemberUserUuid, "organisation_user");

        UUID courseCreatorUuid = courseCreator(creatorUserUuid);
        courseUuid = course("Welding levels 1-3", courseCreatorUuid);

        organisationUuid = organisation(ORGANISATION_NAME, "Westlands");
        grantOrganisationDomain(orgMemberUserUuid, organisationUuid, "organisation_user");

        approvedInstructorUuid = instructor(instructorUserUuid, "Kisumu", true);
        UUID pendingInstructorUuid = instructor(pendingUserUuid, "Nakuru", false);

        approvedApplication("organisation", organisationUuid, "2026-02-14 09:30:00", new BigDecimal("4200.0000"));
        approvedApplication("instructor", approvedInstructorUuid, "2026-03-01 11:00:00", new BigDecimal("3100.0000"));
        pendingApplication("instructor", pendingInstructorUuid);

        // Two active organisation classes and one that has been retired: the count is of live work.
        classDefinition(courseUuid, organisationUuid, pendingInstructorUuid, true);
        classDefinition(courseUuid, organisationUuid, pendingInstructorUuid, true);
        classDefinition(courseUuid, organisationUuid, pendingInstructorUuid, false);
        // One class the approved instructor runs in their own name, outside any organisation.
        classDefinition(courseUuid, null, approvedInstructorUuid, true);
    }

    // ===== THE GUARANTEE: NOT APPROVED MEANS NOT TRANSMITTED =====

    @Test
    @DisplayName("An approved organisation's member gets JSON with no rate_card key anywhere")
    void approvedOrganisationNeverSeesARateCardKey() throws Exception {
        String body = mockMvc.perform(get(trainersUrl()).with(jwt(ORG_MEMBER_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trainers.length()").value(2))
                // The directory itself is readable: this is a redaction, not a refusal.
                .andExpect(jsonPath("$.data.trainers[?(@.display_name == '" + ORGANISATION_NAME + "')]").exists())
                // The creator's own queue depth is absent too, rather than reported as zero.
                .andExpect(jsonPath("$.data.pending_count").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body)
                .as("an approved organisation's directory JSON must contain no rate_card key at all")
                .doesNotContain("rate_card");
        assertThat(body)
                .as("nor any of the figures a rate card is made of")
                .doesNotContain("hourly_rate")
                .doesNotContain("4200")
                .doesNotContain("3100");
    }

    @Test
    @DisplayName("A signed-in stranger reads the directory, also without rate cards")
    void outsiderReadsTheDirectoryWithoutRateCards() throws Exception {
        String body = mockMvc.perform(get(trainersUrl()).with(jwt(OUTSIDER_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trainers.length()").value(2))
                .andExpect(jsonPath("$.data.pending_count").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).doesNotContain("rate_card");
    }

    @Test
    @DisplayName("The course creator sees every rate card and the pending queue")
    void creatorSeesRateCardsAndPendingCount() throws Exception {
        mockMvc.perform(get(trainersUrl()).with(jwt(CREATOR_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trainers.length()").value(2))
                .andExpect(jsonPath("$.data.pending_count").value(1))
                .andExpect(jsonPath("$.data.trainers[?(@.applicant_type == 'organisation')].rate_card").exists())
                .andExpect(jsonPath("$.data.trainers[?(@.applicant_type == 'instructor')].rate_card").exists());
    }

    @Test
    @DisplayName("A platform admin sees them too")
    void platformAdminSeesRateCards() throws Exception {
        mockMvc.perform(get(trainersUrl()).with(jwt(ADMIN_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pending_count").value(1))
                .andExpect(jsonPath("$.data.trainers[0].rate_card").exists());
    }

    // ===== THE DIRECTORY ITSELF =====

    @Test
    @DisplayName("Each row names the trainer, where they work and how much live work they have")
    void rowsCarryIdentityLocationAndClassCount() throws Exception {
        mockMvc.perform(get(trainersUrl() + "?sort=display_name,asc").with(jwt(ORG_MEMBER_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trainers[0].applicant_type").value("instructor"))
                .andExpect(jsonPath("$.data.trainers[0].applicant_uuid").value(approvedInstructorUuid.toString()))
                .andExpect(jsonPath("$.data.trainers[0].display_name").value(INSTRUCTOR_NAME))
                .andExpect(jsonPath("$.data.trainers[0].location").value("Kisumu · verified instructor"))
                .andExpect(jsonPath("$.data.trainers[0].approved_at").exists())
                .andExpect(jsonPath("$.data.trainers[0].active_class_count").value(1))
                .andExpect(jsonPath("$.data.trainers[1].applicant_type").value("organisation"))
                .andExpect(jsonPath("$.data.trainers[1].applicant_uuid").value(organisationUuid.toString()))
                .andExpect(jsonPath("$.data.trainers[1].display_name").value(ORGANISATION_NAME))
                // The town, never the coordinates, and never the retired third class.
                .andExpect(jsonPath("$.data.trainers[1].location").value("Westlands"))
                .andExpect(jsonPath("$.data.trainers[1].active_class_count").value(2));
    }

    @Test
    @DisplayName("Only approved applicants appear — the pending one does not")
    void pendingApplicantsAreNotOnTheDeliveryList() throws Exception {
        mockMvc.perform(get(trainersUrl()).with(jwt(CREATOR_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trainers[?(@.display_name == 'Brian Kimani')]").doesNotExist());
    }

    // ===== THE SORT SURFACE IS NOT AN ORACLE =====

    @Test
    @DisplayName("Sorting by a rate column is rejected, not silently ignored")
    void sortingByARateColumnIsRejected() throws Exception {
        mockMvc.perform(get(trainersUrl() + "?sort=privateOnlineHourlyRate,desc").with(jwt(ORG_MEMBER_SUBJECT)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get(trainersUrl() + "?sort=rate_card").with(jwt(ORG_MEMBER_SUBJECT)))
                .andExpect(status().isBadRequest());

        // The creator may see the rates, but ordering by them is still not a supported query.
        mockMvc.perform(get(trainersUrl() + "?sort=group_inperson_daily_rate").with(jwt(CREATOR_SUBJECT)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("The three published fields are sortable, in both directions")
    void publishedFieldsAreSortable() throws Exception {
        mockMvc.perform(get(trainersUrl() + "?sort=display_name,desc").with(jwt(ORG_MEMBER_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trainers[0].display_name").value(ORGANISATION_NAME));

        mockMvc.perform(get(trainersUrl() + "?sort=active_class_count,desc").with(jwt(ORG_MEMBER_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trainers[0].active_class_count").value(2));

        mockMvc.perform(get(trainersUrl() + "?sort=approved_at,asc").with(jwt(ORG_MEMBER_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trainers[0].display_name").value(ORGANISATION_NAME));
    }

    @Test
    @DisplayName("An unknown course is a 404, not an empty list")
    void unknownCourseIsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/courses/" + UUID.randomUUID() + "/trainers").with(jwt(CREATOR_SUBJECT)))
                .andExpect(status().isNotFound());
    }

    // ===== TEST PLUMBING =====

    private String trainersUrl() {
        return "/api/v1/courses/" + courseUuid + "/trainers";
    }

    private RequestPostProcessor jwt(String subject) {
        return org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                .jwt().jwt(builder -> builder.subject(subject).claim("sub", subject));
    }

    private void clean() {
        jdbc.execute("TRUNCATE class_definitions, course_training_applications, courses, course_creators, "
                + "instructors, user_organisation_domain_mapping, user_domain_mapping, organisation, users "
                + "RESTART IDENTITY CASCADE");
    }

    private UUID user(String keycloakId, String email, String firstName, String lastName) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO users (uuid, user_no, first_name, last_name, email, keycloak_id, created_by) "
                        + "VALUES (?, ?, ?, ?, ?, ?, 'test')",
                uuid, String.format("%09d", Math.abs(uuid.hashCode()) % 1000000000),
                firstName, lastName, email, keycloakId);
        return uuid;
    }

    private void grantGlobalDomain(UUID userUuid, String domainName) {
        jdbc.update("INSERT INTO user_domain_mapping (user_uuid, domain_uuid) "
                + "VALUES (?, (SELECT uuid FROM user_domain WHERE domain_name = ?))", userUuid, domainName);
    }

    private void grantOrganisationDomain(UUID userUuid, UUID organisationUuid, String domainName) {
        jdbc.update("INSERT INTO user_organisation_domain_mapping "
                        + "(uuid, user_uuid, organisation_uuid, domain_uuid, created_by) "
                        + "VALUES (?, ?, ?, (SELECT uuid FROM user_domain WHERE domain_name = ?), 'test')",
                UUID.randomUUID(), userUuid, organisationUuid, domainName);
    }

    private UUID courseCreator(UUID userUuid) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO course_creators (uuid, user_uuid, full_name, created_by) "
                + "VALUES (?, ?, 'Course Creator', 'test')", uuid, userUuid);
        return uuid;
    }

    private UUID course(String name, UUID courseCreatorUuid) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO courses (uuid, name, course_creator_uuid, status, active, created_by) "
                + "VALUES (?, ?, ?, 'published', true, 'test')", uuid, name, courseCreatorUuid);
        return uuid;
    }

    private UUID organisation(String name, String town) {
        UUID uuid = UUID.randomUUID();
        // Coordinates are set deliberately: the endpoint must publish the town and never these.
        jdbc.update("INSERT INTO organisation (uuid, name, location, country, lat, long, created_by) "
                + "VALUES (?, ?, ?, 'Kenya', -1.264400, 36.803400, 'test')", uuid, name, town);
        return uuid;
    }

    private UUID instructor(UUID userUuid, String locationName, boolean adminVerified) {
        UUID uuid = UUID.randomUUID();
        // full_name is derived from the user row by a database trigger, so it is not passed here.
        jdbc.update("INSERT INTO instructors (uuid, user_uuid, full_name, location_name, lat, long, "
                        + "admin_verified, created_by) "
                        + "VALUES (?, ?, 'ignored', ?, -0.091700, 34.767900, ?, 'test')",
                uuid, userUuid, locationName, adminVerified);
        return uuid;
    }

    private void approvedApplication(String applicantType, UUID applicantUuid, String reviewedAt, BigDecimal rate) {
        jdbc.update("INSERT INTO course_training_applications (uuid, course_uuid, applicant_type, applicant_uuid, "
                        + "status, rate_currency, private_online_hourly_rate, private_inperson_hourly_rate, "
                        + "group_online_hourly_rate, group_inperson_hourly_rate, reviewed_at, created_by) "
                        + "VALUES (?, ?, ?, ?, 'approved', 'KES', ?, ?, ?, ?, ?::timestamp, 'test')",
                UUID.randomUUID(), courseUuid, applicantType, applicantUuid,
                rate, rate, rate, rate, reviewedAt);
    }

    private void pendingApplication(String applicantType, UUID applicantUuid) {
        jdbc.update("INSERT INTO course_training_applications (uuid, course_uuid, applicant_type, applicant_uuid, "
                        + "status, rate_currency, private_online_hourly_rate, private_inperson_hourly_rate, "
                        + "group_online_hourly_rate, group_inperson_hourly_rate, created_by) "
                        + "VALUES (?, ?, ?, ?, 'pending', 'KES', 1000, 1000, 1000, 1000, 'test')",
                UUID.randomUUID(), courseUuid, applicantType, applicantUuid);
    }

    private void classDefinition(UUID courseUuid, UUID organisationUuid, UUID instructorUuid, boolean active) {
        // The registration window is mandatory on the table. The directory counts active classes and
        // never consults the window, so it is opened wide around today.
        jdbc.update("INSERT INTO class_definitions (uuid, title, default_instructor_uuid, organisation_uuid, "
                        + "course_uuid, default_start_time, default_end_time, class_visibility, session_format, "
                        + "is_active, registration_period_start_date, registration_period_end_date, created_by) "
                        + "VALUES (?, 'Welding cohort', ?, ?, ?, '2026-04-01 09:00:00'::timestamp, "
                        + "'2026-04-01 10:30:00'::timestamp, 'PUBLIC', 'GROUP', ?, "
                        + "(now() AT TIME ZONE 'UTC')::date - 30, (now() AT TIME ZONE 'UTC')::date + 365, 'test')",
                UUID.randomUUID(), instructorUuid, organisationUuid, courseUuid, active);
    }
}
