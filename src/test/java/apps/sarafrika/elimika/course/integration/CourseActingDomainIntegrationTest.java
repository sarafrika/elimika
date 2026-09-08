package apps.sarafrika.elimika.course.integration;

import apps.sarafrika.elimika.shared.security.ActingDomainResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Proves that one account reads the same course differently depending on the dashboard it is
 * reading from, over real HTTP, against a real database, through the real security filter chain.
 * <p>
 * The subject of nearly every test here is a single user who is <em>both</em> a platform
 * administrator <em>and</em> a learner with no enrolment on the course — the product owner's own
 * account, and the shape the platform used to get wrong. Asked "what is the highest footing this
 * person holds", the server answered {@code admin} on every page, and their learner dashboard
 * therefore carried the entire syllabus of a course they had never enrolled in, the owner
 * statistics block with gross sales and the platform fee, every trainer's rate card, the named
 * roster of everybody enrolled, and the applications those trainers had negotiated.
 * <p>
 * Three things are worth stating about how these tests are written.
 * <p>
 * <strong>The bytes are the assertion.</strong> Where content is withheld the response string is
 * searched for the secret, not just the DTO field for a null. A payload that reached the browser has
 * already left the building whatever the client does with it.
 * <p>
 * <strong>Each negative has its control.</strong> Every "capped" case is paired with the same
 * request from the dashboard entitled to the data, because a fix that returned nothing to anybody
 * would satisfy the negatives perfectly and break the product.
 * <p>
 * <strong>The header is not a credential.</strong> {@link #theHeaderCannotWidenAFooting()} and
 * {@link #claimingAdminWithoutBeingOneChangesNothing()} exist to prove the direction of travel: a
 * caller who asks to act as something they are not is told no by the same check as always, so the
 * worst a forged header achieves is less access than its sender already had.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("Acting domain caps the course record (end-to-end)")
class CourseActingDomainIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
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
        // Flyway builds the real schema. Hibernate's validator additionally sees @Entity classes
        // declared inside other test classes, which have no table, so leave validation to Flyway.
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    private static final String CREATOR_SUBJECT = "keycloak-acting-creator";
    /** Platform admin and a learner, with no enrolment on this course. The account in the report. */
    private static final String ADMIN_LEARNER_SUBJECT = "keycloak-acting-admin-learner";
    /** Platform admin and a learner who <em>is</em> enrolled. */
    private static final String ENROLLED_ADMIN_SUBJECT = "keycloak-acting-enrolled-admin";
    /** Platform admin and an instructor approved to train this course. */
    private static final String ADMIN_INSTRUCTOR_SUBJECT = "keycloak-acting-admin-instructor";
    /** Platform admin and staff of an organisation approved to train this course. */
    private static final String ADMIN_ORG_SUBJECT = "keycloak-acting-admin-org";
    /** Nothing but a learner, and not enrolled. Holds no elevated footing to be narrowed to. */
    private static final String PLAIN_LEARNER_SUBJECT = "keycloak-acting-plain-learner";

    /** Nothing but these strings identifies the withheld material, so they are the leak detectors. */
    private static final String SECRET_CONTENT_TITLE = "Module one: the whole syllabus";
    private static final String SECRET_CONTENT_BODY = "Everything an approved trainer may read.";
    private static final String DRAFT_LESSON_TITLE = "Lesson two, still being written";
    private static final String CLASS_TITLE = "Evening cohort";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;

    /** Never invoked: the jwt() post-processor sets the SecurityContext directly. Mocked only so
     *  the resource server does not fetch Keycloak's JWKS at boot. */
    @MockBean private JwtDecoder jwtDecoder;

    private UUID courseUuid;
    private UUID lessonUuid;

    @BeforeEach
    void seed() {
        clean();

        UUID creatorUserUuid = user(CREATOR_SUBJECT, "acting-creator@test.local");
        grantDomain(creatorUserUuid, "course_creator");
        courseUuid = course("Course a multi-role account can reach three ways", courseCreator(creatorUserUuid));
        lessonUuid = lesson(courseUuid, 1, "Lesson one", "published", true);
        lessonContent(lessonUuid, SECRET_CONTENT_TITLE, SECRET_CONTENT_BODY);
        lesson(courseUuid, 2, DRAFT_LESSON_TITLE, "draft", false);

        // The account the bug was reported from: administrator, learner, no enrolment here.
        UUID adminLearnerUuid = user(ADMIN_LEARNER_SUBJECT, "acting-admin-learner@test.local");
        grantDomain(adminLearnerUuid, "admin");
        grantDomain(adminLearnerUuid, "student");
        student(adminLearnerUuid, "Admin Who Also Learns");

        // The same account shape, but genuinely enrolled — the control for the enrolment check.
        UUID enrolledAdminUuid = user(ENROLLED_ADMIN_SUBJECT, "acting-enrolled-admin@test.local");
        grantDomain(enrolledAdminUuid, "admin");
        grantDomain(enrolledAdminUuid, "student");
        enrol(student(enrolledAdminUuid, "Admin Who Enrolled"), courseUuid);

        UUID adminInstructorUuid = user(ADMIN_INSTRUCTOR_SUBJECT, "acting-admin-instructor@test.local");
        grantDomain(adminInstructorUuid, "admin");
        grantDomain(adminInstructorUuid, "instructor");
        UUID approvedInstructorUuid = instructor(adminInstructorUuid, "Admin Who Also Teaches");
        application("instructor", approvedInstructorUuid, "approved");

        // A class the course runs, carrying both a sale price and what the trainer is paid for it.
        classDefinition(CLASS_TITLE, approvedInstructorUuid);

        UUID adminOrgUserUuid = user(ADMIN_ORG_SUBJECT, "acting-admin-org@test.local");
        grantDomain(adminOrgUserUuid, "admin");
        UUID organisationUuid = organisation("Westlands Technical");
        grantOrganisationDomain(adminOrgUserUuid, organisationUuid, "organisation_user");
        application("organisation", organisationUuid, "approved");

        UUID plainLearnerUuid = user(PLAIN_LEARNER_SUBJECT, "acting-plain-learner@test.local");
        grantDomain(plainLearnerUuid, "student");
        student(plainLearnerUuid, "Ordinary Learner");
    }

    // ===== DEFECT ONE: THE SYLLABUS =====

    @Nested
    @DisplayName("Course content")
    class Content {

        @Test
        @DisplayName("Acting as an admin, the same account still reads the whole course")
        void actingAsAdminReadsEverything() throws Exception {
            String body = mockMvc.perform(content(ADMIN_LEARNER_SUBJECT, "admin"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.access").value("admin"))
                    .andExpect(jsonPath("$.data.full_access").value(true))
                    .andExpect(jsonPath("$.data.lessons[0].uuid").value(lessonUuid.toString()))
                    .andReturn().getResponse().getContentAsString();

            assertThat(body).contains(SECRET_CONTENT_TITLE).contains(SECRET_CONTENT_BODY);
        }

        @Test
        @DisplayName("Acting as a student without an enrolment, the same account is a prospect")
        void actingAsStudentWithoutEnrolmentIsAProspect() throws Exception {
            String body = mockMvc.perform(content(ADMIN_LEARNER_SUBJECT, "student"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.access").value("prospect"))
                    .andExpect(jsonPath("$.data.full_access").value(false))
                    // Neither the content items nor the key that would fetch them separately.
                    .andExpect(jsonPath("$.data.lessons[0].contents").doesNotExist())
                    .andExpect(jsonPath("$.data.lessons[0].uuid").doesNotExist())
                    .andReturn().getResponse().getContentAsString();

            assertThat(body)
                    .doesNotContain(SECRET_CONTENT_TITLE)
                    .doesNotContain(SECRET_CONTENT_BODY)
                    .doesNotContain(lessonUuid.toString())
                    .doesNotContain(DRAFT_LESSON_TITLE);
        }

        @Test
        @DisplayName("Acting as a student with an enrolment reads the course, drafts excepted")
        void actingAsStudentWithEnrolmentReadsTheCourse() throws Exception {
            String body = mockMvc.perform(content(ENROLLED_ADMIN_SUBJECT, "student"))
                    .andExpect(status().isOk())
                    // Narrowed from admin to student, and the enrolment is what carries them there.
                    .andExpect(jsonPath("$.data.access").value("student"))
                    .andExpect(jsonPath("$.data.full_access").value(true))
                    .andExpect(jsonPath("$.data.lessons[0].uuid").value(lessonUuid.toString()))
                    // A learner reads the course as published; the author's unfinished work is theirs.
                    .andExpect(jsonPath("$.data.total_lessons").value(1))
                    .andReturn().getResponse().getContentAsString();

            assertThat(body).contains(SECRET_CONTENT_BODY).doesNotContain(DRAFT_LESSON_TITLE);
        }

        @Test
        @DisplayName("Acting as an instructor keeps the training approval and drops the admin footing")
        void actingAsInstructorKeepsTheInstructorFooting() throws Exception {
            mockMvc.perform(content(ADMIN_INSTRUCTOR_SUBJECT, "instructor"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.access").value("instructor"))
                    .andExpect(jsonPath("$.data.full_access").value(true));
        }

        @Test
        @DisplayName("Acting as an organisation keeps the school's approval and drops the admin footing")
        void actingAsOrganisationKeepsTheOrganisationFooting() throws Exception {
            mockMvc.perform(content(ADMIN_ORG_SUBJECT, "organisation"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.access").value("organisation"))
                    .andExpect(jsonPath("$.data.full_access").value(true));
        }

        @Test
        @DisplayName("An instructor's approval does not carry over to their learner dashboard")
        void anInstructorsApprovalIsNotReadFromTheLearnerDashboard() throws Exception {
            mockMvc.perform(content(ADMIN_INSTRUCTOR_SUBJECT, "student"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.access").value("prospect"))
                    .andExpect(jsonPath("$.data.full_access").value(false));
        }
    }

    // ===== DEFECT TWO: THE MONEY =====

    @Nested
    @DisplayName("Course statistics")
    class Stats {

        @Test
        @DisplayName("Acting as an admin, the owner block with gross sales still arrives")
        void actingAsAdminReadsTheOwnerBlock() throws Exception {
            mockMvc.perform(stats(ADMIN_LEARNER_SUBJECT, "admin"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.owner").exists())
                    .andExpect(jsonPath("$.data.owner.gross_sales").exists())
                    .andExpect(jsonPath("$.data.owner.platform_fee").exists());
        }

        @Test
        @DisplayName("Acting as a student, there is no owner key at all")
        void actingAsStudentNeverSeesTheOwnerBlock() throws Exception {
            String body = mockMvc.perform(stats(ADMIN_LEARNER_SUBJECT, "student"))
                    .andExpect(status().isOk())
                    // The public block is nobody's secret and still arrives, so this is not vacuous.
                    .andExpect(jsonPath("$.data.public").exists())
                    .andExpect(jsonPath("$.data.owner").doesNotExist())
                    .andExpect(jsonPath("$.data.scoped").doesNotExist())
                    .andReturn().getResponse().getContentAsString();

            // Not an owner block of zeros, and not one the client is trusted to drop.
            assertThat(body).doesNotContain("gross_sales").doesNotContain("platform_fee");
        }

        @Test
        @DisplayName("An administrator reading as a trainer gets their own figures, not the owner's")
        void actingAsInstructorGetsScopedFiguresAndNoCommercials() throws Exception {
            mockMvc.perform(stats(ADMIN_INSTRUCTOR_SUBJECT, "instructor"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.scoped").exists())
                    .andExpect(jsonPath("$.data.owner").doesNotExist());
        }

        @Test
        @DisplayName("A trainer's own figures do not follow them onto their learner dashboard")
        void actingAsStudentDropsTheScopedBlockToo() throws Exception {
            mockMvc.perform(stats(ADMIN_INSTRUCTOR_SUBJECT, "student"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.public").exists())
                    .andExpect(jsonPath("$.data.scoped").doesNotExist())
                    .andExpect(jsonPath("$.data.owner").doesNotExist());
        }
    }

    // ===== DEFECT THREE: WHAT THE TRAINERS CHARGE =====

    @Nested
    @DisplayName("Trainer directory")
    class Trainers {

        @Test
        @DisplayName("Acting as an admin, the rate cards and the pending count still arrive")
        void actingAsAdminReadsRateCards() throws Exception {
            mockMvc.perform(trainers(ADMIN_LEARNER_SUBJECT, "admin"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.trainers[0].rate_card").exists())
                    .andExpect(jsonPath("$.data.pending_count").exists());
        }

        @Test
        @DisplayName("Acting as a student, no rate card and no pending count cross the wire")
        void actingAsStudentNeverSeesRateCards() throws Exception {
            String body = mockMvc.perform(trainers(ADMIN_LEARNER_SUBJECT, "student"))
                    .andExpect(status().isOk())
                    // The directory itself is public information and still lists the trainers.
                    .andExpect(jsonPath("$.data.trainers.length()").value(2))
                    .andExpect(jsonPath("$.data.trainers[0].rate_card").doesNotExist())
                    .andExpect(jsonPath("$.data.pending_count").doesNotExist())
                    .andReturn().getResponse().getContentAsString();

            assertThat(body).doesNotContain("rate_card").doesNotContain("hourly_rate");
        }
    }

    // ===== THE ROSTER: WHO ELSE IS ON THIS COURSE =====

    @Nested
    @DisplayName("Course enrolments")
    class Enrolments {

        @Test
        @DisplayName("Acting as an admin, the named roster still arrives")
        void actingAsAdminReadsTheNamedRoster() throws Exception {
            mockMvc.perform(enrolments(ADMIN_LEARNER_SUBJECT, "admin"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].student_uuid").exists());
        }

        @Test
        @DisplayName("Acting as a student, the roster is the anonymous tally everyone else gets")
        void actingAsStudentReadsOnlyTheTally() throws Exception {
            // The rows still count towards the total — the course's popularity is public — but they
            // name nobody. Before the cap this handed over every classmate's uuid and progress.
            mockMvc.perform(enrolments(ADMIN_LEARNER_SUBJECT, "student"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].student_uuid").doesNotExist())
                    .andExpect(jsonPath("$.data.content[0].progress_percentage").doesNotExist());
        }
    }

    // ===== THE APPLICATIONS: WHAT EACH TRAINER NEGOTIATED =====

    @Nested
    @DisplayName("Training applications")
    class Applications {

        @Test
        @DisplayName("Acting as an admin, applications still come back whole")
        void actingAsAdminReadsWholeApplications() throws Exception {
            mockMvc.perform(applications(ADMIN_LEARNER_SUBJECT, "admin"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].rate_card").exists());
        }

        @Test
        @DisplayName("Acting as a student, the directory arrives stripped of every rate")
        void actingAsStudentReadsTheRedactedDirectory() throws Exception {
            String body = mockMvc.perform(applications(ADMIN_LEARNER_SUBJECT, "student"))
                    .andExpect(status().isOk())
                    // The approved list is the "who may deliver this" directory and stays visible.
                    .andExpect(jsonPath("$.data.content.length()").value(2))
                    .andExpect(jsonPath("$.data.content[0].rate_card").doesNotExist())
                    .andReturn().getResponse().getContentAsString();

            assertThat(body).doesNotContain("hourly_rate").doesNotContain("review_notes");
        }
    }

    // ===== THE CLASS LIST: WHAT THE TRAINER IS PAID =====

    @Nested
    @DisplayName("Course classes")
    class Classes {

        @Test
        @DisplayName("Acting as an admin, what the trainer is paid still arrives")
        void actingAsAdminReadsInstructorPay() throws Exception {
            String body = mockMvc.perform(classes(ADMIN_LEARNER_SUBJECT, "admin"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            assertThat(body).contains("instructor_pay");
        }

        @Test
        @DisplayName("Acting as a student, the instructor_pay key is nowhere in the payload")
        void actingAsStudentNeverSeesInstructorPay() throws Exception {
            String body = mockMvc.perform(classes(ADMIN_LEARNER_SUBJECT, "student"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            // The sale price is what a learner pays and stays; the trainer's fee is the
            // organisation's cost and its margin, and is not a learner's to read or to derive.
            assertThat(body)
                    .contains(CLASS_TITLE)
                    .as("the class list must carry no instructor_pay key at all")
                    .doesNotContain("instructor_pay");
        }
    }

    // ===== DEFECT FOUR: THE RAW LESSON ROUTE, ONE REQUEST AWAY =====

    @Nested
    @DisplayName("Raw lesson content")
    class RawLesson {

        @Test
        @DisplayName("Acting as an admin, the lesson body is served")
        void actingAsAdminReadsTheLessonBody() throws Exception {
            String body = mockMvc.perform(lessonContentRequest(ADMIN_LEARNER_SUBJECT, "admin"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            assertThat(body).contains(SECRET_CONTENT_BODY);
        }

        @Test
        @DisplayName("Acting as a student without an enrolment, the lesson route is refused")
        void actingAsStudentIsRefusedTheLessonBody() throws Exception {
            // Withholding the lesson uuid from the outline would be worthless if the uuid, once
            // known, still opened this route from the learner's dashboard.
            mockMvc.perform(lessonContentRequest(ADMIN_LEARNER_SUBJECT, "student"))
                    .andExpect(status().isForbidden());
        }
    }

    // ===== THE HEADER IS A REQUEST TO BE RESTRICTED, NEVER A GRANT =====

    @Test
    @DisplayName("A learner claiming to act as an admin is still a prospect")
    void theHeaderCannotWidenAFooting() throws Exception {
        String body = mockMvc.perform(content(PLAIN_LEARNER_SUBJECT, "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.access").value("prospect"))
                .andExpect(jsonPath("$.data.full_access").value(false))
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain(SECRET_CONTENT_BODY).doesNotContain(lessonUuid.toString());
    }

    @Test
    @DisplayName("Nor does claiming it unlock the commercial blocks")
    void claimingAdminWithoutBeingOneChangesNothing() throws Exception {
        mockMvc.perform(stats(PLAIN_LEARNER_SUBJECT, "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.owner").doesNotExist());
    }

    // ===== EVERY OTHER CLIENT KEEPS WORKING =====

    @Test
    @DisplayName("With no header at all, the platform answers exactly as it did before")
    void absentHeaderChangesNothing() throws Exception {
        mockMvc.perform(get(contentUrl()).with(jwt(ADMIN_LEARNER_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.access").value("admin"))
                .andExpect(jsonPath("$.data.full_access").value(true));

        mockMvc.perform(get(statsUrl()).with(jwt(ADMIN_LEARNER_SUBJECT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.owner").exists());
    }

    @Test
    @DisplayName("An unrecognised value is treated as no claim, never as a claim to be an admin")
    void unrecognisedHeaderChangesNothing() throws Exception {
        mockMvc.perform(content(ADMIN_LEARNER_SUBJECT, "superuser"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.access").value("admin"));
    }

    @Test
    @DisplayName("A blank value is treated as no claim either")
    void blankHeaderChangesNothing() throws Exception {
        mockMvc.perform(content(ADMIN_LEARNER_SUBJECT, "   "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.access").value("admin"));
    }

    @Test
    @DisplayName("The dashboard is read case-insensitively, and organisation_user is the same claim")
    void wireVocabularyToleratesTheClientsSpellings() throws Exception {
        mockMvc.perform(content(ADMIN_ORG_SUBJECT, "ORGANISATION_USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.access").value("organisation"));

        mockMvc.perform(content(ADMIN_LEARNER_SUBJECT, "Student"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.access").value("prospect"));
    }

    // ===== TEST PLUMBING =====

    private String contentUrl() {
        return "/api/v1/courses/" + courseUuid + "/content";
    }

    private String statsUrl() {
        return "/api/v1/courses/" + courseUuid + "/stats";
    }

    private String trainersUrl() {
        return "/api/v1/courses/" + courseUuid + "/trainers";
    }

    private MockHttpServletRequestBuilder content(String subject, String actingDomain) {
        return acting(get(contentUrl()), subject, actingDomain);
    }

    private MockHttpServletRequestBuilder stats(String subject, String actingDomain) {
        return acting(get(statsUrl()), subject, actingDomain);
    }

    private MockHttpServletRequestBuilder trainers(String subject, String actingDomain) {
        return acting(get(trainersUrl()), subject, actingDomain);
    }

    private MockHttpServletRequestBuilder classes(String subject, String actingDomain) {
        return acting(get("/api/v1/classes/course/" + courseUuid), subject, actingDomain);
    }

    private MockHttpServletRequestBuilder enrolments(String subject, String actingDomain) {
        return acting(get("/api/v1/courses/" + courseUuid + "/enrollments"), subject, actingDomain);
    }

    private MockHttpServletRequestBuilder applications(String subject, String actingDomain) {
        return acting(
                get("/api/v1/courses/" + courseUuid + "/training-applications"), subject, actingDomain);
    }

    private MockHttpServletRequestBuilder lessonContentRequest(String subject, String actingDomain) {
        return acting(
                get("/api/v1/courses/" + courseUuid + "/lessons/" + lessonUuid + "/content"),
                subject,
                actingDomain);
    }

    private MockHttpServletRequestBuilder acting(MockHttpServletRequestBuilder request,
                                                 String subject,
                                                 String actingDomain) {
        return request.with(jwt(subject)).header(ActingDomainResolver.HEADER, actingDomain);
    }

    private RequestPostProcessor jwt(String subject) {
        return org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                .jwt().jwt(builder -> builder.subject(subject).claim("sub", subject));
    }

    private void clean() {
        jdbc.execute("TRUNCATE class_definitions, course_enrollments, course_training_applications, "
                + "lesson_contents, lessons, course_reviews, courses, course_creators, instructors, "
                + "students, user_organisation_domain_mapping, user_domain_mapping, organisation, users "
                + "RESTART IDENTITY CASCADE");
    }

    private UUID user(String keycloakId, String email) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO users (uuid, user_no, first_name, last_name, email, keycloak_id, created_by) "
                        + "VALUES (?, ?, 'Test', 'User', ?, ?, 'test')",
                uuid, String.format("%09d", Math.abs(uuid.hashCode()) % 1000000000), email, keycloakId);
        return uuid;
    }

    /** A platform-level domain: the mapping {@code isPlatformAdmin()} reads for {@code admin}. */
    private void grantDomain(UUID userUuid, String domainName) {
        jdbc.update("INSERT INTO user_domain_mapping (user_uuid, domain_uuid) "
                + "VALUES (?, (SELECT uuid FROM user_domain WHERE domain_name = ?))", userUuid, domainName);
    }

    /** An organisation-scoped role, which is what puts a member on the school's teaching side. */
    private void grantOrganisationDomain(UUID userUuid, UUID organisationUuid, String domainName) {
        jdbc.update("INSERT INTO user_organisation_domain_mapping "
                        + "(uuid, user_uuid, organisation_uuid, domain_uuid, created_by) "
                        + "VALUES (?, ?, ?, (SELECT uuid FROM user_domain WHERE domain_name = ?), 'test')",
                UUID.randomUUID(), userUuid, organisationUuid, domainName);
    }

    private UUID organisation(String name) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO organisation (uuid, name, location, country, created_by) "
                + "VALUES (?, ?, 'Westlands', 'Kenya', 'test')", uuid, name);
        return uuid;
    }

    private UUID courseCreator(UUID userUuid) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO course_creators (uuid, user_uuid, full_name, created_by) "
                + "VALUES (?, ?, 'Course Creator', 'test')", uuid, userUuid);
        return uuid;
    }

    private UUID instructor(UUID userUuid, String fullName) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO instructors (uuid, user_uuid, full_name, created_by, updated_by) "
                + "VALUES (?, ?, ?, 'test', 'test')", uuid, userUuid, fullName);
        return uuid;
    }

    private UUID student(UUID userUuid, String fullName) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO students (uuid, user_uuid, full_name, created_by) VALUES (?, ?, ?, 'test')",
                uuid, userUuid, fullName);
        return uuid;
    }

    /** An access-allowing enrolment, written the way the converter writes one. */
    private void enrol(UUID studentUuid, UUID courseUuid) {
        jdbc.update("INSERT INTO course_enrollments (uuid, student_uuid, course_uuid, status, created_by) "
                + "VALUES (?, ?, ?, 'active', 'test')", UUID.randomUUID(), studentUuid, courseUuid);
    }

    private UUID course(String name, UUID courseCreatorUuid) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO courses (uuid, name, course_creator_uuid, status, active, created_by) "
                + "VALUES (?, ?, ?, 'published', true, 'test')", uuid, name, courseCreatorUuid);
        return uuid;
    }

    /**
     * A class on the course with both figures on it. The registration window is mandatory on the
     * table and says nothing about pay visibility, so it is opened wide around today.
     */
    private void classDefinition(String title, UUID instructorUuid) {
        jdbc.update("INSERT INTO class_definitions (uuid, title, default_instructor_uuid, course_uuid, "
                        + "sale_price, instructor_pay, rate_basis, default_start_time, default_end_time, "
                        + "class_visibility, session_format, is_active, registration_period_start_date, "
                        + "registration_period_end_date, created_by) "
                        + "VALUES (?, ?, ?, ?, 8000, 4500, 'PER_SESSION', "
                        + "'2026-04-01 09:00:00'::timestamp, '2026-04-01 10:30:00'::timestamp, "
                        + "'PUBLIC', 'GROUP', true, (now() AT TIME ZONE 'UTC')::date - 30, "
                        + "(now() AT TIME ZONE 'UTC')::date + 365, 'test')",
                UUID.randomUUID(), title, instructorUuid, courseUuid);
    }

    private UUID lesson(UUID courseUuid, int number, String title, String status, boolean active) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO lessons (uuid, course_uuid, lesson_number, title, status, active, created_by) "
                + "VALUES (?, ?, ?, ?, ?::varchar, ?, 'test')", uuid, courseUuid, number, title, status, active);
        return uuid;
    }

    private void lessonContent(UUID lessonUuid, String title, String text) {
        jdbc.update("INSERT INTO lesson_contents "
                        + "(uuid, lesson_uuid, content_type_uuid, title, content_text, display_order, created_by) "
                        + "VALUES (?, ?, (SELECT uuid FROM lesson_content_types WHERE name = 'Text'), ?, ?, 1, 'test')",
                UUID.randomUUID(), lessonUuid, title, text);
    }

    /**
     * The applicant type and status are written lower-case because that is what the entity's
     * {@code AttributeConverter} writes, and therefore what its queries bind.
     */
    private void application(String applicantType, UUID applicantUuid, String status) {
        jdbc.update("INSERT INTO course_training_applications "
                        + "(uuid, course_uuid, applicant_type, applicant_uuid, status, rate_currency, "
                        + " private_online_hourly_rate, private_inperson_hourly_rate, "
                        + " group_online_hourly_rate, group_inperson_hourly_rate, reviewed_at, created_by) "
                        + "VALUES (?, ?, ?, ?, ?, 'KES', 2500, 2500, 2500, 2500, now(), 'test')",
                UUID.randomUUID(), courseUuid, applicantType, applicantUuid, status);
    }
}
