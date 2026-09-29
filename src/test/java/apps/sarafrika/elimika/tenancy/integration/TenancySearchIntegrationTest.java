package apps.sarafrika.elimika.tenancy.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import apps.sarafrika.elimika.shared.search.SearchFilter;
import apps.sarafrika.elimika.shared.search.SearchGateway;
import apps.sarafrika.elimika.shared.search.SearchHit;
import apps.sarafrika.elimika.shared.search.SearchIndexRequests;
import apps.sarafrika.elimika.shared.search.SearchPage;
import apps.sarafrika.elimika.shared.search.SearchRequest;
import apps.sarafrika.elimika.shared.search.SearchScope;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.tenancy.dto.OrganisationDTO;
import apps.sarafrika.elimika.tenancy.dto.UserDTO;
import apps.sarafrika.elimika.tenancy.entity.Organisation;
import apps.sarafrika.elimika.tenancy.repository.OrganisationRepository;
import apps.sarafrika.elimika.tenancy.services.AdminService;
import apps.sarafrika.elimika.tenancy.services.OrganisationService;
import apps.sarafrika.elimika.tenancy.services.UserService;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * The {@code people} and {@code organisations} indexes against a real Meilisearch and PostgreSQL:
 * documents built by the tenancy sources, entity triggers, caller scopes, and the SQL re-check that
 * keeps a lagging index from widening what a caller sees. The callers' roles come from a mocked
 * {@link DomainSecurityService}; everything else is the production wiring.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest
@Testcontainers
@DisplayName("People and organisation search (end-to-end)")
class TenancySearchIntegrationTest {

