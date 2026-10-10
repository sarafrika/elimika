package apps.sarafrika.elimika.course.integration;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Course list items carry lesson count, rating summary and creator name, loaded with one aggregate
 * query each per page: the statement count must not grow with the number of courses listed.
 */
// The container dies with this class; a cached context must not outlive it.
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("Course list enrichment (end-to-end)")
class CourseListEnrichmentIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

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
        registry.add("spring.jpa.properties.hibernate.generate_statistics", () -> "true");
    }

    private static final String ADMIN_SUBJECT = "keycloak-enrich-admin";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManagerFactory entityManagerFactory;

    @MockBean private JwtDecoder jwtDecoder;

    private UUID creatorUuid;
    private UUID secondCreatorUuid;

    @BeforeEach
    void seed() {
        jdbc.execute("TRUNCATE course_reviews, lessons, course_category_mappings, course_categories, courses, course_creators, students, "
                + "user_domain_mapping, users RESTART IDENTITY CASCADE");
        grantDomain(user(ADMIN_SUBJECT, "enrich-admin@test.local"), "admin");
        creatorUuid = courseCreator(user("enrich-creator-1", "enrich-creator-1@test.local", "Jane", "Wanjiku"), "Jane Wanjiku");
        secondCreatorUuid = courseCreator(user("enrich-creator-2", "enrich-creator-2@test.local", "Otieno", "Ouma"), "Otieno Ouma");
    }

    @Test
    @DisplayName("List items carry lesson count, rounded rating summary and creator name")
    void listItemsCarryAggregates() throws Exception {
        UUID reviewed = course("Reviewed", creatorUuid);
        lesson(reviewed, 1);
        lesson(reviewed, 2);
        lesson(reviewed, 3);
        review(reviewed, 5);
        review(reviewed, 4);
        review(reviewed, 4);
        UUID categoryUuid = category("Data science");
        categorise(reviewed, categoryUuid);
        UUID bare = course("Bare", secondCreatorUuid);

        mockMvc.perform(get("/api/v1/courses/search").param("course_creator_uuid_eq", creatorUuid.toString()).with(jwt(ADMIN_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].lesson_count").value(3))
                .andExpect(jsonPath("$.data.content[0].rating_summary.average").value(4.3))
                .andExpect(jsonPath("$.data.content[0].rating_summary.count").value(3))
                .andExpect(jsonPath("$.data.content[0].course_creator_name").value("Jane Wanjiku"))
                .andExpect(jsonPath("$.data.content[0].category_uuids[0]").value(categoryUuid.toString()))
                .andExpect(jsonPath("$.data.content[0].category_names[0]").value("Data science"));

        mockMvc.perform(get("/api/v1/courses/search").param("course_creator_uuid_eq", secondCreatorUuid.toString()).with(jwt(ADMIN_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].lesson_count").value(0))
                .andExpect(jsonPath("$.data.content[0].rating_summary.average").value(nullValue()))
                .andExpect(jsonPath("$.data.content[0].rating_summary.count").value(0))
                .andExpect(jsonPath("$.data.content[0].course_creator_name").value("Otieno Ouma"));
    }

    @Test
    @DisplayName("The statement count of a list page does not grow with the number of courses")
    void statementCountIsIndependentOfPageSize() throws Exception {
        UUID first = course("First", creatorUuid);
        categorise(first, category("First category"));
        lesson(first, 1);
        review(first, 5);
        long onePage = statementsFor();

        for (int i = 0; i < 5; i++) {
            UUID extra = course("Extra " + i, i % 2 == 0 ? creatorUuid : secondCreatorUuid);
            categorise(extra, category("Category " + i));
            lesson(extra, 1);
            lesson(extra, 2);
            review(extra, 3);
        }
        long sixPage = statementsFor();

        assertThat(sixPage).isEqualTo(onePage);
    }

    private long statementsFor() throws Exception {
        mockMvc.perform(get("/api/v1/courses").with(jwt(ADMIN_SUBJECT))).andExpect(status().isOk());
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();
        mockMvc.perform(get("/api/v1/courses").with(jwt(ADMIN_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[*].lesson_count").isNotEmpty());
        return statistics.getPrepareStatementCount();
    }

    // ===== TEST PLUMBING =====

    private RequestPostProcessor jwt(String subject) {
        return org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                .jwt().jwt(builder -> builder.subject(subject).claim("sub", subject));
    }

    private UUID user(String keycloakId, String email) {
        return user(keycloakId, email, "Test", "User");
    }

    private UUID user(String keycloakId, String email, String firstName, String lastName) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO users (uuid, user_no, first_name, last_name, email, keycloak_id, created_by) "
                + "VALUES (?, ?, ?, ?, ?, ?, 'test')",
                uuid, String.format("%09d", Math.abs(uuid.hashCode()) % 1000000000), firstName, lastName, email,
                keycloakId);
        return uuid;
    }

    private void grantDomain(UUID userUuid, String domainName) {
        jdbc.update("INSERT INTO user_domain_mapping (user_uuid, domain_uuid) "
                + "VALUES (?, (SELECT uuid FROM user_domain WHERE domain_name = ?))", userUuid, domainName);
    }

    private UUID courseCreator(UUID userUuid, String fullName) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO course_creators (uuid, user_uuid, full_name, created_by) VALUES (?, ?, ?, 'test')",
                uuid, userUuid, fullName);
        return uuid;
    }

    private UUID course(String name, UUID courseCreatorUuid) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO courses (uuid, name, course_creator_uuid, status, active, admin_approved, created_by) "
                + "VALUES (?, ?, ?, 'published', true, true, 'test')", uuid, name, courseCreatorUuid);
        return uuid;
    }

    private UUID category(String name) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO course_categories (uuid, name, created_by) VALUES (?, ?, 'test')", uuid, name);
        return uuid;
    }

    private void categorise(UUID courseUuid, UUID categoryUuid) {
        jdbc.update("INSERT INTO course_category_mappings (uuid, course_uuid, category_uuid, created_by) "
                + "VALUES (?, ?, ?, 'test')", UUID.randomUUID(), courseUuid, categoryUuid);
    }

    private void lesson(UUID courseUuid, int number) {
        jdbc.update("INSERT INTO lessons (uuid, course_uuid, lesson_number, title, status, active, created_by) "
                + "VALUES (?, ?, ?, ?, 'published', true, 'test')",
                UUID.randomUUID(), courseUuid, number, "Lesson " + number);
    }

    private void review(UUID courseUuid, int rating) {
        UUID studentUuid = UUID.randomUUID();
        UUID studentUser = user(studentUuid.toString(), studentUuid + "@test.local");
        jdbc.update("INSERT INTO students (uuid, user_uuid, full_name, created_by) VALUES (?, ?, 'Student', 'test')",
                studentUuid, studentUser);
        jdbc.update("INSERT INTO course_reviews (course_uuid, student_uuid, rating, created_by) VALUES (?, ?, ?, 'test')",
                courseUuid, studentUuid, rating);
    }
}
