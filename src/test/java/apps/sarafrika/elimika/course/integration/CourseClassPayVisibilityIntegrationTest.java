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
 * Exercises {@code GET /api/v1/classes/course/{courseUuid}} over real HTTP against a real database.
 * <p>
 * This is the listing a course record page loads for every viewer, so it is the one place where a
 * class's two prices would otherwise be published side by side. {@code sale_price} is what a learner
 * pays and stays; {@code instructor_pay} is what the organisation pays its trainer, and the
 * difference between them is the organisation's margin. Publishing the pair publishes the margin.
 * <p>
 * The claim under test is not "the pay is blank for the wrong caller" but "the pay is <em>not
 * there</em>". Only the second guarantee survives a client that iterates keys or a log that captures
 * bodies, so the central assertions read the raw response body and require the string
 * {@code instructor_pay} — and the figures themselves — to appear nowhere in it. A JSON-path
 * assertion on one row would pass while a second class leaked.
 * <p>
 * Two classes are seeded on purpose: one owned by the organisation and one an independent instructor
 * runs in their own name. Neither party is party to the other's pay, so each privileged caller must
 * come back holding exactly one figure — which is what separates a real projection from a blanket
 * "this caller is important" flag.
 * <p>
 * The last case is the sort surface. An endpoint that will order by any column it is asked for hands
 * back the values it refuses to print, one comparison at a time, so the refusal is tested as a
 * rejection rather than as a silent no-op.
 */
// The container dies with this class; a cached context must not outlive it.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("Instructor pay on the course class list (end-to-end)")
class CourseClassPayVisibilityIntegrationTest {

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

    private static final String ORG_MANAGER_SUBJECT = "keycloak-pay-org-manager";
    private static final String ORG_INSTRUCTOR_SUBJECT = "keycloak-pay-org-instructor";
    private static final String SOLO_INSTRUCTOR_SUBJECT = "keycloak-pay-solo-instructor";
    private static final String ADMIN_SUBJECT = "keycloak-pay-admin";
    private static final String LEARNER_SUBJECT = "keycloak-pay-learner";

    private static final String ORG_CLASS_TITLE = "Welding cohort — institute";
    private static final String SOLO_CLASS_TITLE = "Welding cohort — independent";

    /** The organisation's class: charged at 3000, of which the trainer is paid 2000. */
    private static final String ORG_SALE_PRICE = "3000.00";
    private static final String ORG_INSTRUCTOR_PAY = "2000.00";
    /** The independent instructor's own class, priced so no figure is shared with the other row. */
    private static final String SOLO_SALE_PRICE = "1500.00";
    private static final String SOLO_INSTRUCTOR_PAY = "1250.00";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    /** Never invoked: the jwt() post-processor sets the SecurityContext directly. Mocked only so
     *  the resource server does not fetch Keycloak's JWKS at boot. */
    @MockBean private JwtDecoder jwtDecoder;

    private UUID courseUuid;

    @BeforeEach
    void seed() {
        clean();

        UUID managerUserUuid = user(ORG_MANAGER_SUBJECT, "pay-manager@test.local", "Njeri", "Kamau");
        UUID adminUserUuid = user(ADMIN_SUBJECT, "pay-admin@test.local", "Platform", "Admin");
        user(LEARNER_SUBJECT, "pay-learner@test.local", "Passing", "Stranger");
        UUID orgInstructorUserUuid = user(ORG_INSTRUCTOR_SUBJECT, "pay-teacher@test.local", "Amina", "Otieno");
        UUID soloInstructorUserUuid = user(SOLO_INSTRUCTOR_SUBJECT, "pay-solo@test.local", "Brian", "Kimani");

        grantGlobalDomain(adminUserUuid, "admin");

        UUID courseCreatorUuid = courseCreator(user("keycloak-pay-creator", "pay-creator@test.local", "Cyrus", "Waweru"));
        courseUuid = course("Welding levels 1-3", courseCreatorUuid);

        UUID organisationUuid = organisation("Westlands Training Institute");
        grantOrganisationDomain(managerUserUuid, organisationUuid, "organisation_user");

        UUID orgInstructorUuid = instructor(orgInstructorUserUuid);
        UUID soloInstructorUuid = instructor(soloInstructorUserUuid);

        classDefinition(ORG_CLASS_TITLE, organisationUuid, orgInstructorUuid,
                new BigDecimal(ORG_SALE_PRICE), new BigDecimal(ORG_INSTRUCTOR_PAY));
        classDefinition(SOLO_CLASS_TITLE, null, soloInstructorUuid,
                new BigDecimal(SOLO_SALE_PRICE), new BigDecimal(SOLO_INSTRUCTOR_PAY));
    }

