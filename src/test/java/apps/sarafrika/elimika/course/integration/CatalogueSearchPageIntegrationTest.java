package apps.sarafrika.elimika.course.integration;

import apps.sarafrika.elimika.course.internal.search.CourseSearchSource;
import apps.sarafrika.elimika.course.internal.search.ProgramSearchSource;
import apps.sarafrika.elimika.search.config.SearchProperties;
import apps.sarafrika.elimika.search.internal.sync.SearchIndexRebuilder;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code GET /api/v1/catalogue/search} against a real PostgreSQL and Meilisearch: courses and
 * programmes seeded in SQL, indexed through the blue/green rebuild, and read back anonymously.
 * <p>
 * Seed (public): course Python for data analysis (Data, Beginner, free, 18+, 2 lessons, oldest),
 * course Python machine learning (Data, Advanced, paid), course Piano foundations (Music, Beginner,
 * paid, newest), programme Python data pathway (Data, members: the two Python courses, free).
 * Not public: a draft course, a shadow draft, an unapproved course, a draft and an unapproved programme.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("Public catalogue search (end-to-end)")
class CatalogueSearchPageIntegrationTest {

    private static final String MASTER_KEY = "catalogue-page-master-key-0123456789";
    private static final String CREATOR_SUBJECT = "keycloak-catalogue-creator";

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
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private SearchIndexRebuilder rebuilder;
    @Autowired private SearchProperties searchProperties;

    @MockBean private JwtDecoder jwtDecoder;

    private UUID creatorUuid;
    private UUID otherCreatorUuid;
    private UUID dataCategory;
    private UUID musicCategory;
    private UUID pythonData;
    private UUID pythonMl;
    private UUID piano;
    private UUID draftCourse;
    private UUID shadowDraft;
    private UUID unapprovedCourse;
    private UUID pathway;
    private UUID draftProgram;
    private UUID unapprovedProgram;

    @BeforeEach
    void seed() {
        jdbc.execute("TRUNCATE program_courses, training_programs, course_category_mappings, course_categories, "
                + "courses, course_creators, user_domain_mapping, users RESTART IDENTITY CASCADE");
        UUID creatorUser = user(CREATOR_SUBJECT, "catalogue-creator@test.local");
        jdbc.update("INSERT INTO user_domain_mapping (user_uuid, domain_uuid) "
                + "VALUES (?, (SELECT uuid FROM user_domain WHERE domain_name = 'course_creator'))", creatorUser);
        creatorUuid = courseCreator(creatorUser, "Grace Hopper");
        otherCreatorUuid = courseCreator(user("keycloak-catalogue-other", "catalogue-other@test.local"), "Clara Schumann");

        UUID beginner = difficulty("Beginner");
        UUID advanced = difficulty("Advanced");
        dataCategory = category("Data science");
        musicCategory = category("Music");

        pythonData = course("Python for data analysis", creatorUuid, "published", true, true, null, beginner,
                BigDecimal.ZERO, 18, "2024-01-01T00:00:00Z");
        jdbc.update("UPDATE courses SET thumbnail_url = 'course_thumbnails/python.jpg' WHERE uuid = ?", pythonData);
        pythonMl = course("Python machine learning", creatorUuid, "published", true, true, null, advanced,
                new BigDecimal("1500.00"), null, "2025-01-01T00:00:00Z");
        piano = course("Piano foundations", otherCreatorUuid, "published", true, true, null, beginner,
                new BigDecimal("500.00"), null, "2025-06-01T00:00:00Z");
        draftCourse = course("Python draft notes", creatorUuid, "draft", false, false, null, beginner,
                BigDecimal.ZERO, null, "2025-07-01T00:00:00Z");
        shadowDraft = course("Python for data analysis (pending edit)", creatorUuid, "draft", false, false, pythonData,
                beginner, BigDecimal.ZERO, null, "2025-07-01T00:00:00Z");
        unapprovedCourse = course("Python unapproved", creatorUuid, "published", true, false, null, beginner,
                BigDecimal.ZERO, null, "2025-07-01T00:00:00Z");
        mapCategory(pythonData, dataCategory);
        mapCategory(pythonMl, dataCategory);
        mapCategory(piano, musicCategory);
        mapCategory(draftCourse, dataCategory);
        mapCategory(unapprovedCourse, dataCategory);
        lesson(pythonData, 1);
        lesson(pythonData, 2);

        pathway = program("Python data pathway", "published", true, true, true, dataCategory, "2024-06-01T00:00:00Z");
        draftProgram = program("Python draft pathway", "draft", false, false, false, dataCategory, "2025-07-01T00:00:00Z");
        unapprovedProgram = program("Python unapproved pathway", "published", true, true, false, dataCategory,
                "2025-07-01T00:00:00Z");
        member(pathway, pythonData, 1);
        member(pathway, pythonMl, 2);

        rebuilder.rebuild(CourseSearchSource.INDEX);
        rebuilder.rebuild(ProgramSearchSource.INDEX);
    }

