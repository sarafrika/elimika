package apps.sarafrika.elimika.course.integration;

import apps.sarafrika.elimika.course.internal.search.CourseContentSearchSource;
import apps.sarafrika.elimika.search.config.SearchProperties;
import apps.sarafrika.elimika.search.internal.sync.SearchIndexRebuilder;
import apps.sarafrika.elimika.shared.search.SearchGateway;
import apps.sarafrika.elimika.shared.search.SearchHit;
import apps.sarafrika.elimika.shared.search.SearchPage;
import apps.sarafrika.elimika.shared.search.SearchRequest;
import apps.sarafrika.elimika.shared.search.SearchScope;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The {@code course_content} index against a real PostgreSQL and a real Meilisearch: in-course search,
 * the {@code course_content} global search type, the learner/manager boundary and what the documents
 * may never contain.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("In-course content search (end-to-end)")
class CourseContentSearchIntegrationTest {

    private static final String MASTER_KEY = "course-content-search-master-key-0123456789";
    private static final String ANSWER_TEXT = "Quokkamitochondrion";
    private static final String QUESTION_TEXT = "Which organelle hosts the Krebsfeather cycle";
    private static final String FILE_URL = "course-content/secret-worksheet-file.pdf";

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
        registry.add("search.read-enabled.course_content", () -> "true");
    }

    private static final String CREATOR_SUBJECT = "keycloak-content-creator";
    private static final String LEARNER_SUBJECT = "keycloak-content-learner";
    private static final String OUTSIDER_SUBJECT = "keycloak-content-outsider";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private SearchIndexRebuilder rebuilder;
    @Autowired private SearchGateway gateway;
    @Autowired private SearchProperties searchProperties;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private JwtDecoder jwtDecoder;

    private UUID course;
    private UUID shadowCourse;
    private UUID publishedLesson;
    private UUID draftLesson;
    private UUID contentItem;
    private UUID quiz;
    private UUID classQuiz;
    private UUID assignment;
    private UUID shadowLesson;

    @BeforeEach
    void seed() {
        jdbc.execute("TRUNCATE quiz_question_options, quiz_questions, quizzes, assignments, lesson_contents, lessons, "
                + "course_enrollments, students, courses, course_creators, user_domain_mapping, users "
                + "RESTART IDENTITY CASCADE");

        UUID creatorUser = user(CREATOR_SUBJECT, "content-creator@test.local");
        grantDomain(creatorUser, "course_creator");
        UUID courseCreatorUuid = UUID.randomUUID();
        jdbc.update("INSERT INTO course_creators (uuid, user_uuid, full_name, created_by) VALUES (?, ?, 'Ada Author', 'test')",
                courseCreatorUuid, creatorUser);

        UUID learnerUser = user(LEARNER_SUBJECT, "content-learner@test.local");
        grantDomain(learnerUser, "student");
        UUID learner = student(learnerUser);
        UUID outsiderUser = user(OUTSIDER_SUBJECT, "content-outsider@test.local");
        grantDomain(outsiderUser, "student");
        student(outsiderUser);

        course = course("Cell biology", courseCreatorUuid, null);
        shadowCourse = course("Cell biology (pending edit)", courseCreatorUuid, course);
        jdbc.update("INSERT INTO course_enrollments (uuid, student_uuid, course_uuid, status, created_by) "
                + "VALUES (?, ?, ?, 'active', 'test')", UUID.randomUUID(), learner, course);

        publishedLesson = lesson(course, 1, "Photosynthesis fundamentals", "published", true,
                "How chloroplasts capture sunlight");
        draftLesson = lesson(course, 2, "Cellular respiration", "draft", false, "Glycolysis and beyond");
        shadowLesson = lesson(shadowCourse, 1, "Photosynthesis fundamentals revised", "draft", false, "Pending edit");

        contentItem = UUID.randomUUID();
        jdbc.update("INSERT INTO lesson_contents (uuid, lesson_uuid, content_type_uuid, title, description, content_text, "
                        + "file_url, display_order, created_by) VALUES (?, ?, (SELECT uuid FROM lesson_content_types "
                        + "WHERE name = 'Text'), 'Reading: the light reactions', 'Thylakoid notes', "
                        + "'<p>The <b>thylakoid</b> membrane hosts photosystem II.</p>', ?, 1, 'test')",
                contentItem, publishedLesson, FILE_URL);

        quiz = quiz(publishedLesson, "Photosynthesis check", "COURSE_TEMPLATE");
        classQuiz = quiz(publishedLesson, "Photosynthesis class drill", "CLASS_CLONE");
        UUID question = UUID.randomUUID();
        jdbc.update("INSERT INTO quiz_questions (uuid, quiz_uuid, question_text, question_type, points, display_order, "
                + "created_by) VALUES (?, ?, ?, 'multiple_choice', 1.00, 1, 'test')", question, quiz, QUESTION_TEXT);
        jdbc.update("INSERT INTO quiz_question_options (uuid, question_uuid, option_text, is_correct, display_order, "
                + "created_by) VALUES (?, ?, ?, true, 1, 'test')", UUID.randomUUID(), question, ANSWER_TEXT);

        assignment = UUID.randomUUID();
        jdbc.update("INSERT INTO assignments (uuid, lesson_uuid, title, description, instructions, is_published, "
                        + "created_by) VALUES (?, ?, 'Photosynthesis lab report', 'Measure oxygen output', "
                        + "'Submit a two page report', true, 'test')", assignment, publishedLesson);

        rebuilder.rebuild(CourseContentSearchSource.INDEX);
    }

    @Test
    @DisplayName("An enrolled learner finds a published lesson by a misspelt word, in the course and in global search")
    void enrolledLearnerFindsPublishedLessonByTypo() throws Exception {
        mockMvc.perform(get(searchPath(course)).param("q", "photosynthsis").param("types", "lesson")
                        .with(jwt(LEARNER_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].uuid").value(containsInAnyOrder(publishedLesson.toString())))
                .andExpect(jsonPath("$.data.content[0].type").value("lesson"))
                .andExpect(jsonPath("$.data.content[0].lesson_number").value(1))
                .andExpect(jsonPath("$.data.content[0].lesson_title").value("Photosynthesis fundamentals"))
                .andExpect(jsonPath("$.data.content[0].highlight").value(containsString("<em>")));

        // Every learner-visible type, and never the class-specific quiz.
        mockMvc.perform(get(searchPath(course)).param("q", "photosynthesis").with(jwt(LEARNER_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].uuid").value(containsInAnyOrder(
                        publishedLesson.toString(), quiz.toString(), assignment.toString())));
        mockMvc.perform(get(searchPath(course)).param("q", "thylakoid").with(jwt(LEARNER_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].uuid").value(containsInAnyOrder(contentItem.toString())));

        mockMvc.perform(get("/api/v1/search").param("q", "photosynthsis").param("types", "course_content")
                        .with(jwt(LEARNER_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hits[*].uuid").value(containsInAnyOrder(
                        publishedLesson.toString(), quiz.toString(), assignment.toString())))
                .andExpect(jsonPath("$.data.hits[0].subtitle").value("Lesson 1 · Cell biology"));
    }

    @Test
    @DisplayName("An unenrolled learner gets 403 on the course and no global hits; anonymous callers get no global hits")
    void unenrolledLearnerIsRefused() throws Exception {
        mockMvc.perform(get(searchPath(course)).param("q", "photosynthesis").with(jwt(OUTSIDER_SUBJECT)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/search").param("q", "photosynthesis").param("types", "course_content")
                        .with(jwt(OUTSIDER_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hits.length()").value(0));
        mockMvc.perform(get("/api/v1/search").param("q", "photosynthesis").param("types", "course_content"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hits.length()").value(0));
    }

    @Test
    @DisplayName("A draft lesson is visible only to the course's manager")
    void draftLessonVisibleOnlyToManager() throws Exception {
        mockMvc.perform(get(searchPath(course)).param("q", "respiration").with(jwt(LEARNER_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(0));
        mockMvc.perform(get(searchPath(course)).param("q", "respiration").with(jwt(CREATOR_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].uuid").value(containsInAnyOrder(draftLesson.toString())));
        mockMvc.perform(get("/api/v1/search").param("q", "respiration").param("types", "course_content")
                        .with(jwt(CREATOR_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hits[*].uuid").value(containsInAnyOrder(draftLesson.toString())));
        // The manager also sees the class-specific quiz.
        mockMvc.perform(get(searchPath(course)).param("q", "drill").with(jwt(CREATOR_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].uuid").value(containsInAnyOrder(classQuiz.toString())));
    }

    @Test
    @DisplayName("A lesson unpublished behind the index's back is dropped by the SQL re-check")
    void sqlRecheckDropsStaleHits() throws Exception {
        // A JDBC write fires no trigger, so the index still says published.
        jdbc.update("UPDATE lessons SET status = 'draft', active = false WHERE uuid = ?", publishedLesson);
        mockMvc.perform(get(searchPath(course)).param("q", "fundamentals").param("types", "lesson")
                        .with(jwt(LEARNER_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(0));
        mockMvc.perform(get("/api/v1/search").param("q", "fundamentals").param("types", "course_content")
                        .with(jwt(LEARNER_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hits.length()").value(0));
    }

    @Test
    @DisplayName("No quiz answer, question text or file URL is ever in the index, and shadow drafts are left out")
    void rawDocumentsCarryNoAnswers() throws Exception {
        SearchPage everything = gateway.search(new SearchRequest(CourseContentSearchSource.INDEX, null, null,
                SearchScope.unrestricted("test"), List.of(), 0, 100, List.of(), null));
        assertThat(everything.hits()).extracting(SearchHit::uuid).containsExactlyInAnyOrder(
                publishedLesson, draftLesson, contentItem, quiz, classQuiz, assignment);
        assertThat(everything.hits()).extracting(SearchHit::uuid).doesNotContain(shadowLesson);
        for (SearchHit hit : everything.hits()) {
            Map<String, Object> document = hit.document();
            String raw = objectMapper.writeValueAsString(document);
            assertThat(raw).doesNotContainIgnoringCase(ANSWER_TEXT)
                    .doesNotContainIgnoringCase("Krebsfeather")
                    .doesNotContain(FILE_URL)
                    .doesNotContain("<p>");
            assertThat(document).doesNotContainKeys("file_url", "questions", "options", "is_correct", "rubric_uuid");
        }
        assertThat(gateway.search(SearchRequest.of(CourseContentSearchSource.INDEX, ANSWER_TEXT, null,
                SearchScope.unrestricted("test"), 0, 10)).hits()).isEmpty();
    }

    @Test
    @DisplayName("q is required (400), an unknown type is a 400, and with reads off q answers 503")
    void validationAndAvailability() throws Exception {
        mockMvc.perform(get(searchPath(course)).with(jwt(LEARNER_SUBJECT)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(searchPath(course)).param("q", "photosynthesis").param("types", "questions")
                        .with(jwt(LEARNER_SUBJECT)))
                .andExpect(status().isBadRequest());
        searchProperties.getReadEnabled().put(CourseContentSearchSource.INDEX, false);
        try {
            mockMvc.perform(get(searchPath(course)).param("q", "photosynthesis").with(jwt(LEARNER_SUBJECT)))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.message").value("Search is unavailable"));
        } finally {
            searchProperties.getReadEnabled().put(CourseContentSearchSource.INDEX, true);
        }
    }

    // ===== TEST PLUMBING =====

    private static String searchPath(UUID courseUuid) {
        return "/api/v1/courses/" + courseUuid + "/content/search";
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

    private UUID student(UUID userUuid) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO students (uuid, user_uuid, full_name, created_by) VALUES (?, ?, 'Test Learner', 'test')",
                uuid, userUuid);
        return uuid;
    }

    private UUID course(String name, UUID courseCreatorUuid, UUID parentCourseUuid) {
        UUID uuid = UUID.randomUUID();
        boolean live = parentCourseUuid == null;
        jdbc.update("INSERT INTO courses (uuid, name, course_creator_uuid, status, active, admin_approved, "
                        + "parent_course_uuid, created_by) VALUES (?, ?, ?, ?, ?, ?, ?, 'test')",
                uuid, name, courseCreatorUuid, live ? "published" : "draft", live, live, parentCourseUuid);
        return uuid;
    }

    private UUID lesson(UUID courseUuid, int number, String title, String status, boolean active, String description) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO lessons (uuid, course_uuid, lesson_number, title, description, status, active, created_by) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, 'test')", uuid, courseUuid, number, title, description, status, active);
        return uuid;
    }

    private UUID quiz(UUID lessonUuid, String title, String scope) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO quizzes (uuid, lesson_uuid, title, attempts_allowed, passing_score, status, active, "
                + "scope, created_by) VALUES (?, ?, ?, 3, 50.00, 'published', true, ?, 'test')",
                uuid, lessonUuid, title, scope);
        return uuid;
    }
}