    // ===== THE GUARANTEE: NOT ENTITLED MEANS NOT TRANSMITTED =====

    @Test
    @DisplayName("A learner browsing the course gets prices and no instructor_pay key anywhere")
    void learnerNeverSeesAnInstructorPayKey() throws Exception {
        String body = mockMvc.perform(get(classesUrl()).with(jwt(LEARNER_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                // The catalogue itself is readable: this is a redaction, not a refusal.
                .andExpect(jsonPath("$.data[?(@.class_definition.title == '" + ORG_CLASS_TITLE
                        + "')].class_definition.sale_price").exists())
                .andExpect(jsonPath("$.data[?(@.class_definition.title == '" + SOLO_CLASS_TITLE
                        + "')].class_definition.sale_price").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body)
                .as("the course class list must carry no instructor_pay key at all")
                .doesNotContain("instructor_pay");
        assertThat(body)
                .as("nor the figures themselves, from either class")
                .doesNotContain(ORG_INSTRUCTOR_PAY)
                .doesNotContain(SOLO_INSTRUCTOR_PAY);
        assertThat(body)
                .as("the public price is not what is being withheld")
                .contains(ORG_SALE_PRICE)
                .contains(SOLO_SALE_PRICE);
    }

    @Test
    @DisplayName("A manager of the owning organisation reads its own class's pay, and only its own")
    void owningOrganisationSeesItsOwnPayOnly() throws Exception {
        String body = mockMvc.perform(get(classesUrl()).with(jwt(ORG_MANAGER_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[?(@.class_definition.title == '" + ORG_CLASS_TITLE
                        + "')].class_definition.instructor_pay").exists())
                .andExpect(jsonPath("$.data[?(@.class_definition.title == '" + SOLO_CLASS_TITLE
                        + "')].class_definition.instructor_pay").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains(ORG_INSTRUCTOR_PAY);
        assertThat(body)
                .as("a manager is party to their own organisation's margin, not to a stranger's")
                .doesNotContain(SOLO_INSTRUCTOR_PAY);
    }

    @Test
    @DisplayName("The instructor named on a class reads what they are paid, and nobody else's")
    void assignedInstructorSeesTheirOwnPayOnly() throws Exception {
        String body = mockMvc.perform(get(classesUrl()).with(jwt(SOLO_INSTRUCTOR_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.class_definition.title == '" + SOLO_CLASS_TITLE
                        + "')].class_definition.instructor_pay").exists())
                .andExpect(jsonPath("$.data[?(@.class_definition.title == '" + ORG_CLASS_TITLE
                        + "')].class_definition.instructor_pay").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains(SOLO_INSTRUCTOR_PAY).doesNotContain(ORG_INSTRUCTOR_PAY);
    }

    @Test
    @DisplayName("A platform admin sees both")
    void platformAdminSeesEveryPay() throws Exception {
        String body = mockMvc.perform(get(classesUrl()).with(jwt(ADMIN_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.class_definition.title == '" + ORG_CLASS_TITLE
                        + "')].class_definition.instructor_pay").exists())
                .andExpect(jsonPath("$.data[?(@.class_definition.title == '" + SOLO_CLASS_TITLE
                        + "')].class_definition.instructor_pay").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains(ORG_INSTRUCTOR_PAY).contains(SOLO_INSTRUCTOR_PAY);
    }

    @Test
    @DisplayName("An anonymous caller is not served the catalogue at all")
    void anonymousCallerIsRefused() throws Exception {
        mockMvc.perform(get(classesUrl()))
                .andExpect(status().isUnauthorized());
    }

    // ===== THE SORT SURFACE IS NOT AN ORACLE =====

    @Test
    @DisplayName("Ordering the class catalogue by instructor pay is rejected, not silently ignored")
    void sortingByInstructorPayIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/classes?sort=instructorPay,desc").with(jwt(LEARNER_SUBJECT)))
                .andExpect(status().isBadRequest());

        // Spring Data treats instructor_pay and instructorPay as the same property; so does the guard.
        mockMvc.perform(get("/api/v1/classes?sort=instructor_pay").with(jwt(LEARNER_SUBJECT)))
                .andExpect(status().isBadRequest());

        // A platform admin may read the figure, but ordering by it is still not a supported query.
        mockMvc.perform(get("/api/v1/classes?sort=instructorPay,asc").with(jwt(ADMIN_SUBJECT)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("The public price is still sortable — the price is not what is confidential")
    void salePriceRemainsSortable() throws Exception {
        mockMvc.perform(get("/api/v1/classes?sort=salePrice,desc").with(jwt(LEARNER_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].class_definition.title").value(ORG_CLASS_TITLE));
    }

    // ===== TEST PLUMBING =====

    private String classesUrl() {
        return "/api/v1/classes/course/" + courseUuid;
    }

    private RequestPostProcessor jwt(String subject) {
        return org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                .jwt().jwt(builder -> builder.subject(subject).claim("sub", subject));
    }

    private void clean() {
        jdbc.execute("TRUNCATE class_definitions, courses, course_creators, instructors, "
                + "user_organisation_domain_mapping, user_domain_mapping, organisation, users "
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

    private UUID organisation(String name) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO organisation (uuid, name, location, country, lat, long, created_by) "
                + "VALUES (?, ?, 'Westlands', 'Kenya', -1.264400, 36.803400, 'test')", uuid, name);
        return uuid;
    }

    private UUID instructor(UUID userUuid) {
        UUID uuid = UUID.randomUUID();
        // full_name is derived from the user row by a database trigger, so it is not passed here.
        jdbc.update("INSERT INTO instructors (uuid, user_uuid, full_name, location_name, lat, long, "
                        + "admin_verified, created_by) "
                        + "VALUES (?, ?, 'ignored', 'Kisumu', -0.091700, 34.767900, true, 'test')",
                uuid, userUuid);
        return uuid;
    }

    private void classDefinition(String title, UUID organisationUuid, UUID instructorUuid,
                                 BigDecimal salePrice, BigDecimal instructorPay) {
        // The registration window is mandatory on the table; this listing never consults it, so it is
        // opened wide around today.
        jdbc.update("INSERT INTO class_definitions (uuid, title, default_instructor_uuid, organisation_uuid, "
                        + "course_uuid, sale_price, instructor_pay, rate_basis, default_start_time, "
                        + "default_end_time, class_visibility, session_format, is_active, "
                        + "registration_period_start_date, registration_period_end_date, created_by) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, 'PER_SESSION', '2026-04-01 09:00:00'::timestamp, "
                        + "'2026-04-01 10:30:00'::timestamp, 'PUBLIC', 'GROUP', true, "
                        + "(now() AT TIME ZONE 'UTC')::date - 30, (now() AT TIME ZONE 'UTC')::date + 365, 'test')",
                UUID.randomUUID(), title, instructorUuid, organisationUuid, courseUuid, salePrice, instructorPay);
    }
}
