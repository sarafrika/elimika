package apps.sarafrika.elimika.tenancy.integration;

import apps.sarafrika.elimika.shared.event.notification.NotificationRequestedEvent;
import apps.sarafrika.elimika.testsupport.SharedPostgres;
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
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * An organisation invites a parent for a child under 18 with no email:
 * {@code POST /organisations/{uuid}/invitations/parents}, then the parent approves on
 * {@code /guardian-invitations/token/{token}/accept}. Real HTTP and the Flyway schema; the student module's
 * listeners run after commit, so their effects are polled for.
 */
// Each class gets its own database; a cached context must not outlive it.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest
@AutoConfigureMockMvc
@RecordApplicationEvents
@DisplayName("Organisation invites a parent for a child (end-to-end)")
class ParentInvitationIntegrationTest {

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

    private static final String ADMIN_SUBJECT = "keycloak-org-admin";
    private static final String PARENT_SUBJECT = "keycloak-parent";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ApplicationEvents events;

    /** Never invoked: the jwt() post-processor sets the SecurityContext directly. */
    @MockitoBean private JwtDecoder jwtDecoder;

    private UUID organisationUuid;
    private UUID parentUuid;

    @BeforeEach
    void seed() {
        jdbc.execute("TRUNCATE organisation_invitations, student_guardian_links, students, "
                + "user_organisation_domain_mapping, user_domain_mapping, organisation, users RESTART IDENTITY CASCADE");
        organisationUuid = UUID.randomUUID();
        jdbc.update("INSERT INTO organisation (uuid, name, created_by) VALUES (?, 'Sunrise School', 'test')", organisationUuid);

        UUID adminUuid = user("100000001", "Olive", "Admin", "admin@sunrise.test", ADMIN_SUBJECT, "1980-02-01");
        jdbc.update("INSERT INTO user_organisation_domain_mapping (user_uuid, organisation_uuid, domain_uuid, created_by) "
                + "VALUES (?, ?, (SELECT uuid FROM user_domain WHERE domain_name = 'admin'), 'test')", adminUuid, organisationUuid);
        jdbc.update("INSERT INTO user_domain_mapping (user_uuid, domain_uuid, status) "
                + "VALUES (?, (SELECT uuid FROM user_domain WHERE domain_name = 'organisation_user'), 'APPROVED')", adminUuid);

        parentUuid = user("100000002", "Mary", "Kamau", "mary@parents.test", PARENT_SUBJECT, "1985-04-12");
    }

