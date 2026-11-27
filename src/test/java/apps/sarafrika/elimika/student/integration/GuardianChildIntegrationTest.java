package apps.sarafrika.elimika.student.integration;

import apps.sarafrika.elimika.testsupport.SharedPostgres;
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

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A signed-in guardian adds a child with no login: {@code POST /api/v1/guardians/me/students} over real HTTP,
 * through the security chain, against the Flyway schema (nullable email, the login CHECK, the full_name trigger,
 * the age gate rule and the relationship CHECK).
 */
// Each class gets its own database; a cached context must not outlive it.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Guardian adds a child as a managed learner (end-to-end)")
class GuardianChildIntegrationTest {

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

    private static final String PARENT_SUBJECT = "keycloak-parent";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    /** Never invoked: the jwt() post-processor sets the SecurityContext directly. */
    @MockitoBean private JwtDecoder jwtDecoder;

    private UUID parentUserUuid;

    @BeforeEach
    void seed() {
        jdbc.execute("TRUNCATE student_guardian_links, students, user_domain_mapping, users RESTART IDENTITY CASCADE");
        parentUserUuid = UUID.randomUUID();
        jdbc.update("INSERT INTO users (uuid, user_no, first_name, last_name, email, keycloak_id, dob, created_by) "
                + "VALUES (?, '100000001', 'Mary', 'Kamau', 'mary@test.local', ?, '1985-04-12', 'test')",
                parentUserUuid, PARENT_SUBJECT);
    }

    @Test
    @DisplayName("creates a login-less learner and links the parent with full access")
    void parentAddsAChild() throws Exception {
        mockMvc.perform(post("/api/v1/guardians/me/students").with(parent())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(child("Wanjiru", "Kamau", yearsAgo(9), "GUARDIAN")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.student_name").value("Wanjiru Kamau"))
                .andExpect(jsonPath("$.data.guardian_user_uuid").value(parentUserUuid.toString()))
                .andExpect(jsonPath("$.data.relationship_type").value("GUARDIAN"))
                .andExpect(jsonPath("$.data.share_scope").value("FULL"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.primary_guardian").value(true));

        Map<String, Object> childUser = jdbc.queryForMap("SELECT u.email, u.keycloak_id, u.dob, u.user_no, s.full_name "
                + "FROM students s JOIN users u ON u.uuid = s.user_uuid WHERE u.uuid <> ?", parentUserUuid);
        assertThat(childUser.get("email")).isNull();
        assertThat(childUser.get("keycloak_id")).isNull();
        assertThat(childUser.get("user_no")).asString().matches("[0-9]{9}");
        assertThat(childUser.get("full_name")).isEqualTo("Wanjiru Kamau");

        mockMvc.perform(get("/api/v1/guardians/me/students").with(parent()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].student_name").value("Wanjiru Kamau"));
    }

    @Test
    @DisplayName("a second submit of the same child is a 409, not a second learner")
    void addingTheSameChildTwiceIsRefused() throws Exception {
        String body = child("Wanjiru", "Kamau", yearsAgo(9), "PARENT");
        mockMvc.perform(post("/api/v1/guardians/me/students").with(parent())
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/guardians/me/students").with(parent())
                        .contentType(MediaType.APPLICATION_JSON).content(body.replace("Wanjiru", " wanjiru ")))
                .andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM students", Integer.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("refuses adults, sponsors and students below the age gate, leaving no account behind")
    void ineligibleChildrenAreRefused() throws Exception {
        mockMvc.perform(post("/api/v1/guardians/me/students").with(parent())
                        .contentType(MediaType.APPLICATION_JSON).content(child("Adult", "Kamau", yearsAgo(19), "PARENT")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/guardians/me/students").with(parent())
                        .contentType(MediaType.APPLICATION_JSON).content(child("Sponsored", "Kamau", yearsAgo(9), "SPONSOR")))
                .andExpect(status().isBadRequest());
        // The seeded learner age gate starts at 5; the child's account rolls back with the profile.
        mockMvc.perform(post("/api/v1/guardians/me/students").with(parent())
                        .contentType(MediaType.APPLICATION_JSON).content(child("Toddler", "Kamau", yearsAgo(3), "PARENT")))
                .andExpect(status().isBadRequest());

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("requires a signed-in account")
    void anonymousCallersAreRejected() throws Exception {
        mockMvc.perform(post("/api/v1/guardians/me/students")
                        .contentType(MediaType.APPLICATION_JSON).content(child("Wanjiru", "Kamau", yearsAgo(9), "PARENT")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("a signed-in account with no parent domain may add a child, and gains the parent domain")
    void anyAccountMayAddAChildAndBecomesAParent() throws Exception {
        assertThat(parentDomainCount()).isZero();
        mockMvc.perform(get("/api/v1/guardians/me/students").with(parent())).andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/guardians/me/students").with(parent())
                        .contentType(MediaType.APPLICATION_JSON).content(child("Wanjiru", "Kamau", yearsAgo(9), "PARENT")))
                .andExpect(status().isCreated());

        assertThat(parentDomainCount()).isEqualTo(1);
        mockMvc.perform(get("/api/v1/guardians/me/students").with(parent())).andExpect(status().isOk());
    }

    private Integer parentDomainCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM user_domain_mapping m JOIN user_domain d ON d.uuid = m.domain_uuid "
                + "WHERE m.user_uuid = ? AND d.domain_name = 'parent'", Integer.class, parentUserUuid);
    }

    @Test
    @DisplayName("an account that can sign in must still have an email")
    void loginAccountsKeepTheirEmail() {
        assertThatThrownBy(() -> jdbc.update("INSERT INTO users (uuid, user_no, first_name, last_name, keycloak_id, created_by) "
                + "VALUES (?, '100000002', 'No', 'Email', 'keycloak-no-email', 'test')", UUID.randomUUID()))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("chk_users_email_required_for_login");
    }

    private RequestPostProcessor parent() {
        return jwt().jwt(builder -> builder.subject(PARENT_SUBJECT).claim("sub", PARENT_SUBJECT));
    }

    private static String child(String firstName, String lastName, LocalDate dob, String relationship) {
        return """
                {"first_name":"%s","last_name":"%s","dob":"%s","relationship_type":"%s"}
                """.formatted(firstName, lastName, dob, relationship);
    }

    private static LocalDate yearsAgo(int years) {
        return LocalDate.now(ZoneOffset.UTC).minusYears(years).minusDays(30);
    }
}