    @Test
    @DisplayName("A misspelt query ranks courses and programmes together, public items only")
    void typoQueryMixesCoursesAndProgrammes() throws Exception {
        catalogue(get("/api/v1/catalogue/search").param("q", "pyton"))
                .andExpect(jsonPath("$.data.content[*].uuid").value(containsInAnyOrder(
                        pythonData.toString(), pythonMl.toString(), pathway.toString())))
                .andExpect(jsonPath("$.data.content[*].type").value(hasItem("course")))
                .andExpect(jsonPath("$.data.content[*].type").value(hasItem("programme")))
                .andExpect(jsonPath("$.data.content[?(@.uuid == '" + pythonData + "')].highlight")
                        .value(hasItem(containsString("<em>"))))
                .andExpect(jsonPath("$.data.metadata.totalElements").value(3))
                .andExpect(jsonPath("$.data.facets.show.all").value(3))
                .andExpect(jsonPath("$.data.facets.show.courses").value(2))
                .andExpect(jsonPath("$.data.facets.show.programmes").value(1));
    }

    @Test
    @DisplayName("Cards carry the document fields and the live counts")
    void cardFields() throws Exception {
        String course = "$.data.content[?(@.uuid == '" + pythonData + "')]";
        String programme = "$.data.content[?(@.uuid == '" + pathway + "')]";
        catalogue(get("/api/v1/catalogue/search").param("q", "python data"))
                .andExpect(jsonPath(course + ".type").value(hasItem("course")))
                .andExpect(jsonPath(course + ".level").value(hasItem("Beginner")))
                .andExpect(jsonPath(course + ".lesson_count").value(hasItem(2)))
                .andExpect(jsonPath(course + ".class_count").value(hasItem(0)))
                .andExpect(jsonPath(course + ".course_count").value(hasItem(nullValue())))
                .andExpect(jsonPath(course + ".age_label").value(hasItem("18+")))
                .andExpect(jsonPath(course + ".is_free").value(hasItem(true)))
                .andExpect(jsonPath(course + ".creator_name").value(hasItem("Test User")))
                .andExpect(jsonPath(course + ".thumbnail_url").value(hasItem("/api/v1/files/course_thumbnails/python.jpg")))
                .andExpect(jsonPath(course + ".category_names[0]").value(hasItem("Data science")))
                .andExpect(jsonPath(programme + ".type").value(hasItem("programme")))
                .andExpect(jsonPath(programme + ".level").value(hasItem("Beginner → Advanced")))
                .andExpect(jsonPath(programme + ".course_count").value(hasItem(2)))
                .andExpect(jsonPath(programme + ".lesson_count").value(hasItem(2)))
                .andExpect(jsonPath(programme + ".class_count").value(hasItem(nullValue())))
                .andExpect(jsonPath(programme + ".thumbnail_url").value(hasItem("/api/v1/files/course_thumbnails/python.jpg")))
                .andExpect(jsonPath(programme + ".category_uuids[0]").value(hasItem(dataCategory.toString())));
    }

    @Test
    @DisplayName("show narrows the results to one type but not the show counts")
    void showFilter() throws Exception {
        catalogue(get("/api/v1/catalogue/search").param("q", "pyton").param("show", "programmes"))
                .andExpect(jsonPath("$.data.content[*].uuid").value(containsInAnyOrder(pathway.toString())))
                .andExpect(jsonPath("$.data.metadata.totalElements").value(1))
                .andExpect(jsonPath("$.data.facets.show.courses").value(2))
                .andExpect(jsonPath("$.data.facets.show.programmes").value(1));
        catalogue(get("/api/v1/catalogue/search").param("show", "courses"))
                .andExpect(jsonPath("$.data.content[*].type").value(not(hasItem("programme"))))
                .andExpect(jsonPath("$.data.metadata.totalElements").value(3))
                // Category counts follow show: the pathway is not counted under Data science.
                .andExpect(jsonPath("$.data.facets.category[?(@.name == 'Data science')].count").value(hasItem(2)));
    }

