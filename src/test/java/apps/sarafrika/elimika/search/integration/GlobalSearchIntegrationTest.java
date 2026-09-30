package apps.sarafrika.elimika.search.integration;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import apps.sarafrika.elimika.classes.search.ClassSearchDocument;
import apps.sarafrika.elimika.course.internal.search.CourseSearchDocument;
import apps.sarafrika.elimika.course.internal.search.ProgramSearchDocument;
import apps.sarafrika.elimika.instructor.search.InstructorSearchDocument;
import apps.sarafrika.elimika.shared.search.GlobalSearchProvider;
import apps.sarafrika.elimika.shared.search.SearchGateway;
import apps.sarafrika.elimika.shared.search.SearchIndexAdmin;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.tenancy.search.OrganisationSearchDocument;
import apps.sarafrika.elimika.tenancy.search.PeopleSearchDocument;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
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
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * {@code GET /api/v1/search} and {@code /api/v1/search/{type}} against a real Meilisearch, through the
 * security filter chain as an anonymous request (the routes are permitAll).
 * <p>
 * Documents are written straight into the indexes: global search answers from the stored documents
 * without a database round trip, so what matters here is that each provider's scope admits exactly
 * what it should. The caller is simulated by mocking {@link DomainSecurityService}, which every
 * provider reads the caller from; its defaults (no user, no roles) are an anonymous visitor.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("Global search (end-to-end)")
class GlobalSearchIntegrationTest {

