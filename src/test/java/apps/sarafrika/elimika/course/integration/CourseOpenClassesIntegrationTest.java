package apps.sarafrika.elimika.course.integration;

import apps.sarafrika.elimika.course.internal.search.CourseSearchSource;
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
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code GET /api/v1/courses/{uuid}/open-classes} and the catalogue's {@code price_from} /
 * {@code open_class_count}, over real HTTP with no token, against a real PostgreSQL and Meilisearch.
 * <p>
 * The signed-out course page used to show no classes (the class search needs a token) and priced the
 * course from {@code courses.price}, which is usually 0 - "Starting from Free" for a paid course. A
 * learner pays the class fee, so the page now reads the joinable classes and their lowest fee here.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("Open classes of a public course (end-to-end)")
class CourseOpenClassesIntegrationTest {

    private static final String MASTER_KEY = "open-classes-master-key-0123456789";

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
        registry.add("commerce.internal.default-currency", () -> "KES");

        registry.add("search.enabled", () -> "true");
        registry.add("search.meilisearch.host",
                () -> "http://" + meilisearch.getHost() + ":" + meilisearch.getMappedPort(7700));
        registry.add("search.meilisearch.api-key", () -> MASTER_KEY);
        registry.add("search.read-enabled.courses", () -> "true");
        registry.add("search.read-enabled.programs", () -> "true");
    }

    /** Distinctive coordinates, so a leak anywhere in the JSON is found by plain text search. */
    private static final String LATITUDE = "-1.2345678";
    private static final String LONGITUDE = "36.8765432";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private SearchIndexRebuilder rebuilder;

    @MockBean private JwtDecoder jwtDecoder;

    private UUID publicCourse;
    private UUID unpublishedCourse;
    private UUID unapprovedCourse;
    private UUID cheapClass;
    private UUID onlineClass;

    @BeforeEach
    void seed() {
        jdbc.execute("TRUNCATE class_enrollments, scheduled_instances, class_definitions, program_courses, "
                + "training_programs, courses, course_creators, instructors, students, user_domain_mapping, users "
                + "RESTART IDENTITY CASCADE");

        UUID creatorUuid = courseCreator(user("open-classes-creator", "creator@test.local"));
        UUID instructorUuid = instructor(user("open-classes-instructor", "instructor@test.local"));
        publicCourse = course("Organic gardening", creatorUuid, "published", true);
        unpublishedCourse = course("Hydroponics", creatorUuid, "draft", false);
        unapprovedCourse = course("Composting", creatorUuid, "published", false);

        // Joinable: in person, KES 2,000, 10 seats, three taken (a cancelled and a waitlisted learner
        // hold no seat). Starts in ten days.
        cheapClass = classDefinition(publicCourse, instructorUuid, "Saturday cohort", "IN_PERSON", "PUBLIC", true,
                "2000.00", 10, "Kenya School of Government, Lower Kabete Road, Nairobi, Kenya", 30, 10, 60);
        UUID session = session(cheapClass, instructorUuid);
        enrol(session, learner("one"), "ENROLLED");
        enrol(session, learner("two"), "ENROLLED");
        enrol(session, learner("three"), "ATTENDED");
        enrol(session, learner("four"), "CANCELLED");
        enrol(session, learner("five"), "WAITLISTED");

        // Joinable: online, KES 5,000, starts sooner. Sorted after the cheaper class all the same.
        onlineClass = classDefinition(publicCourse, instructorUuid, "Evening online", "ONLINE", "PUBLIC", true,
                "5000.00", 5, null, 30, 5, 90);

        // Not joinable, each cheaper than both above so a leak would also move price_from.
        classDefinition(publicCourse, instructorUuid, "Private cohort", "ONLINE", "PRIVATE", true,
                "100.00", 5, null, 30, 5, 90);
        classDefinition(publicCourse, instructorUuid, "Retired cohort", "ONLINE", "PUBLIC", false,
                "50.00", 5, null, 30, 5, 90);
        classDefinition(publicCourse, instructorUuid, "Registration closed", "ONLINE", "PUBLIC", true,
                "10.00", 5, null, -1, 5, 90);
        classDefinition(publicCourse, instructorUuid, "Already finished", "ONLINE", "PUBLIC", true,
                "20.00", 5, null, 30, -60, -1);

        classDefinition(unpublishedCourse, instructorUuid, "Draft course class", "ONLINE", "PUBLIC", true,
                "1000.00", 5, null, 30, 5, 90);

        rebuilder.rebuild(CourseSearchSource.INDEX);
    }

    @Test
    @DisplayName("An anonymous caller gets the joinable classes, cheapest first, with price_from the minimum fee")
    void anonymousCallerSeesOpenClasses() throws Exception {
        mockMvc.perform(get(url(publicCourse)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.open_class_count").value(2))
                .andExpect(jsonPath("$.data.price_from").value(2000.00))
                .andExpect(jsonPath("$.data.currency_code").value("KES"))
                .andExpect(jsonPath("$.data.classes[*].uuid").value(contains(cheapClass.toString(), onlineClass.toString())))
                .andExpect(jsonPath("$.data.classes[0].fee").value(2000.00))
                .andExpect(jsonPath("$.data.classes[0].currency_code").value("KES"))
                .andExpect(jsonPath("$.data.classes[0].location_type").value("IN_PERSON"))
                .andExpect(jsonPath("$.data.classes[0].session_format").value("GROUP"))
                .andExpect(jsonPath("$.data.classes[0].place_name").value("Kenya School of Government"))
                .andExpect(jsonPath("$.data.classes[0].area").value("Lower Kabete Road, Nairobi"))
                .andExpect(jsonPath("$.data.classes[0].max_participants").value(10))
                .andExpect(jsonPath("$.data.classes[0].seats_left").value(7))
                .andExpect(jsonPath("$.data.classes[0].starts_on").exists())
                .andExpect(jsonPath("$.data.classes[0].ends_on").exists())
                .andExpect(jsonPath("$.data.classes[0].registration_closes_on").exists())
                .andExpect(jsonPath("$.data.classes[1].seats_left").value(5))
                .andExpect(jsonPath("$.data.classes[1].place_name").doesNotExist());
    }

    @Test
    @DisplayName("No coordinates, meeting links, instructor or revenue data reach the wire")
    void nothingPrivateIsSerialised() throws Exception {
        String body = mockMvc.perform(get(url(publicCourse)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(body)
                .doesNotContain(LATITUDE)
                .doesNotContain(LONGITUDE)
                .doesNotContain("latitude")
                .doesNotContain("longitude")
                .doesNotContain("meeting_link")
                .doesNotContain("instructor")
                .doesNotContain("organisation")
                .doesNotContain("revenue")
                .doesNotContain("share_percentage");
    }

    @Test
    @DisplayName("A course that is not public is a 404, even when it has public classes")
    void nonPublicCourseIsNotFound() throws Exception {
        mockMvc.perform(get(url(unpublishedCourse))).andExpect(status().isNotFound());
        mockMvc.perform(get(url(unapprovedCourse))).andExpect(status().isNotFound());
        mockMvc.perform(get(url(UUID.randomUUID()))).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("A public course with no joinable class answers an empty list and a null price_from")
    void courseWithoutClassesAnswersEmpty() throws Exception {
        jdbc.update("UPDATE courses SET admin_approved = true WHERE uuid = ?", unapprovedCourse);
        mockMvc.perform(get(url(unapprovedCourse)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.open_class_count").value(0))
                .andExpect(jsonPath("$.data.price_from").doesNotExist())
                .andExpect(jsonPath("$.data.classes.length()").value(0));
    }

    @Test
    @DisplayName("Anonymous catalogue cards carry price_from and open_class_count from the classes")
    void catalogueCardsCarryClassPricing() throws Exception {
        mockMvc.perform(get("/api/v1/catalogue/search").param("show", "courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[?(@.uuid == '" + publicCourse + "')].price_from").value(contains(2000.00)))
                .andExpect(jsonPath("$.data.content[?(@.uuid == '" + publicCourse + "')].open_class_count").value(contains(2)));
    }

    // ===== TEST PLUMBING =====

    private static String url(UUID courseUuid) {
        return "/api/v1/courses/" + courseUuid + "/open-classes";
    }

    private UUID user(String keycloakId, String email) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO users (uuid, user_no, first_name, last_name, email, keycloak_id, created_by) "
                        + "VALUES (?, ?, 'Test', 'User', ?, ?, 'test')",
                uuid, String.format("%09d", Math.abs(uuid.hashCode()) % 1000000000), email, keycloakId);
        return uuid;
    }

    private UUID courseCreator(UUID userUuid) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO course_creators (uuid, user_uuid, full_name, created_by) "
                + "VALUES (?, ?, 'Course Creator', 'test')", uuid, userUuid);
        return uuid;
    }

    private UUID instructor(UUID userUuid) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO instructors (uuid, user_uuid, full_name, admin_verified, created_by) "
                + "VALUES (?, ?, 'Private Trainer Name', true, 'test')", uuid, userUuid);
        return uuid;
    }

    private UUID course(String name, UUID creatorUuid, String status, boolean approved) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO courses (uuid, name, course_creator_uuid, status, active, admin_approved, price, "
                        + "creator_share_percentage, instructor_share_percentage, revenue_share_notes, created_by) "
                        + "VALUES (?, ?, ?, ?, ?, ?, 0, 40, 60, 'secret split', 'test')",
                uuid, name, creatorUuid, status, "published".equals(status), approved);
        return uuid;
    }

    /**
     * A class. Day offsets are relative to today (UTC): registration opens 30 days ago and closes
     * {@code registrationEndsIn} days from now; teaching runs {@code startsIn} to {@code endsIn}.
     */
    private UUID classDefinition(UUID courseUuid, UUID instructorUuid, String title, String locationType,
                                 String visibility, boolean active, String fee, int seats, String locationName,
                                 int registrationEndsIn, int startsIn, int endsIn) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO class_definitions (uuid, title, default_instructor_uuid, course_uuid, "
                        + "default_start_time, default_end_time, location_type, location_name, location_latitude, "
                        + "location_longitude, meeting_link, class_visibility, session_format, max_participants, "
                        + "is_active, sale_price, instructor_pay, registration_period_start_date, "
                        + "registration_period_end_date, academic_period_start_date, academic_period_end_date, created_by) "
                        + "VALUES (?, ?, ?, ?, TIMESTAMPTZ '2026-01-05 09:00:00+00', TIMESTAMPTZ '2026-01-05 10:30:00+00', "
                        + "?, ?, " + LATITUDE + ", " + LONGITUDE + ", 'https://meet.example/secret', ?, 'GROUP', ?, ?, "
                        + "CAST(? AS NUMERIC), 1.00, "
                        + "(now() AT TIME ZONE 'UTC')::date - 30, (now() AT TIME ZONE 'UTC')::date + ?, "
                        + "(now() AT TIME ZONE 'UTC')::date + ?, (now() AT TIME ZONE 'UTC')::date + ?, 'test')",
                uuid, title, instructorUuid, courseUuid, locationType, locationName, visibility, seats, active, fee,
                registrationEndsIn, startsIn, endsIn);
        return uuid;
    }

    private UUID session(UUID classDefinitionUuid, UUID instructorUuid) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO scheduled_instances (uuid, class_definition_uuid, instructor_uuid, "
                        + "start_time, end_time, title, location_type, max_participants, status, created_by) "
                        + "VALUES (?, ?, ?, NOW(), NOW() + INTERVAL '1 hour', 'Session', 'IN_PERSON', 10, "
                        + "'SCHEDULED', 'test')",
                uuid, classDefinitionUuid, instructorUuid);
        return uuid;
    }

    private UUID learner(String suffix) {
        UUID userUuid = user("open-classes-learner-" + suffix, suffix + "@learners.test.local");
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO students (uuid, user_uuid, full_name, created_by) VALUES (?, ?, ?, 'test')",
                uuid, userUuid, "Learner " + suffix);
        return uuid;
    }

    private void enrol(UUID scheduledInstanceUuid, UUID studentUuid, String status) {
        jdbc.update("INSERT INTO class_enrollments (uuid, scheduled_instance_uuid, student_uuid, status, created_by) "
                + "VALUES (?, ?, ?, ?, 'test')", UUID.randomUUID(), scheduledInstanceUuid, studentUuid, status);
    }
}
