package apps.sarafrika.elimika.course.integration;

import apps.sarafrika.elimika.testsupport.SharedPostgres;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Who may grade whom, over real HTTP. One piano course is taught three ways, as at St. Scholastica:
 * two school trainers each with their own class, and an independent instructor with private students.
 * Tests in "Red" encode the workbook rule that an instructor marks only their own students.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Instructor grading access (end-to-end)")
class InstructorGradingAccessIntegrationTest {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        SharedPostgres.register(registry);
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
    }

    private static final String SCHOOL_PIANO = "kc-grading-school-piano";
    private static final String SCHOOL_DRUMS = "kc-grading-school-drums";
    private static final String INDEPENDENT = "kc-grading-independent";
    private static final String UNRELATED_STUDENT = "kc-grading-unrelated-student";
    private static final String SUBSTITUTE = "kc-grading-substitute";
    private static final String SCHOOL_ADMIN = "kc-grading-school-admin";
    private static final String CREATOR = "kc-grading-creator";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @MockitoBean private JwtDecoder jwtDecoder;

    private UUID courseUuid;
    private UUID assessmentUuid;
    private UUID lineItemUuid;
    private UUID schoolDrumsInstructorUuid;
    private UUID schoolDrumsUserUuid;
    private UUID schoolPianoInstructorUuid;
    private UUID schoolPianoSessionUuid;
    private final List<UUID> criteria = new ArrayList<>();
    private UUID topLevelUuid;

    /** Enrolment of a student in the school piano trainer's own class. */
    private UUID ownStudentEnrollment;
    /** Enrolment of a student in the school drums trainer's class, same course. */
    private UUID colleagueStudentEnrollment;
    /** Enrolment of a private student of the independent instructor, same course. */
    private UUID privateStudentEnrollment;

    @BeforeEach
    void seed() {
        clean();

        UUID creatorUser = user(CREATOR, "creator@grading.test");
        grantDomain(creatorUser, "course_creator");
        UUID creatorUuid = courseCreator(creatorUser);
        courseUuid = course("Introduction to Piano", creatorUuid);
        UUID lessonUuid = lesson(courseUuid);
        UUID rubricUuid = sixCriterionRubric(creatorUuid);
        assessmentUuid = perLessonAssessment(courseUuid, rubricUuid);
        lineItemUuid = lineItem(assessmentUuid, lessonUuid);

        // The school is approved to train the course; both trainers teach there.
        UUID school = organisation("St. Scholastica");
        courseApproval(courseUuid, "organisation", school);

        UUID pianoUser = user(SCHOOL_PIANO, "piano@grading.test");
        grantDomain(pianoUser, "instructor");
        schoolPianoInstructorUuid = instructor(pianoUser, "School Piano Trainer");
        membership(pianoUser, school, "instructor");

        UUID drumsUser = user(SCHOOL_DRUMS, "drums@grading.test");
        grantDomain(drumsUser, "instructor");
        schoolDrumsInstructorUuid = instructor(drumsUser, "School Drums Trainer");
        schoolDrumsUserUuid = drumsUser;
        membership(drumsUser, school, "instructor");

        // The independent instructor holds a personal approval for the same course.
        UUID independentUser = user(INDEPENDENT, "independent@grading.test");
        grantDomain(independentUser, "instructor");
        UUID independentInstructorUuid = instructor(independentUser, "Independent Trainer");
        courseApproval(courseUuid, "instructor", independentInstructorUuid);

        UUID pianoClass = classDefinition(courseUuid, schoolPianoInstructorUuid, school, "Piano 2B");
        UUID drumsClass = classDefinition(courseUuid, schoolDrumsInstructorUuid, school, "Drums 2B");
        UUID privateClass = classDefinition(courseUuid, independentInstructorUuid, null, "Private piano");
        schoolPianoSessionUuid = session(pianoClass, schoolPianoInstructorUuid);

        ownStudentEnrollment = enrolInClass(schoolPianoSessionUuid, "own-student");
        colleagueStudentEnrollment = enrolInClass(session(drumsClass, schoolDrumsInstructorUuid), "colleague-student");
        privateStudentEnrollment = enrolInClass(session(privateClass, independentInstructorUuid), "private-student");

        // A substitute is scheduled on one of the piano class's sessions; the school admin runs the school.
        UUID substituteUser = user(SUBSTITUTE, "substitute@grading.test");
        grantDomain(substituteUser, "instructor");
        membership(substituteUser, school, "instructor");
        session(pianoClass, instructor(substituteUser, "Substitute Trainer"));

        UUID adminUser = user(SCHOOL_ADMIN, "admin@grading.test");
        grantDomain(adminUser, "organisation_user");
        membership(adminUser, school, "organisation_user");

        UUID unrelated = user(UNRELATED_STUDENT, "unrelated@grading.test");
        grantDomain(unrelated, "student");
    }

    @Nested
    @DisplayName("Access that already behaves correctly")
    class Baseline {

        @Test
        @DisplayName("a school trainer can open the gradebook of a student in their own class")
        void trainerReadsOwnStudent() throws Exception {
            mockMvc.perform(get(gradebookUrl(ownStudentEnrollment)).with(jwt(SCHOOL_PIANO)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("a school trainer can grade a lesson for a student in their own class")
        void trainerGradesOwnStudent() throws Exception {
            mockMvc.perform(put(rubricUrl(ownStudentEnrollment)).with(jwt(SCHOOL_PIANO))
                            .contentType(MediaType.APPLICATION_JSON).content(allTopMarks(null)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("a student with no relationship to the course cannot grade anyone")
        void unrelatedStudentIsRefused() throws Exception {
            mockMvc.perform(put(rubricUrl(ownStudentEnrollment)).with(jwt(UNRELATED_STUDENT))
                            .contentType(MediaType.APPLICATION_JSON).content(allTopMarks(null)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("People around the class may read marks but not award them")
    class ReadButNotGrade {

        @Test
        @DisplayName("a substitute on the class can read the student's gradebook")
        void substituteReads() throws Exception {
            mockMvc.perform(get(gradebookUrl(ownStudentEnrollment)).with(jwt(SUBSTITUTE))).andExpect(status().isOk());
        }

        @Test
        @DisplayName("a substitute on the class cannot grade the student")
        void substituteCannotGrade() throws Exception {
            mockMvc.perform(put(rubricUrl(ownStudentEnrollment)).with(jwt(SUBSTITUTE))
                    .contentType(MediaType.APPLICATION_JSON).content(allTopMarks(null))).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("the school's admin can read the student's gradebook")
        void schoolAdminReads() throws Exception {
            mockMvc.perform(get(gradebookUrl(ownStudentEnrollment)).with(jwt(SCHOOL_ADMIN))).andExpect(status().isOk());
        }

        @Test
        @DisplayName("the school's admin cannot grade the student")
        void schoolAdminCannotGrade() throws Exception {
            mockMvc.perform(put(rubricUrl(ownStudentEnrollment)).with(jwt(SCHOOL_ADMIN))
                    .contentType(MediaType.APPLICATION_JSON).content(allTopMarks(null))).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("the course author can read but not grade")
        void creatorReadsButCannotGrade() throws Exception {
            mockMvc.perform(get(gradebookUrl(privateStudentEnrollment)).with(jwt(CREATOR))).andExpect(status().isOk());
            mockMvc.perform(put(rubricUrl(privateStudentEnrollment)).with(jwt(CREATOR))
                    .contentType(MediaType.APPLICATION_JSON).content(allTopMarks(null))).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("the class trainer can leave feedback; a substitute cannot")
        void onlyTheTrainerLeavesFeedback() throws Exception {
            String body = "{\"comments\":\"Amazing and a hardworking student\"}";
            mockMvc.perform(post(gradebookUrl(ownStudentEnrollment) + "/feedback").with(jwt(SCHOOL_PIANO))
                    .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());
            mockMvc.perform(post(gradebookUrl(ownStudentEnrollment) + "/feedback").with(jwt(SUBSTITUTE))
                    .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
            mockMvc.perform(get(gradebookUrl(ownStudentEnrollment)).with(jwt(SCHOOL_ADMIN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.instructor_comments").value("Amazing and a hardworking student"));
        }

        @Test
        @DisplayName("the school's admin can open the progress report; a colleague trainer cannot")
        void progressReportFollowsReadRules() throws Exception {
            mockMvc.perform(put(rubricUrl(ownStudentEnrollment)).with(jwt(SCHOOL_PIANO))
                    .contentType(MediaType.APPLICATION_JSON).content(allTopMarks(null))).andExpect(status().isOk());
            mockMvc.perform(get(gradebookUrl(ownStudentEnrollment) + "/progress-report").with(jwt(SCHOOL_ADMIN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.lessons[0].marks[0].score").value(30.0))
                    .andExpect(jsonPath("$.data.lessons[0].marks[0].criteria.length()").value(6));
            mockMvc.perform(get(gradebookUrl(ownStudentEnrollment) + "/progress-report").with(jwt(SCHOOL_DRUMS)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("the school's admin can list a student's course enrolments; a trainer cannot")
        void schoolAdminListsStudentEnrolments() throws Exception {
            UUID student = jdbc.queryForObject("SELECT student_uuid FROM course_enrollments WHERE uuid = ?", UUID.class,
                    ownStudentEnrollment);
            // Enrolling in a school class makes the learner a member of the school.
            UUID school = jdbc.queryForObject("SELECT organisation_uuid FROM class_definitions WHERE organisation_uuid IS NOT NULL LIMIT 1", UUID.class);
            membership(jdbc.queryForObject("SELECT user_uuid FROM students WHERE uuid = ?", UUID.class, student), school, "student");
            mockMvc.perform(get("/api/v1/courses/enrollments/students/" + student).with(jwt(SCHOOL_ADMIN)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1));
            mockMvc.perform(get("/api/v1/courses/enrollments/students/" + student).with(jwt(SCHOOL_DRUMS)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("the school's admin cannot read an independent instructor's private student")
        void schoolAdminCannotReadPrivateStudent() throws Exception {
            mockMvc.perform(get(gradebookUrl(privateStudentEnrollment)).with(jwt(SCHOOL_ADMIN)))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("Drafts stay with the instructor")
    class Drafts {

        @Test
        @DisplayName("the learner sees a draft lesson score as not graded yet")
        void learnerCannotSeeDraft() throws Exception {
            String draft = "{\"status\":\"pending\",\"comments\":\"Still assessing pitch\",\"criteria_selections\":[{"
                    + "\"criteria_uuid\":\"" + criteria.getFirst() + "\",\"scoring_level_uuid\":\"" + topLevelUuid + "\"}]}";
            mockMvc.perform(put(rubricUrl(ownStudentEnrollment)).with(jwt(SCHOOL_PIANO))
                    .contentType(MediaType.APPLICATION_JSON).content(draft)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("pending"))
                    .andExpect(jsonPath("$.data.criteria_selections.length()").value(1));

            mockMvc.perform(get(rubricUrl(ownStudentEnrollment)).with(jwt("kc-grading-own-student")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.criteria_selections.length()").value(0))
                    .andExpect(jsonPath("$.data.comments").doesNotExist());
        }
    }

    @Nested
    @DisplayName("Red: an instructor must only reach their own students")
    class Red {

        @Test
        @DisplayName("a school trainer cannot read a colleague's student's gradebook in the same school")
        void trainerCannotReadColleaguesStudent() throws Exception {
            mockMvc.perform(get(gradebookUrl(colleagueStudentEnrollment)).with(jwt(SCHOOL_PIANO)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("a school trainer cannot grade a colleague's student in the same school")
        void trainerCannotGradeColleaguesStudent() throws Exception {
            mockMvc.perform(put(rubricUrl(colleagueStudentEnrollment)).with(jwt(SCHOOL_PIANO))
                            .contentType(MediaType.APPLICATION_JSON).content(allTopMarks(null)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("a school trainer cannot grade an independent instructor's private student")
        void schoolTrainerCannotGradePrivateStudent() throws Exception {
            mockMvc.perform(put(rubricUrl(privateStudentEnrollment)).with(jwt(SCHOOL_PIANO))
                            .contentType(MediaType.APPLICATION_JSON).content(allTopMarks(null)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("an independent instructor cannot grade a school's student")
        void independentCannotGradeSchoolStudent() throws Exception {
            mockMvc.perform(put(rubricUrl(ownStudentEnrollment)).with(jwt(INDEPENDENT))
                            .contentType(MediaType.APPLICATION_JSON).content(allTopMarks(null)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("'graded by' is the signed-in trainer, not whatever the request body claims")
        void gradedByIsTheCaller() throws Exception {
            mockMvc.perform(put(rubricUrl(ownStudentEnrollment)).with(jwt(SCHOOL_PIANO))
                            .contentType(MediaType.APPLICATION_JSON).content(allTopMarks(schoolDrumsUserUuid)))
                    .andExpect(status().isOk());

            UUID gradedBy = jdbc.queryForObject("SELECT graded_by_uuid FROM course_assessment_line_item_rubric_evaluations "
                    + "WHERE enrollment_uuid = ?", UUID.class, ownStudentEnrollment);
            assertThat(gradedBy).isNotEqualTo(schoolDrumsUserUuid);
        }

        @Test
        @DisplayName("a trainer cannot mark a colleague's session as completed")
        void trainerCannotCompleteColleaguesSession() throws Exception {
            mockMvc.perform(patch("/api/v1/timetable/schedule/" + schoolPianoSessionUuid + "/status")
                            .param("status", "COMPLETED").with(jwt(SCHOOL_DRUMS)))
                    .andExpect(status().isForbidden());
        }
    }

    // ===== TEST PLUMBING =====

    private String gradebookUrl(UUID enrollment) {
        return "/api/v1/courses/" + courseUuid + "/gradebook/enrollments/" + enrollment;
    }

    private String rubricUrl(UUID enrollment) {
        return "/api/v1/courses/" + courseUuid + "/assessments/" + assessmentUuid + "/line-items/" + lineItemUuid
                + "/rubric-evaluations/" + enrollment;
    }

    private String allTopMarks(UUID gradedBy) {
        StringBuilder rows = new StringBuilder();
        for (UUID c : criteria) {
            if (!rows.isEmpty()) {
                rows.append(',');
            }
            rows.append("{\"criteria_uuid\":\"").append(c).append("\",\"scoring_level_uuid\":\"")
                    .append(topLevelUuid).append("\"}");
        }
        String by = gradedBy == null ? "" : ",\"graded_by_uuid\":\"" + gradedBy + "\"";
        return "{\"comments\":\"Good tone\"" + by + ",\"criteria_selections\":[" + rows + "]}";
    }

    private RequestPostProcessor jwt(String subject) {
        return org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                .jwt().jwt(builder -> builder.subject(subject).claim("sub", subject));
    }

    private void clean() {
        jdbc.execute("TRUNCATE course_enrollment_feedback, course_assessment_grade_revisions, "
                + "course_assessment_line_item_rubric_evaluation_rows, "
                + "course_assessment_line_item_rubric_evaluations, course_assessment_line_item_scores, "
                + "course_assessment_scores, course_assessment_line_items, course_assessments, rubric_scoring_levels, "
                + "rubric_criteria, assessment_rubrics, course_enrollments, class_enrollments, scheduled_instances, "
                + "course_training_applications, class_definitions, lessons, courses, course_creators, instructors, "
                + "students, user_organisation_domain_mapping, organisation, user_domain_mapping, users "
                + "RESTART IDENTITY CASCADE");
        criteria.clear();
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

    private UUID organisation(String name) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO organisation (uuid, name, created_by) VALUES (?, ?, 'test')", uuid, name);
        return uuid;
    }

    private void membership(UUID userUuid, UUID organisation, String domainName) {
        jdbc.update("INSERT INTO user_organisation_domain_mapping "
                        + "(uuid, user_uuid, organisation_uuid, domain_uuid, active, created_by) "
                        + "VALUES (?, ?, ?, (SELECT uuid FROM user_domain WHERE domain_name = ?), true, 'test')",
                UUID.randomUUID(), userUuid, organisation, domainName);
    }

    private UUID courseCreator(UUID userUuid) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO course_creators (uuid, user_uuid, full_name, created_by) "
                + "VALUES (?, ?, 'Course Creator', 'test')", uuid, userUuid);
        return uuid;
    }

    private UUID instructor(UUID userUuid, String fullName) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO instructors (uuid, user_uuid, full_name, admin_verified, created_by) "
                + "VALUES (?, ?, ?, true, 'test')", uuid, userUuid, fullName);
        return uuid;
    }

    private UUID course(String name, UUID courseCreatorUuid) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO courses (uuid, name, course_creator_uuid, status, active, created_by) "
                + "VALUES (?, ?, ?, 'published', true, 'test')", uuid, name, courseCreatorUuid);
        return uuid;
    }

    private UUID lesson(UUID course) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO lessons (uuid, course_uuid, lesson_number, title, status, active, created_by) "
                + "VALUES (?, ?, 1, 'Class 1', 'published', true, 'test')", uuid, course);
        return uuid;
    }

    private UUID sixCriterionRubric(UUID creator) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO assessment_rubrics (uuid, title, rubric_type, course_creator_uuid, is_public, "
                + "status, is_active, uses_custom_levels, created_by) "
                + "VALUES (?, 'Instrument performance', 'performance', ?, false, 'published', true, true, 'test')",
                uuid, creator);
        String[] names = {"Technique", "Tonal Quality", "Rhythm", "Pitch", "Synchronisation", "Presentation/Expression"};
        for (int i = 0; i < names.length; i++) {
            UUID c = UUID.randomUUID();
            jdbc.update("INSERT INTO rubric_criteria (uuid, rubric_uuid, component_name, display_order, created_by) "
                    + "VALUES (?, ?, ?, ?, 'test')", c, uuid, names[i], i + 1);
            criteria.add(c);
        }
        int[] points = {5, 4, 3, 2, 0};
        for (int i = 0; i < points.length; i++) {
            UUID l = UUID.randomUUID();
            jdbc.update("INSERT INTO rubric_scoring_levels (uuid, rubric_uuid, name, points, level_order, is_passing, "
                    + "created_by) VALUES (?, ?, ?, ?, ?, ?, 'test')", l, uuid, "Level " + points[i], points[i], i + 1,
                    points[i] >= 3);
            if (i == 0) {
                topLevelUuid = l;
            }
        }
        return uuid;
    }

    private UUID perLessonAssessment(UUID course, UUID rubric) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO course_assessments (uuid, course_uuid, assessment_type, title, weight_percentage, "
                + "aggregation_strategy, rubric_uuid, is_required, active, per_lesson, sync_class_attendance, created_by) "
                + "VALUES (?, ?, 'performance', 'Lesson performance', 100, 'points_sum', ?, true, true, true, false, "
                + "'test')", uuid, course, rubric);
        return uuid;
    }

    private UUID lineItem(UUID assessment, UUID lesson) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO course_assessment_line_items (uuid, course_assessment_uuid, title, lesson_uuid, "
                + "item_type, max_score, display_order, active, created_by) "
                + "VALUES (?, ?, 'Class 1', ?, 'performance', 30, 1, true, 'test')", uuid, assessment, lesson);
        return uuid;
    }

    private void courseApproval(UUID course, String applicantType, UUID applicant) {
        jdbc.update("INSERT INTO course_training_applications (uuid, course_uuid, applicant_type, applicant_uuid, "
                        + "status, rate_currency, private_online_hourly_rate, private_inperson_hourly_rate, "
                        + "group_online_hourly_rate, group_inperson_hourly_rate, created_by) "
                        + "VALUES (?, ?, ?, ?, 'approved', 'KES', 1000, 1000, 1000, 1000, 'test')",
                UUID.randomUUID(), course, applicantType, applicant);
    }

    private UUID classDefinition(UUID course, UUID instructorUuid, UUID organisation, String title) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO class_definitions (uuid, title, default_instructor_uuid, course_uuid, organisation_uuid, "
                        + "default_start_time, default_end_time, location_type, class_visibility, session_format, "
                        + "max_participants, is_active, registration_period_start_date, "
                        + "registration_period_end_date, created_by) "
                        + "VALUES (?, ?, ?, ?, ?, TIMESTAMPTZ '2026-05-05 09:00:00+00', "
                        + "TIMESTAMPTZ '2026-05-05 09:45:00+00', 'IN_PERSON', 'PUBLIC', 'GROUP', 20, true, "
                        + "(now() AT TIME ZONE 'UTC')::date - 30, (now() AT TIME ZONE 'UTC')::date + 365, 'test')",
                uuid, title, instructorUuid, course, organisation);
        return uuid;
    }

    private UUID session(UUID classDefinitionUuid, UUID instructorUuid) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO scheduled_instances (uuid, class_definition_uuid, instructor_uuid, "
                        + "start_time, end_time, title, location_type, max_participants, status, created_by) "
                        + "VALUES (?, ?, ?, NOW(), NOW() + INTERVAL '45 minutes', 'Session', 'IN_PERSON', 20, "
                        + "'SCHEDULED', 'test')",
                uuid, classDefinitionUuid, instructorUuid);
        return uuid;
    }

    /** Places a new learner in a class session and gives them the course enrolment that holds grades. */
    private UUID enrolInClass(UUID sessionUuid, String key) {
        UUID userUuid = user("kc-grading-" + key, key + "@grading.test");
        grantDomain(userUuid, "student");
        UUID student = UUID.randomUUID();
        jdbc.update("INSERT INTO students (uuid, user_uuid, full_name, created_by) VALUES (?, ?, ?, 'test')",
                student, userUuid, key);
        jdbc.update("INSERT INTO class_enrollments (uuid, scheduled_instance_uuid, student_uuid, status, created_by) "
                + "VALUES (?, ?, ?, 'ENROLLED', 'test')", UUID.randomUUID(), sessionUuid, student);
        UUID enrollment = UUID.randomUUID();
        jdbc.update("INSERT INTO course_enrollments (uuid, student_uuid, course_uuid, status, created_by) "
                + "VALUES (?, ?, ?, 'active', 'test')", enrollment, student, courseUuid);
        return enrollment;
    }
}
