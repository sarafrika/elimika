package apps.sarafrika.elimika.course.integration;

import apps.sarafrika.elimika.course.internal.search.CourseSearchSource;
import apps.sarafrika.elimika.course.internal.search.ProgramSearchSource;
import apps.sarafrika.elimika.course.internal.search.RubricSearchSource;
import apps.sarafrika.elimika.course.model.Category;
import apps.sarafrika.elimika.course.repository.CategoryRepository;
import apps.sarafrika.elimika.search.config.SearchProperties;
import apps.sarafrika.elimika.search.internal.sync.SearchIndexRebuilder;
import apps.sarafrika.elimika.shared.search.SearchGateway;
import apps.sarafrika.elimika.shared.search.SearchHit;
import apps.sarafrika.elimika.shared.search.SearchPage;
import apps.sarafrika.elimika.shared.search.SearchRequest;
import apps.sarafrika.elimika.shared.search.SearchScope;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The {@code courses}, {@code programs} and {@code rubrics} indexes against a real PostgreSQL and a
 * real Meilisearch: rows seeded in SQL, indexed through the blue/green rebuild and the durable
 * listener, and read back through the public endpoints with {@code q}, so the scopes and the
 * no-fallback rules (400 for what the index cannot express, 503 when search cannot answer) are
 * exercised exactly as callers see them.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("Course, program and rubric search (end-to-end)")
class CatalogueSearchIntegrationTest {