    @Test
    @DisplayName("Each facet group is counted under the other filters, without its own selection")
    void disjunctiveFacets() throws Exception {
        catalogue(get("/api/v1/catalogue/search").param("category_uuid", dataCategory.toString()).param("level", "beginner"))
                .andExpect(jsonPath("$.data.content[*].uuid").value(containsInAnyOrder(
                        pythonData.toString(), pathway.toString())))
                .andExpect(jsonPath("$.data.facets.show.all").value(2))
                .andExpect(jsonPath("$.data.facets.show.courses").value(1))
                .andExpect(jsonPath("$.data.facets.show.programmes").value(1))
                // category ignores category_uuid, honours level=beginner: Python course + pathway, Piano.
                .andExpect(jsonPath("$.data.facets.category[?(@.uuid == '" + dataCategory + "')].count").value(hasItem(2)))
                .andExpect(jsonPath("$.data.facets.category[?(@.uuid == '" + musicCategory + "')].count").value(hasItem(1)))
                // level ignores level, honours category: two beginner (course + pathway), two advanced (ML + pathway).
                .andExpect(jsonPath("$.data.facets.level.beginner").value(2))
                .andExpect(jsonPath("$.data.facets.level.intermediate").value(0))
                .andExpect(jsonPath("$.data.facets.level.advanced").value(2))
                // price honours both: the Python course and the pathway are free.
                .andExpect(jsonPath("$.data.facets.price.free").value(2))
                .andExpect(jsonPath("$.data.facets.price.paid").value(0));
    }

    @Test
    @DisplayName("Anonymous and signed-in callers alike see only the public catalogue")
    void publicOnly() throws Exception {
        for (MockHttpServletRequestBuilder request : new MockHttpServletRequestBuilder[]{
                get("/api/v1/catalogue/search").param("q", "python"),
                get("/api/v1/catalogue/search").param("q", "python").with(jwt(CREATOR_SUBJECT)),
                get("/api/v1/catalogue/search")}) {
            catalogue(request)
                    .andExpect(jsonPath("$.data.content[*].uuid").value(not(hasItem(draftCourse.toString()))))
                    .andExpect(jsonPath("$.data.content[*].uuid").value(not(hasItem(shadowDraft.toString()))))
                    .andExpect(jsonPath("$.data.content[*].uuid").value(not(hasItem(unapprovedCourse.toString()))))
                    .andExpect(jsonPath("$.data.content[*].uuid").value(not(hasItem(draftProgram.toString()))))
                    .andExpect(jsonPath("$.data.content[*].uuid").value(not(hasItem(unapprovedProgram.toString()))));
        }
        catalogue(get("/api/v1/catalogue/search"))
                .andExpect(jsonPath("$.data.metadata.totalElements").value(4));
    }

    @Test
    @DisplayName("price=paid keeps paid items; the price facet still counts free ones")
    void priceFilter() throws Exception {
        catalogue(get("/api/v1/catalogue/search").param("price", "paid"))
                .andExpect(jsonPath("$.data.content[*].uuid").value(containsInAnyOrder(
                        pythonMl.toString(), piano.toString())))
                .andExpect(jsonPath("$.data.facets.price.free").value(2))
                .andExpect(jsonPath("$.data.facets.price.paid").value(2));
    }