    @Test
    @DisplayName("the parent approves: the child is created with no login, joins as a student and is linked")
    void parentApprovesAndTheChildJoins() throws Exception {
        String token = inviteParent("Wanjiru", "Kamau", yearsAgo(9));

        mockMvc.perform(get("/api/v1/guardian-invitations/token/" + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.student_name").value("Wanjiru Kamau"))
                .andExpect(jsonPath("$.data.status").value("AWAITING_GUARDIAN_CONSENT"));

        mockMvc.perform(post("/api/v1/guardian-invitations/token/" + token + "/accept").with(as(PARENT_SUBJECT))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"scope_acknowledged\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.affiliated").value(true));

        Map<String, Object> child = jdbc.queryForMap("SELECT uuid, email, keycloak_id, dob FROM users "
                + "WHERE first_name = 'Wanjiru' AND last_name = 'Kamau'");
        assertThat(child.get("email")).isNull();
        assertThat(child.get("keycloak_id")).isNull();
        UUID childUuid = (UUID) child.get("uuid");
        assertThat(jdbc.queryForObject("SELECT d.domain_name FROM user_organisation_domain_mapping m "
                        + "JOIN user_domain d ON d.uuid = m.domain_uuid WHERE m.user_uuid = ? AND m.organisation_uuid = ?",
                String.class, childUuid, organisationUuid)).isEqualTo("student");

        eventually(() -> count("SELECT COUNT(*) FROM student_guardian_links l JOIN students s ON s.uuid = l.student_uuid "
                + "WHERE s.user_uuid = '" + childUuid + "' AND l.guardian_user_uuid = '" + parentUuid + "' AND l.link_status = 'ACTIVE'") == 1);
        eventually(() -> domainCount(parentUuid, "parent") == 1 && domainCount(childUuid, "student") == 1);

        mockMvc.perform(get("/api/v1/guardians/me/students").with(as(PARENT_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].student_name").value("Wanjiru Kamau"));
    }

    @Test
    @DisplayName("a child the parent already added is reused rather than duplicated")
    void anExistingChildIsReused() throws Exception {
        mockMvc.perform(post("/api/v1/guardians/me/students").with(as(PARENT_SUBJECT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"first_name\":\"Wanjiru\",\"last_name\":\"Kamau\",\"dob\":\"" + yearsAgo(9)
                                + "\",\"relationship_type\":\"PARENT\"}"))
                .andExpect(status().isCreated());
        String token = inviteParent("wanjiru", " Kamau ", yearsAgo(9));

        mockMvc.perform(post("/api/v1/guardian-invitations/token/" + token + "/accept").with(as(PARENT_SUBJECT))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"scope_acknowledged\":true}"))
                .andExpect(status().isOk());

        assertThat(count("SELECT COUNT(*) FROM users WHERE lower(first_name) = 'wanjiru'")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM user_organisation_domain_mapping WHERE organisation_uuid = '"
                + organisationUuid + "'")).isEqualTo(2);
    }

    @Test
    @DisplayName("only the invited parent's account may approve, and declining leaves nothing behind")
    void someoneElseCannotApproveAndDeclineLeavesNothing() throws Exception {
        String token = inviteParent("Wanjiru", "Kamau", yearsAgo(9));

        mockMvc.perform(post("/api/v1/guardian-invitations/token/" + token + "/accept").with(as(ADMIN_SUBJECT))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"scope_acknowledged\":true}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/guardian-invitations/token/" + token + "/decline").with(as(PARENT_SUBJECT)))
                .andExpect(status().isOk());

        assertThat(count("SELECT COUNT(*) FROM users WHERE first_name = 'Wanjiru'")).isZero();
        assertThat(jdbc.queryForObject("SELECT status FROM organisation_invitations", String.class)).isEqualTo("DECLINED");
    }

    @Test
    @DisplayName("each sibling gets their own invitation, and a child already invited or aged 18 is reported")
    void siblingsDuplicatesAndAdults() throws Exception {
        mockMvc.perform(post("/api/v1/organisations/" + organisationUuid + "/invitations/parents").with(as(ADMIN_SUBJECT))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"children":[
                                  {"first_name":"Wanjiru","last_name":"Kamau","dob":"%s","guardian_email":"mary@parents.test"},
                                  {"first_name":"Baraka","last_name":"Kamau","dob":"%s","guardian_email":"mary@parents.test"},
                                  {"first_name":"Wanjiru","last_name":"Kamau","dob":"%s","guardian_email":"MARY@parents.test"},
                                  {"first_name":"Grown","last_name":"Kamau","dob":"%s","guardian_email":"mary@parents.test"}
                                ]}""".formatted(yearsAgo(9), yearsAgo(12), yearsAgo(9), yearsAgo(18))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.sent.length()").value(2))
                .andExpect(jsonPath("$.data.sent[0].parent_invitation").value(true))
                .andExpect(jsonPath("$.data.sent[0].guardian_email").value("mary@parents.test"))
                .andExpect(jsonPath("$.data.failed.length()").value(2));
    }

    @Test
    @DisplayName("only a manager of the organisation may invite parents")
    void onlyManagersMayInvite() throws Exception {
        mockMvc.perform(post("/api/v1/organisations/" + organisationUuid + "/invitations/parents").with(as(PARENT_SUBJECT))
                        .contentType(MediaType.APPLICATION_JSON).content(children("Wanjiru", "Kamau", yearsAgo(9))))
                .andExpect(status().isForbidden());
    }

    /** Sends one parent invitation and returns the raw token from the consent link in the email. */
    private String inviteParent(String firstName, String lastName, LocalDate dob) throws Exception {
        events.clear();
        mockMvc.perform(post("/api/v1/organisations/" + organisationUuid + "/invitations/parents").with(as(ADMIN_SUBJECT))
                        .contentType(MediaType.APPLICATION_JSON).content(children(firstName, lastName, dob)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.sent.length()").value(1))
                .andExpect(jsonPath("$.data.sent[0].status").value("AWAITING_GUARDIAN_CONSENT"));
        NotificationRequestedEvent email = events.stream(NotificationRequestedEvent.class)
                .filter(event -> "GUARDIAN_CONSENT_REQUEST".equals(event.notificationType()))
                .findFirst().orElseThrow();
        assertThat(email.recipientEmail()).isEqualTo("mary@parents.test");
        String link = (String) email.templateVariables().get("consentLink");
        return link.substring(link.lastIndexOf('/') + 1);
    }

    private static String children(String firstName, String lastName, LocalDate dob) {
        return """
                {"children":[{"first_name":"%s","last_name":"%s","dob":"%s","guardian_email":"mary@parents.test",
                "guardian_name":"Mary Kamau"}]}""".formatted(firstName, lastName, dob);
    }

    private UUID user(String userNo, String first, String last, String email, String subject, String dob) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO users (uuid, user_no, first_name, last_name, email, keycloak_id, dob, created_by) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?::date, 'test')", uuid, userNo, first, last, email, subject, dob);
        return uuid;
    }

    private int domainCount(UUID userUuid, String domain) {
        return count("SELECT COUNT(*) FROM user_domain_mapping m JOIN user_domain d ON d.uuid = m.domain_uuid "
                + "WHERE m.user_uuid = '" + userUuid + "' AND d.domain_name = '" + domain + "'");
    }

    private int count(String sql) {
        return jdbc.queryForObject(sql, Integer.class);
    }

    /** The student module reacts after commit on its own thread. */
    private static void eventually(Supplier<Boolean> condition) throws InterruptedException {
        for (int attempt = 0; attempt < 50 && !condition.get(); attempt++) {
            Thread.sleep(200);
        }
        assertThat(condition.get()).isTrue();
    }

    private static RequestPostProcessor as(String subject) {
        return jwt().jwt(builder -> builder.subject(subject).claim("sub", subject));
    }

    private static LocalDate yearsAgo(int years) {
        return LocalDate.now(ZoneOffset.UTC).minusYears(years).minusDays(30);
    }
}