    private static final String IMAGE = "getmeili/meilisearch:v1.54.1";
    private static final String MASTER_KEY = "integration-test-master-key-0123456789";
    private static final String PEOPLE = "people";
    private static final String ORGANISATIONS = "organisations";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @Container
    static GenericContainer<?> meilisearch = new GenericContainer<>(DockerImageName.parse(IMAGE))
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
        registry.add("search.read-enabled.people", () -> "true");
        registry.add("search.read-enabled.organisations", () -> "true");
        registry.add("search.meilisearch.host",
                () -> "http://" + meilisearch.getHost() + ":" + meilisearch.getMappedPort(7700));
        registry.add("search.meilisearch.api-key", () -> MASTER_KEY);
    }

    @MockBean private JwtDecoder jwtDecoder;
    @MockBean private DomainSecurityService domainSecurityService;

    @Autowired private UserService userService;
    @Autowired private AdminService adminService;
    @Autowired private OrganisationService organisationService;
    @Autowired private OrganisationRepository organisationRepository;
    @Autowired private SearchGateway gateway;
    @Autowired private SearchIndexRequests indexRequests;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactionManager;

    private final Random random = new Random();

    @BeforeEach
    void anonymousCaller() {
        reset(domainSecurityService);
        when(domainSecurityService.isPlatformAdmin()).thenReturn(false);
        when(domainSecurityService.managesOrganisation(any())).thenReturn(false);
    }

    // ===== people =====

    @Test
    @DisplayName("A platform admin finds a user by a misspelt name and by email")
    void adminFindsByTypoAndEmail() {
        String surname = word();
        UUID user = insertUser("Bartholomew", null, surname, surname + ".Person@Example.test");
        UUID other = insertUser("Bartholomew", null, word(), word() + "@example.test");
        index(PEOPLE, user, other);
        asPlatformAdmin();

        assertThat(uuids(userService.search(Map.of("q", typo(surname)), PageRequest.of(0, 20))))
                .containsExactly(user);
        // Stored lower-cased and matched without typo tolerance.
        assertThat(uuids(userService.search(Map.of("q", surname + ".person@example.test"), PageRequest.of(0, 20))))
                .containsExactly(user);
        // Other parameters filter on the index attributes.
        assertThat(uuids(userService.search(Map.of("q", typo(surname), "active", "false"), PageRequest.of(0, 20))))
                .isEmpty();

        SearchHit document = gateway.search(SearchRequest.of(PEOPLE, surname, SearchFilter.eq("uuid", user),
                SearchScope.unrestricted("test"), 0, 1)).hits().getFirst();
        assertThat(document.document())
                .containsEntry("full_name", "Bartholomew " + surname)
                .containsEntry("email", surname.toLowerCase() + ".person@example.test")
                .doesNotContainKeys("phone_number", "dob", "gender", "keycloak_id", "profile_image_url");
    }

    @Test
    @DisplayName("A non-admin cannot search people platform-wide")
    void nonAdminIsRefused() {
        assertThatThrownBy(() -> userService.search(Map.of("q", "anyone"), PageRequest.of(0, 20)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Admin-eligible search excludes users who hold an admin role")
    void adminEligibleExcludesAdmins() {
        String surname = word();
        UUID organisation = insertOrganisation("Eligibility " + word());
        UUID plain = insertUser("Plain", null, surname, word() + "@example.test");
        UUID orgAdmin = insertUser("Orgadmin", null, surname, word() + "@example.test");
        insertMembership(orgAdmin, organisation, "organisation_user");
        index(PEOPLE, plain, orgAdmin);
        asPlatformAdmin();

        assertThat(uuids(adminService.getAdminEligibleUsers(typo(surname), PageRequest.of(0, 20))))
                .containsExactly(plain);
    }

    @Test
    @DisplayName("An organisation manager finds only their own members, by name and never by email")
    void organisationManagerIsScopedToTheirMembers() {
        String surname = word();
        UUID ownOrganisation = insertOrganisation("Own " + word());
        UUID otherOrganisation = insertOrganisation("Other " + word());
        UUID member = insertUser("Cornelius", null, surname, surname + "member@example.test");
        UUID outsider = insertUser("Cornelius", null, surname, surname + "outsider@example.test");
        insertMembership(member, ownOrganisation, "student");
        insertMembership(outsider, otherOrganisation, "student");
        index(PEOPLE, member, outsider);
        managing(ownOrganisation);

        Page<UserDTO> byName = userService.getUsersByOrganisation(ownOrganisation, typo(surname), PageRequest.of(0, 20));
        assertThat(uuids(byName)).containsExactly(member);
        assertThat(byName.getTotalElements()).isEqualTo(1);

        assertThat(userService.getUsersByOrganisation(ownOrganisation, surname + "member@example.test",
                PageRequest.of(0, 20)).getContent()).isEmpty();

        assertThatThrownBy(() -> userService.getUsersByOrganisation(otherOrganisation, surname, PageRequest.of(0, 20)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Removing a membership removes the user from that organisation's search results")
    void removedMembershipDisappears() {
        String surname = word();
        UUID organisation = insertOrganisation("Roster " + word());
        UUID staying = insertUser("Desmond", null, surname, word() + "@example.test");
        UUID leaving = insertUser("Desmond", null, surname, word() + "@example.test");
        UUID revoked = insertUser("Desmond", null, surname, word() + "@example.test");
        insertMembership(staying, organisation, "student");
        insertMembership(leaving, organisation, "student");
        insertMembership(revoked, organisation, "student");
        index(PEOPLE, staying, leaving, revoked);
        managing(organisation);
        assertThat(uuids(userService.getUsersByOrganisation(organisation, surname, PageRequest.of(0, 20))))
                .containsExactlyInAnyOrder(staying, leaving, revoked);

        // Through the service: the mapping's entity trigger re-indexes the user.
        userService.removeUserFromOrganisation(leaving, organisation);
        awaitTrue(() -> !organisationMembersInIndex(organisation, surname).contains(leaving));

        // Behind the index's back: the SQL re-check drops the row and restates the total at once.
        jdbc.update("UPDATE user_organisation_domain_mapping SET active = false, deleted = true WHERE user_uuid = ?",
                revoked);
        assertThat(organisationMembersInIndex(organisation, surname)).contains(revoked);

        Page<UserDTO> page = userService.getUsersByOrganisation(organisation, surname, PageRequest.of(0, 20));
        assertThat(uuids(page)).containsExactly(staying);
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    // ===== organisations =====

    @Test
    @DisplayName("A deleted organisation is never indexed, and deleting one removes it")
    void deletedOrganisationIsNotIndexed() {
        String tag = word();
        Organisation deleted = organisation("Deleted " + tag + " Academy", true, true);
        deleted.setDeleted(true);
        Organisation kept = organisation("Kept " + tag + " Academy", true, true);
        Organisation later = organisation("Later " + tag + " Academy", true, true);
        save(deleted, kept, later);

        // All three were saved in one transaction, so one indexing request carried them.
        awaitTrue(() -> organisationsInIndex(tag).contains(kept.getUuid()));
        assertThat(organisationsInIndex(tag)).containsExactlyInAnyOrder(kept.getUuid(), later.getUuid());

        organisationService.deleteOrganisation(later.getUuid());
        awaitTrue(() -> !organisationsInIndex(tag).contains(later.getUuid()));
        assertThat(organisationsInIndex(tag)).containsExactly(kept.getUuid());
    }

    @Test
    @DisplayName("A non-admin sees only active, verified organisations; an admin sees all, pending included")
    void organisationVisibility() {
        String tag = word();
        Organisation verified = organisation("Verified " + tag + " College", true, true);
        Organisation pending = organisation("Pending " + tag + " College", true, false);
        Organisation inactive = organisation("Inactive " + tag + " College", false, true);
        save(verified, pending, inactive);
        awaitTrue(() -> organisationsInIndex(tag).size() == 3);

        assertThat(organisationUuids(organisationService.search(Map.of("q", typo(tag)), PageRequest.of(0, 20))))
                .containsExactly(verified.getUuid());

        asPlatformAdmin();
        assertThat(organisationUuids(organisationService.search(Map.of("q", typo(tag)), PageRequest.of(0, 20))))
                .containsExactlyInAnyOrder(verified.getUuid(), pending.getUuid(), inactive.getUuid());
        assertThat(organisationUuids(organisationService.getUnverifiedOrganisations(tag, PageRequest.of(0, 20))))
                .containsExactly(pending.getUuid());
    }

    // ===== Helpers =====

    private void asPlatformAdmin() {
        when(domainSecurityService.isPlatformAdmin()).thenReturn(true);
    }

    private void managing(UUID organisationUuid) {
        when(domainSecurityService.managesOrganisation(organisationUuid)).thenReturn(true);
    }

    /** Pushes rows written with plain SQL through the indexing listener and waits until they land. */
    private void index(String index, UUID... uuids) {
        indexRequests.enqueue(index, List.of(uuids));
        awaitTrue(() -> gateway.search(SearchRequest.of(index, null, SearchFilter.in("uuid", List.of((Object[]) uuids)),
                SearchScope.unrestricted("test"), 0, 100)).totalHits() == uuids.length);
    }

    private List<UUID> organisationMembersInIndex(UUID organisationUuid, String text) {
        return hitUuids(gateway.search(SearchRequest.of(PEOPLE, text, null,
                SearchScope.of(SearchFilter.eq("organisation_uuids", organisationUuid), "test"), 0, 100)));
    }

    private List<UUID> organisationsInIndex(String text) {
        return hitUuids(gateway.search(SearchRequest.of(ORGANISATIONS, text, null,
                SearchScope.unrestricted("test"), 0, 100)));
    }

    private static List<UUID> hitUuids(SearchPage page) {
        return page.hits().stream().map(SearchHit::uuid).toList();
    }

    private static List<UUID> uuids(Page<UserDTO> page) {
        return page.getContent().stream().map(UserDTO::uuid).toList();
    }

    private static List<UUID> organisationUuids(Page<OrganisationDTO> page) {
        return page.getContent().stream().map(OrganisationDTO::uuid).toList();
    }

    private Organisation organisation(String name, boolean active, boolean verified) {
        Organisation organisation = new Organisation();
        organisation.setName(name);
        organisation.setSlug(name.toLowerCase().replace(' ', '-'));
        organisation.setDescription("About " + name);
        organisation.setLocation("Nairobi");
        organisation.setCountry("Kenya");
        organisation.setActive(active);
        organisation.setAdminVerified(verified);
        return organisation;
    }

    private void save(Organisation... organisations) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                organisationRepository.saveAll(List.of(organisations)));
    }

    private UUID insertOrganisation(String name) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO organisation (uuid, name, created_by) VALUES (?, ?, 'test')", uuid, name);
        return uuid;
    }

    private UUID insertUser(String firstName, String middleName, String lastName, String email) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO users (uuid, first_name, middle_name, last_name, email, user_no, created_by) "
                        + "VALUES (?, ?, ?, ?, ?, lpad(nextval('user_no_seq')::text, 9, '0'), 'test')",
                uuid, firstName, middleName, lastName, email);
        return uuid;
    }

    private void insertMembership(UUID userUuid, UUID organisationUuid, String domainName) {
        jdbc.update("INSERT INTO user_organisation_domain_mapping "
                        + "(uuid, user_uuid, organisation_uuid, domain_uuid, active, created_by) "
                        + "VALUES (?, ?, ?, (SELECT uuid FROM user_domain WHERE domain_name = ?), true, 'test')",
                UUID.randomUUID(), userUuid, organisationUuid, domainName);
    }

    /** A random eight-letter word, so tests never match each other's rows. */
    private String word() {
        StringBuilder word = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            word.append((char) ('a' + random.nextInt(26)));
        }
        return word.toString();
    }

    /** The word with its fourth letter replaced - one typo, which an eight-letter word tolerates. */
    private static String typo(String word) {
        char replaced = word.charAt(3) == 'z' ? 'y' : (char) (word.charAt(3) + 1);
        return word.substring(0, 3) + replaced + word.substring(4);
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