    @Test
    @DisplayName("sort=newest orders the merged list by creation date across both types, and pages it")
    void sortAndPaging() throws Exception {
        catalogue(get("/api/v1/catalogue/search").param("sort", "newest"))
                .andExpect(jsonPath("$.data.content[0].uuid").value(piano.toString()))
                .andExpect(jsonPath("$.data.content[1].uuid").value(pythonMl.toString()))
                .andExpect(jsonPath("$.data.content[2].uuid").value(pathway.toString()))
                .andExpect(jsonPath("$.data.content[3].uuid").value(pythonData.toString()));
        catalogue(get("/api/v1/catalogue/search").param("sort", "newest").param("page", "1").param("size", "2"))
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].uuid").value(pathway.toString()))
                .andExpect(jsonPath("$.data.content[1].uuid").value(pythonData.toString()))
                .andExpect(jsonPath("$.data.metadata.pageNumber").value(1))
                .andExpect(jsonPath("$.data.metadata.pageSize").value(2))
                .andExpect(jsonPath("$.data.metadata.totalElements").value(4))
                .andExpect(jsonPath("$.data.metadata.totalPages").value(2))
                .andExpect(jsonPath("$.data.metadata.hasNext").value(false))
                .andExpect(jsonPath("$.data.metadata.hasPrevious").value(true));
        mockMvc.perform(get("/api/v1/catalogue/search").param("size", "49")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/catalogue/search").param("sort", "cheapest")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("A hit that is no longer public in the database is dropped and the total restated")
    void staleHitsAreDropped() throws Exception {
        // Straight SQL: no entity event, so the index still lists the course as public.
        jdbc.update("UPDATE courses SET active = false, status = 'archived' WHERE uuid = ?", pythonMl);
        catalogue(get("/api/v1/catalogue/search"))
                .andExpect(jsonPath("$.data.content[*].uuid").value(not(hasItem(pythonMl.toString()))))
                .andExpect(jsonPath("$.data.metadata.totalElements").value(3));
    }

    @Test
    @DisplayName("503 \"Search is unavailable\" when the catalogue indexes cannot be read")
    void unavailable() throws Exception {
        searchProperties.getReadEnabled().put(ProgramSearchSource.INDEX, false);
        try {
            mockMvc.perform(get("/api/v1/catalogue/search").param("q", "python"))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.message").value("Search is unavailable"));
        } finally {
            searchProperties.getReadEnabled().put(ProgramSearchSource.INDEX, true);
        }
    }

    // ===== TEST PLUMBING =====

    private ResultActions catalogue(MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request).andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true));
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor jwt(String subject) {
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

    private UUID courseCreator(UUID userUuid, String name) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO course_creators (uuid, user_uuid, full_name, created_by) VALUES (?, ?, ?, 'test')",
                uuid, userUuid, name);
        return uuid;
    }

    private UUID difficulty(String name) {
        return jdbc.queryForObject("SELECT uuid FROM course_difficulty_levels WHERE name = ?", UUID.class, name);
    }

    private UUID category(String name) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO course_categories (uuid, name, created_by) VALUES (?, ?, 'test')", uuid, name);
        return uuid;
    }

    private UUID course(String name, UUID creator, String status, boolean active, boolean adminApproved,
                        UUID parentCourseUuid, UUID difficultyUuid, BigDecimal price, Integer ageLowerLimit,
                        String createdAt) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO courses (uuid, name, course_creator_uuid, status, active, admin_approved, "
                        + "parent_course_uuid, difficulty_uuid, price, age_lower_limit, created_date, created_by) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'test')",
                uuid, name, creator, status, active, adminApproved, parentCourseUuid, difficultyUuid, price,
                ageLowerLimit, Timestamp.from(Instant.parse(createdAt)));
        return uuid;
    }

    private void mapCategory(UUID course, UUID category) {
        jdbc.update("INSERT INTO course_category_mappings (uuid, course_uuid, category_uuid, created_by) "
                + "VALUES (?, ?, ?, 'test')", UUID.randomUUID(), course, category);
    }

    private void lesson(UUID course, int number) {
        jdbc.update("INSERT INTO lessons (uuid, course_uuid, lesson_number, title, status, active, created_by) "
                + "VALUES (?, ?, ?, ?, 'published', true, 'test')", UUID.randomUUID(), course, number, "Lesson " + number);
    }

    private UUID program(String title, String status, boolean published, boolean active, boolean adminApproved,
                         UUID category, String createdAt) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO training_programs (uuid, title, course_creator_uuid, status, is_published, is_active, "
                        + "admin_approved, category_uuid, created_date, created_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'test')",
                uuid, title, creatorUuid, status, published, active, adminApproved, category,
                Timestamp.from(Instant.parse(createdAt)));
        return uuid;
    }

    private void member(UUID program, UUID course, int order) {
        jdbc.update("INSERT INTO program_courses (uuid, program_uuid, course_uuid, sequence_order, is_required, created_by) "
                + "VALUES (?, ?, ?, ?, true, 'test')", UUID.randomUUID(), program, course, order);
    }
}
