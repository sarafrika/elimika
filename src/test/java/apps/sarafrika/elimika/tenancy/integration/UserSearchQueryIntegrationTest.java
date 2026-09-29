package apps.sarafrika.elimika.tenancy.integration;

import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import apps.sarafrika.elimika.shared.utils.LikePatterns;
import apps.sarafrika.elimika.tenancy.entity.User;
import apps.sarafrika.elimika.tenancy.repository.UserRepository;
import apps.sarafrika.elimika.tenancy.util.UserSpecificationBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the user search predicates against a real PostgreSQL instance, where the generated SQL
 * (CASE/CONCAT/COALESCE expressions, LIKE escaping) actually executes.
 */
// The container dies with this class; a cached context must not outlive it.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import({UserSpecificationBuilder.class, GenericSpecificationBuilder.class})
@DisplayName("User search queries")
class UserSearchQueryIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserSpecificationBuilder userSpecificationBuilder;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    @DisplayName("full_name matches users without a middle name and ignores a middle name when omitted")
    void fullNameMatchesWithAndWithoutMiddleName() {
        UUID noMiddle = insertUser("John", null, "Doe", uniqueEmail());
        UUID blankMiddle = insertUser("John", "  ", "Doe", uniqueEmail());
        UUID withMiddle = insertUser("John", "Kamau", "Doe", uniqueEmail());
        UUID other = insertUser("Jane", null, "Doe", uniqueEmail());

        assertThat(search(Map.of("full_name", "John Doe")))
                .containsExactlyInAnyOrder(noMiddle, blankMiddle, withMiddle)
                .doesNotContain(other);
        assertThat(search(Map.of("full_name", "john  kamau doe"))).containsExactly(withMiddle);
        assertThat(search(Map.of("full_name_like", "john doe")))
                .containsExactlyInAnyOrder(noMiddle, blankMiddle, withMiddle);
        assertThat(search(Map.of("full_name_like", "hn kamau d"))).containsExactly(withMiddle);
    }

    @Test
    @DisplayName("full_name_like treats LIKE wildcards in the input literally")
    void fullNameLikeEscapesWildcards() {
        UUID percent = insertUser("Fifty%", null, "Off", uniqueEmail());
        insertUser("Fiftyx", null, "Off", uniqueEmail());

        assertThat(search(Map.of("full_name_like", "fifty%"))).containsExactly(percent);
        assertThat(search(Map.of("full_name_like", "f_fty"))).isEmpty();
    }

    @Test
    @DisplayName("email lookup ignores case and prefers an exact match")
    void emailLookupIgnoresCase() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        UUID mixed = insertUser("Mixed", null, "Case", "Mixed.Case" + suffix + "@Example.test");

        assertThat(userRepository.findByEmailIgnoreCase("mixed.case" + suffix + "@example.test"))
                .map(User::getUuid).contains(mixed);
        assertThat(userRepository.findByEmailIgnoreCase("  MIXED.CASE" + suffix + "@EXAMPLE.TEST "))
                .map(User::getUuid).contains(mixed);

        UUID lower = insertUser("Lower", null, "Case", "mixed.case" + suffix + "@example.test");
        assertThat(userRepository.findByEmailIgnoreCase("mixed.case" + suffix + "@example.test"))
                .map(User::getUuid).contains(lower);
        assertThat(userRepository.findByEmailIgnoreCase(null)).isEmpty();
    }

    @Test
    @DisplayName("admin-eligible users exclude global and organisation admins, search and page in the database")
    void adminEligibleUsersExcludeAdmins() {
        String tag = "elig" + UUID.randomUUID().toString().substring(0, 6);
        UUID organisationUuid = UUID.randomUUID();
        jdbc.update("INSERT INTO organisation (uuid, name, created_by) VALUES (?, ?, 'test')",
                organisationUuid, "Org " + tag);

        UUID eligible = insertUser("Plain", null, tag, uniqueEmail());
        UUID emailOnly = insertUser("", null, "", tag + "@example.test");
        UUID globalAdmin = insertUser("Global", null, tag, uniqueEmail());
        jdbc.update("INSERT INTO user_domain_mapping (user_uuid, domain_uuid) "
                + "VALUES (?, (SELECT uuid FROM user_domain WHERE domain_name = 'admin'))", globalAdmin);
        UUID orgAdmin = insertUser("Org", null, tag, uniqueEmail());
        insertOrganisationMapping(orgAdmin, organisationUuid, true);
        UUID formerOrgAdmin = insertUser("Former", null, tag, uniqueEmail());
        insertOrganisationMapping(formerOrgAdmin, organisationUuid, false);

        String pattern = LikePatterns.containsLower(tag.toUpperCase());
        assertThat(userRepository.findAdminEligibleUsers(pattern, PageRequest.of(0, 20, Sort.by("id")))
                .map(User::getUuid).getContent())
                .containsExactly(eligible, emailOnly, formerOrgAdmin);

        var secondPage = userRepository.findAdminEligibleUsers(pattern, PageRequest.of(1, 2, Sort.by("id")));
        assertThat(secondPage.getTotalElements()).isEqualTo(3);
        assertThat(secondPage.map(User::getUuid).getContent()).containsExactly(formerOrgAdmin);
        assertThat(userRepository.findAdminEligibleUsers(pattern, PageRequest.of(5, 2)).getContent()).isEmpty();

        assertThat(userRepository.findAdminEligibleUsers(null, PageRequest.of(0, 1000)).map(User::getUuid).getContent())
                .contains(eligible, emailOnly, formerOrgAdmin)
                .doesNotContain(globalAdmin, orgAdmin);
        assertThat(userRepository.findAdminEligibleUsers(LikePatterns.containsLower("%"), PageRequest.of(0, 20))
                .getContent()).isEmpty();
    }

    private void insertOrganisationMapping(UUID userUuid, UUID organisationUuid, boolean active) {
        jdbc.update("INSERT INTO user_organisation_domain_mapping "
                        + "(uuid, user_uuid, organisation_uuid, domain_uuid, active, created_by) "
                        + "VALUES (?, ?, ?, (SELECT uuid FROM user_domain WHERE domain_name = 'organisation_user'), ?, 'test')",
                UUID.randomUUID(), userUuid, organisationUuid, active);
    }

    private List<UUID> search(Map<String, String> params) {
        return userRepository.findAll(userSpecificationBuilder.buildUserSpecification(params)).stream()
                .map(User::getUuid)
                .toList();
    }

    private UUID insertUser(String firstName, String middleName, String lastName, String email) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO users (uuid, first_name, middle_name, last_name, email, user_no, created_by) "
                        + "VALUES (?, ?, ?, ?, ?, lpad(nextval('user_no_seq')::text, 9, '0'), 'test')",
                uuid, firstName, middleName, lastName, email);
        return uuid;
    }

    private static String uniqueEmail() {
        return "u" + UUID.randomUUID().toString().substring(0, 12) + "@example.test";
    }
}