    private static final String MASTER_KEY = "integration-test-master-key-0123456789";
    private static final String WORD = "zephyrine";
    private static final String ALL_TYPES =
            "courses,programs,classes,marketplace_jobs,instructors,organisations,people,rubrics";

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
        registry.add("search.meilisearch.host",
                () -> "http://" + meilisearch.getHost() + ":" + meilisearch.getMappedPort(7700));
        registry.add("search.meilisearch.api-key", () -> MASTER_KEY);
        for (String index : ALL_TYPES.split(",")) {
            registry.add("search.read-enabled." + index, () -> "true");
        }
    }

    @MockBean private JwtDecoder jwtDecoder;
    @MockBean private DomainSecurityService domainSecurityService;

    @Autowired private MockMvc mockMvc;
    @Autowired private SearchGateway gateway;
    @Autowired private SearchIndexAdmin indexAdmin;
    @Autowired private List<GlobalSearchProvider> providers;
    @Autowired private JdbcTemplate jdbc;

    private static final UUID PUBLIC_COURSE = UUID.randomUUID();
    private static final UUID DRAFT_COURSE = UUID.randomUUID();
    private static final UUID LIVE_PROGRAM = UUID.randomUUID();
    private static final UUID VERIFIED_ORGANISATION = UUID.randomUUID();
    private static final UUID PENDING_ORGANISATION = UUID.randomUUID();
    private static final UUID PUBLIC_CLASS = UUID.randomUUID();
    private static final UUID PRIVATE_CLASS = UUID.randomUUID();
    private static final UUID PERSON = UUID.randomUUID();
    private static final UUID VERIFIED_INSTRUCTOR = UUID.randomUUID();

    private static boolean indexed;

    @BeforeEach
    void setUp() {
        reset(domainSecurityService);
        if (!indexed) {
            indexDocuments();
            indexed = true;
        }
    }

    private void indexDocuments() {
        Map<String, GlobalSearchProvider> byType = providers.stream()
                .collect(Collectors.toMap(GlobalSearchProvider::type, Function.identity()));
        byType.values().forEach(provider -> indexAdmin.ensureIndex(provider.index(), provider.definition()));
        long now = Instant.now().getEpochSecond();

        gateway.upsert("courses", List.of(
                course(PUBLIC_COURSE, "Zephyrine Astronomy", "published", true),
                course(DRAFT_COURSE, "Zephyrine Draft Notes", "draft", false)));
        gateway.upsert("programs", List.of(new ProgramSearchDocument(LIVE_PROGRAM, "Zephyrine Programme", "Stars",
                null, null, UUID.randomUUID(), "Ada Creator", List.of("Zephyrine Astronomy"), "published", true, true,
                true, true, true, BigDecimal.ZERO, now)));
        gateway.upsert("organisations", List.of(
                new OrganisationSearchDocument(VERIFIED_ORGANISATION, "Zephyrine Academy", "zephyrine-academy",
                        "An academy", "Nairobi", "KE", true, true, now),
                new OrganisationSearchDocument(PENDING_ORGANISATION, "Zephyrine Pending Institute", "zephyrine-pending",
                        "Awaiting review", "Mombasa", "KE", true, false, now)));
        gateway.upsert("classes", List.of(
                classDocument(PUBLIC_CLASS, "Zephyrine Evening Class", "PUBLIC"),
                classDocument(PRIVATE_CLASS, "Zephyrine Private Cohort", "PRIVATE")));
        gateway.upsert("people", List.of(new PeopleSearchDocument(PERSON, "Zephyrine", null, "Otieno",
                "Zephyrine Otieno", "zephyrine@example.test", "zotieno", "U-1", List.of("student"),
                List.of(VERIFIED_ORGANISATION), List.of(), true, false, false, now)));
        gateway.upsert("instructors", List.of(new InstructorSearchDocument(VERIFIED_INSTRUCTOR, "Zephyrine Kamau",
                "Astronomy tutor", "Teaches stars", "Nairobi", List.of("astronomy"), List.of("EXPERT"), List.of(),
                List.of(), true, true, null, 0, now)));
    }

    @Test
    @DisplayName("An anonymous caller finds public courses, programs, organisations and classes, and no people")
    void anonymousSeesPublicTypesOnly() throws Exception {
        mockMvc.perform(get("/api/v1/search").param("q", WORD).param("types", ALL_TYPES).param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hits[*].uuid", containsInAnyOrder(PUBLIC_COURSE.toString(),
                        LIVE_PROGRAM.toString(), VERIFIED_ORGANISATION.toString(), PUBLIC_CLASS.toString())))
                .andExpect(jsonPath("$.data.hits[*].type", not(hasItem("people"))))
                .andExpect(jsonPath("$.data.totals.courses").value(1))
                .andExpect(jsonPath("$.data.totals.people").doesNotExist())
                .andExpect(jsonPath("$.data.totals.instructors").doesNotExist())
                .andExpect(jsonPath("$.data.totals.marketplace_jobs").doesNotExist())
                .andExpect(jsonPath("$.data.totals.rubrics").doesNotExist())
                // Grouped by type in the order requested.
                .andExpect(jsonPath("$.data.hits[0].type").value("courses"))
                .andExpect(jsonPath("$.data.hits[0].title").value("Zephyrine Astronomy"))
                .andExpect(jsonPath("$.data.hits[0].subtitle").value("Ada Creator"))
                .andExpect(jsonPath("$.data.hits[1].type").value("programs"))
                .andExpect(jsonPath("$.data.hits[2].type").value("classes"))
                .andExpect(jsonPath("$.data.hits[3].type").value("organisations"));

        mockMvc.perform(get("/api/v1/search/people").param("q", WORD))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("A signed-in student sees verified instructors too, but still no people")
    void studentSeesNoPeople() throws Exception {
        when(domainSecurityService.getCurrentUserUuid()).thenReturn(user());
        when(domainSecurityService.getCurrentStudentUuid()).thenReturn(UUID.randomUUID());
        when(domainSecurityService.isStudent()).thenReturn(true);

        mockMvc.perform(get("/api/v1/search").param("q", WORD).param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hits[*].uuid", containsInAnyOrder(PUBLIC_COURSE.toString(),
                        LIVE_PROGRAM.toString(), VERIFIED_ORGANISATION.toString(), PUBLIC_CLASS.toString(),
                        VERIFIED_INSTRUCTOR.toString())))
                .andExpect(jsonPath("$.data.totals.people").doesNotExist());
    }

    @Test
    @DisplayName("A platform admin finds people, and everything the public boundary hides")
    void platformAdminSeesPeople() throws Exception {
        when(domainSecurityService.getCurrentUserUuid()).thenReturn(user());
        when(domainSecurityService.isPlatformAdmin()).thenReturn(true);

        mockMvc.perform(get("/api/v1/search").param("q", WORD).param("types", "people,courses,classes,organisations")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hits[0].type").value("people"))
                .andExpect(jsonPath("$.data.hits[0].uuid").value(PERSON.toString()))
                .andExpect(jsonPath("$.data.hits[0].title").value("Zephyrine Otieno"))
                .andExpect(jsonPath("$.data.totals.people").value(1))
                .andExpect(jsonPath("$.data.totals.courses").value(2))
                .andExpect(jsonPath("$.data.hits[*].uuid", hasItem(PRIVATE_CLASS.toString())))
                .andExpect(jsonPath("$.data.hits[*].uuid", hasItem(PENDING_ORGANISATION.toString())));

        mockMvc.perform(get("/api/v1/search/people").param("q", WORD).param("facets", "domains"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].uuid").value(PERSON.toString()))
                .andExpect(jsonPath("$.data.metadata.totalElements").value(1))
                .andExpect(jsonPath("$.data.facets.domains.student").value(1));
    }

    @Test
    @DisplayName("A manager's people hits are re-checked in SQL: a revoked membership disappears before the index catches up")
    void revokedMembershipDropsOutOfManagerSearch() throws Exception {
        UUID organisation = UUID.randomUUID();
        jdbc.update("INSERT INTO organisation (uuid, name, created_by) VALUES (?, 'Quillonar Org', 'test')", organisation);
        UUID manager = user();
        UUID member = user();
        membership(manager, organisation, "organisation_user");
        UUID memberMapping = membership(member, organisation, "student");
        // Written straight to the index: no trigger will ever update this document, which is exactly
        // the "index lags or indexing is failing" case.
        gateway.upsert("people", List.of(new PeopleSearchDocument(member, "Quillonar", null, "Wanjiru",
                "Quillonar Wanjiru", "quillonar@example.test", "qwanjiru", "U-2", List.of("student"),
                List.of(organisation), List.of(), true, false, false, Instant.now().getEpochSecond())));

        when(domainSecurityService.getCurrentUserUuid()).thenReturn(manager);
        when(domainSecurityService.managesOrganisation(organisation)).thenReturn(true);

        mockMvc.perform(get("/api/v1/search").param("q", "quillonar").param("types", "people"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hits[*].uuid", containsInAnyOrder(member.toString())))
                .andExpect(jsonPath("$.data.totals.people").value(1));

        jdbc.update("UPDATE user_organisation_domain_mapping SET active = false WHERE uuid = ?", memberMapping);

        mockMvc.perform(get("/api/v1/search").param("q", "quillonar").param("types", "people"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hits[*].uuid", not(hasItem(member.toString()))))
                .andExpect(jsonPath("$.data.totals.people").value(0));
        mockMvc.perform(get("/api/v1/search/people").param("q", "quillonar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.metadata.totalElements").value(0));
    }

    @Test
    @DisplayName("Per-type search filters, facets and pages over the type's allow-lists")
    void perTypeSearch() throws Exception {
        mockMvc.perform(get("/api/v1/search/organisations").param("q", WORD).param("country", "KE")
                        .param("facets", "country"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].uuid", containsInAnyOrder(VERIFIED_ORGANISATION.toString())))
                .andExpect(jsonPath("$.data.facets.country.KE").value(1));

        mockMvc.perform(get("/api/v1/search/organisations").param("q", WORD).param("facets", "slug"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/search/organisations").param("q", WORD).param("licence_no", "x"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Unknown types, short queries and bad limits are rejected")
    void rejectsBadRequests() throws Exception {
        mockMvc.perform(get("/api/v1/search").param("q", WORD).param("types", "courses,spaceships"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/search/spaceships").param("q", WORD))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/search").param("q", "z"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/search").param("q", WORD).param("limit", "21"))
                .andExpect(status().isBadRequest());
    }

    /** A real user row: the request audit log references the caller. */
    private UUID user() {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO users (uuid, user_no, first_name, last_name, email, keycloak_id, created_by) "
                        + "VALUES (?, ?, 'Test', 'User', ?, ?, 'test')",
                uuid, String.format("%09d", Math.abs(uuid.hashCode()) % 1000000000), uuid + "@test.local",
                uuid.toString());
        return uuid;
    }

    private UUID membership(UUID userUuid, UUID organisationUuid, String domainName) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO user_organisation_domain_mapping "
                        + "(uuid, user_uuid, organisation_uuid, domain_uuid, active, created_by) "
                        + "VALUES (?, ?, ?, (SELECT uuid FROM user_domain WHERE domain_name = ?), true, 'test')",
                uuid, userUuid, organisationUuid, domainName);
        return uuid;
    }

    private static CourseSearchDocument course(UUID uuid, String name, String status, boolean isPublic) {
        return new CourseSearchDocument(uuid, name, "About " + name, null, List.of(), List.of(), null, null,
                UUID.randomUUID(), "Ada Creator", status, true, isPublic, isPublic, false, new BigDecimal("100.00"),
                null, null, 0, 0, Instant.now().getEpochSecond(), null, 0, null, null, List.of(), null, null);
    }

    private static ClassSearchDocument classDocument(UUID uuid, String title, String visibility) {
        return new ClassSearchDocument(uuid, title, "About " + title, "Online", null, null, null, null,
                UUID.randomUUID(), "Some Organisation", null, null, UUID.randomUUID(), "Some Instructor", null,
                true, visibility, true, "ONLINE", "GROUP", null, null, new BigDecimal("10.00"),
                Instant.now().getEpochSecond());
    }
}