    private static final String MASTER_KEY = "catalogue-search-master-key-0123456789";

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
        registry.add("search.read-enabled.courses", () -> "true");
        registry.add("search.read-enabled.programs", () -> "true");
        registry.add("search.read-enabled.rubrics", () -> "true");
    }

    private static final String CREATOR_SUBJECT = "keycloak-search-creator";
    private static final String OUTSIDER_SUBJECT = "keycloak-search-outsider";
    private static final String TRAINER_SUBJECT = "keycloak-search-trainer";
    private static final String ADMIN_SUBJECT = "keycloak-search-admin";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private SearchIndexRebuilder rebuilder;
    @Autowired private SearchGateway gateway;
    @Autowired private SearchProperties searchProperties;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    @MockBean private JwtDecoder jwtDecoder;

    private UUID courseCreatorUuid;
    private UUID categoryUuid;
    private UUID liveCourse;
    private UUID draftCourse;
    private UUID shadowDraft;
    private UUID liveProgram;
    private UUID draftProgram;
    private UUID publicRubric;
    private UUID privateRubric;

    @BeforeEach
    void seed() {
        jdbc.execute("TRUNCATE program_courses, training_programs, course_rubric_associations, assessment_rubrics, "
                + "course_category_mappings, course_categories, courses, course_creators, instructors, students, "
                + "user_domain_mapping, users RESTART IDENTITY CASCADE");

        UUID creatorUserUuid = user(CREATOR_SUBJECT, "search-creator@test.local");
        grantDomain(creatorUserUuid, "course_creator");
        courseCreatorUuid = courseCreator(creatorUserUuid);
        grantDomain(user(OUTSIDER_SUBJECT, "search-outsider@test.local"), "student");
        grantDomain(user(ADMIN_SUBJECT, "search-admin@test.local"), "admin");
        grantDomain(user(TRAINER_SUBJECT, "search-trainer@test.local"), "instructor");

        categoryUuid = category("Serpent studies");
        liveCourse = course("Python for data analysis", "published", true, true, null);
        draftCourse = course("Rust systems programming", "draft", false, false, null);
        shadowDraft = course("Python for data analysis (pending edit)", "draft", false, false, liveCourse);
        jdbc.update("INSERT INTO course_category_mappings (uuid, course_uuid, category_uuid, created_by) "
                + "VALUES (?, ?, ?, 'test')", UUID.randomUUID(), liveCourse, categoryUuid);

        liveProgram = program("Machine learning pathway", "published", true, true, true);
        draftProgram = program("Kubernetes pathway", "draft", false, false, false);
        jdbc.update("INSERT INTO program_courses (uuid, program_uuid, course_uuid, sequence_order, is_required, created_by) "
                + "VALUES (?, ?, ?, 1, true, 'test')", UUID.randomUUID(), liveProgram, liveCourse);

        publicRubric = rubric("Presentation skills rubric", true);
        privateRubric = rubric("Laboratory safety rubric", false);

        rebuilder.rebuild(CourseSearchSource.INDEX);
        rebuilder.rebuild(ProgramSearchSource.INDEX);
        rebuilder.rebuild(RubricSearchSource.INDEX);
    }

    // ===== COURSES =====

    @Test
    @DisplayName("A published course is found by a misspelt query")
    void typoFindsPublishedCourse() throws Exception {
        search("/api/v1/courses/search", "pyton", OUTSIDER_SUBJECT)
                .andExpect(jsonPath("$.data.content[*].uuid").value(containsInAnyOrder(liveCourse.toString())))
                .andExpect(jsonPath("$.data.metadata.totalElements").value(1));
        search("/api/v1/courses/published", "pyton", OUTSIDER_SUBJECT)
                .andExpect(jsonPath("$.data.content[*].uuid").value(containsInAnyOrder(liveCourse.toString())));
    }

    @Test
    @DisplayName("A draft is invisible to a non-owner and visible to its owner")
    void draftVisibleOnlyToOwner() throws Exception {
        search("/api/v1/courses", "rust", OUTSIDER_SUBJECT)
                .andExpect(jsonPath("$.data.content.length()").value(0));
        search("/api/v1/courses/search", "rust", CREATOR_SUBJECT)
                .andExpect(jsonPath("$.data.content[*].uuid").value(containsInAnyOrder(draftCourse.toString())));
        // A caller's own filter narrows the scope but can never widen it.
        mockMvc.perform(get("/api/v1/courses/search").param("q", "rust").param("status", "DRAFT")
                        .with(jwt(OUTSIDER_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(0));
    }

    @Test
    @DisplayName("Shadow drafts are never indexed, not even for a platform admin")
    void shadowDraftsAreNeverIndexed() throws Exception {
        SearchPage everything = gateway.search(SearchRequest.of(CourseSearchSource.INDEX, "python", null,
                SearchScope.unrestricted("test"), 0, 100));
        assertThat(everything.hits()).extracting(SearchHit::uuid).containsExactly(liveCourse);

        search("/api/v1/courses/search", "pending edit", ADMIN_SUBJECT)
                .andExpect(jsonPath("$.data.content[*].uuid").value(not(hasItem(shadowDraft.toString()))));
    }

    @Test
    @DisplayName("Renaming a category re-indexes the courses in it")
    void categoryRenameReindexesCourses() throws Exception {
        search("/api/v1/courses/search", "herpetology", OUTSIDER_SUBJECT)
                .andExpect(jsonPath("$.data.content.length()").value(0));

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            Category category = categoryRepository.findByUuid(categoryUuid).orElseThrow();
            category.setName("Herpetology");
        });

        awaitTrue(() -> hitUuids(CourseSearchSource.INDEX, "herpetology").contains(liveCourse));
        search("/api/v1/courses/search", "herpetology", OUTSIDER_SUBJECT)
                .andExpect(jsonPath("$.data.content[*].uuid").value(containsInAnyOrder(liveCourse.toString())));
    }

    @Test
    @DisplayName("Without read routing, q answers 503 \"Search is unavailable\" instead of querying the database")
    void unavailableWhenReadsAreOff() throws Exception {
        searchProperties.getReadEnabled().put(CourseSearchSource.INDEX, false);
        try {
            mockMvc.perform(get("/api/v1/courses/search").param("q", "python").with(jwt(OUTSIDER_SUBJECT)))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("Search is unavailable"));
            // Without q the relational listing still answers from the database.
            mockMvc.perform(get("/api/v1/courses/search").param("status", "PUBLISHED").with(jwt(OUTSIDER_SUBJECT)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[*].uuid").value(containsInAnyOrder(liveCourse.toString())));
        } finally {
            searchProperties.getReadEnabled().put(CourseSearchSource.INDEX, true);
        }
    }

    @Test
    @DisplayName("Filters the UI sends with q keep working on the courses index")
    void courseFiltersSentWithQuery() throws Exception {
        for (String status : List.of("draft", "DRAFT")) {
            mockMvc.perform(get("/api/v1/courses/search").param("q", "rust")
                            .param("course_creator_uuid_eq", courseCreatorUuid.toString())
                            .param("status_eq", status)
                            .with(jwt(CREATOR_SUBJECT)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[*].uuid").value(containsInAnyOrder(draftCourse.toString())));
        }
        mockMvc.perform(get("/api/v1/courses/search").param("q", "python").param("lifecycle_stage", "published")
                        .param("is_published", "true").param("status_noteq", "draft")
                        .with(jwt(OUTSIDER_SUBJECT)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/courses/search").param("q", "python").param("lifecycle_stage", "published")
                        .with(jwt(OUTSIDER_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].uuid").value(containsInAnyOrder(liveCourse.toString())));
        mockMvc.perform(get("/api/v1/courses/search").param("q", "python").param("is_published", "false")
                        .with(jwt(ADMIN_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(0));
    }

    @Test
    @DisplayName("A filter or sort the courses index cannot express is a 400 naming it, never a database query")
    void unsupportedKeysAreRejected() throws Exception {
        mockMvc.perform(get("/api/v1/courses/search").param("q", "python").param("category_name", "serpent")
                        .with(jwt(OUTSIDER_SUBJECT)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("category_name")));
        mockMvc.perform(get("/api/v1/courses/search").param("q", "python").param("sort", "lastModifiedDate,desc")
                        .with(jwt(OUTSIDER_SUBJECT)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("lastModifiedDate")));
        mockMvc.perform(get("/api/v1/courses/search").param("name_like", "python").with(jwt(OUTSIDER_SUBJECT)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString(
                        "Text operators were removed; use the q parameter for text search")));
        // Without q, category_name is gone from the relational filters too.
        mockMvc.perform(get("/api/v1/courses/search").param("category_name", "serpent").with(jwt(OUTSIDER_SUBJECT)))
                .andExpect(status().isBadRequest());
    }

    // ===== PROGRAMS =====

    @Test
    @DisplayName("Programs are found by title typos and member course names, within the program scope")
    void programSearch() throws Exception {
        search("/api/v1/programs/search", "machin lerning", OUTSIDER_SUBJECT)
                .andExpect(jsonPath("$.data.content[*].uuid").value(containsInAnyOrder(liveProgram.toString())));
        search("/api/v1/programs", "python", OUTSIDER_SUBJECT)
                .andExpect(jsonPath("$.data.content[*].uuid").value(containsInAnyOrder(liveProgram.toString())));
        search("/api/v1/programs", "kubernetes", OUTSIDER_SUBJECT)
                .andExpect(jsonPath("$.data.content.length()").value(0));
        search("/api/v1/programs/search", "kubernetes", CREATOR_SUBJECT)
                .andExpect(jsonPath("$.data.content[*].uuid").value(containsInAnyOrder(draftProgram.toString())));
    }

    @Test
    @DisplayName("The admin pending-programs queue takes q through the programs index and stays a queue")
    void pendingProgramQueueSearch() throws Exception {
        UUID pending = program("Quantum computing pathway", "in_review", false, true, false);
        rebuilder.rebuild(ProgramSearchSource.INDEX);

        search("/api/v1/admin/programs/pending", "quantm", ADMIN_SUBJECT)
                .andExpect(jsonPath("$.data.content[*].uuid").value(containsInAnyOrder(pending.toString())));
        // An approved, live program matches the text but is not waiting on approval.
        search("/api/v1/admin/programs/pending", "machine", ADMIN_SUBJECT)
                .andExpect(jsonPath("$.data.content.length()").value(0));
        mockMvc.perform(get("/api/v1/admin/programs/pending").with(jwt(ADMIN_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].uuid").value(containsInAnyOrder(pending.toString())));
        mockMvc.perform(get("/api/v1/admin/programs/pending").param("q", "quantum").with(jwt(OUTSIDER_SUBJECT)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Filters the UI sends with q keep working on the programs index")
    void programFiltersSentWithQuery() throws Exception {
        for (String status : List.of("draft", "DRAFT")) {
            mockMvc.perform(get("/api/v1/programs/search").param("q", "kubernetes")
                            .param("course_creator_uuid_eq", courseCreatorUuid.toString())
                            .param("status_eq", status)
                            .with(jwt(CREATOR_SUBJECT)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[*].uuid").value(containsInAnyOrder(draftProgram.toString())));
        }
        mockMvc.perform(get("/api/v1/programs/search").param("q", "kubernetes").param("status_eq", "published")
                        .with(jwt(CREATOR_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(0));
        mockMvc.perform(get("/api/v1/programs/search").param("q", "kubernetes").param("title_like", "kube")
                        .with(jwt(CREATOR_SUBJECT)))
                .andExpect(status().isBadRequest());
    }

    // ===== RUBRICS =====

    @Test
    @DisplayName("Rubric discovery finds public rubrics; private ones stay with their author")
    void rubricSearch() throws Exception {
        search("/api/v1/rubrics/discovery/search", "presentaton", OUTSIDER_SUBJECT)
                .andExpect(jsonPath("$.data.content[*].uuid").value(containsInAnyOrder(publicRubric.toString())));
        search("/api/v1/rubrics/discovery/search", "laboratory", CREATOR_SUBJECT)
                .andExpect(jsonPath("$.data.content.length()").value(0));
        // Rubric listings are management-only; an unrelated instructor sees public rubrics only.
        search("/api/v1/rubrics/search", "presentation", TRAINER_SUBJECT)
                .andExpect(jsonPath("$.data.content[*].uuid").value(containsInAnyOrder(publicRubric.toString())));
        search("/api/v1/rubrics/search", "laboratory", TRAINER_SUBJECT)
                .andExpect(jsonPath("$.data.content.length()").value(0));
        search("/api/v1/rubrics/search", "laboratry", CREATOR_SUBJECT)
                .andExpect(jsonPath("$.data.content[*].uuid").value(containsInAnyOrder(privateRubric.toString())));
    }

    @Test
    @DisplayName("Rubric discovery by type is an exact, case-insensitive match, with q through the index and without q in SQL")
    void rubricDiscoveryByType() throws Exception {
        for (String type : List.of("assignment", "ASSIGNMENT")) {
            mockMvc.perform(get("/api/v1/rubrics/discovery/search").param("q", "presentation").param("type", type)
                            .with(jwt(OUTSIDER_SUBJECT)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[*].uuid").value(containsInAnyOrder(publicRubric.toString())));
            mockMvc.perform(get("/api/v1/rubrics/discovery/search").param("type", type).with(jwt(OUTSIDER_SUBJECT)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[*].uuid").value(containsInAnyOrder(publicRubric.toString())));
        }
        // A part of the type no longer matches: the type is an exact filter, not a substring search.
        mockMvc.perform(get("/api/v1/rubrics/discovery/search").param("type", "assign").with(jwt(OUTSIDER_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(0));
        mockMvc.perform(get("/api/v1/rubrics/discovery/search").param("q", "presentation").param("type", "assign")
                        .with(jwt(OUTSIDER_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(0));

        searchProperties.getReadEnabled().put(RubricSearchSource.INDEX, false);
        try {
            mockMvc.perform(get("/api/v1/rubrics/discovery/search").param("q", "presentation").with(jwt(OUTSIDER_SUBJECT)))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.message").value("Search is unavailable"));
        } finally {
            searchProperties.getReadEnabled().put(RubricSearchSource.INDEX, true);
        }
    }

    // ===== TEST PLUMBING =====

    private ResultActions search(String path, String q, String subject) throws Exception {
        return mockMvc.perform(get(path).param("q", q).with(jwt(subject))).andExpect(status().isOk());
    }

    private List<UUID> hitUuids(String index, String text) {
        return gateway.search(SearchRequest.of(index, text, null, SearchScope.unrestricted("test"), 0, 100))
                .hits().stream().map(SearchHit::uuid).toList();
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

    private RequestPostProcessor jwt(String subject) {
        return org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                .jwt().jwt(builder -> builder.subject(subject).claim("sub", subject));
    }

    private UUID user(String keycloakId, String email) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO users (uuid, user_no, first_name, last_name, email, keycloak_id, created_by) "
                        + "VALUES (?, ?, 'Test', 'User', ?, ?, 'test')",
                uuid, String.format("%09d", Math.abs(uuid.hashCode()) % 1000000000), email, keycloakId);
        return uuid;
    }

    private void grantDomain(UUID userUuid, String domainName) {
        jdbc.update("INSERT INTO user_domain_mapping (user_uuid, domain_uuid) "
                + "VALUES (?, (SELECT uuid FROM user_domain WHERE domain_name = ?))", userUuid, domainName);
    }

    private UUID courseCreator(UUID userUuid) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO course_creators (uuid, user_uuid, full_name, created_by) "
                + "VALUES (?, ?, 'Grace Hopper', 'test')", uuid, userUuid);
        return uuid;
    }

    private UUID category(String name) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO course_categories (uuid, name, created_by) VALUES (?, ?, 'test')", uuid, name);
        return uuid;
    }

    private UUID course(String name, String status, boolean active, boolean adminApproved, UUID parentCourseUuid) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO courses (uuid, name, course_creator_uuid, status, active, admin_approved, "
                        + "parent_course_uuid, created_by) VALUES (?, ?, ?, ?, ?, ?, ?, 'test')",
                uuid, name, courseCreatorUuid, status, active, adminApproved, parentCourseUuid);
        return uuid;
    }

    private UUID program(String title, String status, boolean published, boolean active, boolean adminApproved) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO training_programs (uuid, title, course_creator_uuid, status, is_published, is_active, "
                        + "admin_approved, created_by) VALUES (?, ?, ?, ?, ?, ?, ?, 'test')",
                uuid, title, courseCreatorUuid, status, published, active, adminApproved);
        return uuid;
    }

    private UUID rubric(String title, boolean isPublic) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO assessment_rubrics (uuid, title, rubric_type, course_creator_uuid, is_public, "
                        + "is_active, status, created_by) VALUES (?, ?, 'Assignment', ?, ?, true, 'published', 'test')",
                uuid, title, courseCreatorUuid, isPublic);
        return uuid;
    }
}
