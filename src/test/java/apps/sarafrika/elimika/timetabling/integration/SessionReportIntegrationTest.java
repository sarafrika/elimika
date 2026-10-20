package apps.sarafrika.elimika.timetabling.integration;

import apps.sarafrika.elimika.timetabling.internal.SessionReportReminderScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
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

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The mandatory session report end to end: capture, checklist, final submission (drafts become
 * scores), the school's grace period, the lock, what the school sees, and the reminder job.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("Session reports (end-to-end)")
class SessionReportIntegrationTest {

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
        registry.add("app.session-reports.reminder-cron", () -> "-");
    }

    private static final String TRAINER = "kc-report-trainer";
    private static final String COLLEAGUE = "kc-report-colleague";
    private static final String SCHOOL_ADMIN = "kc-report-school-admin";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private SessionReportReminderScheduler scheduler;
    @MockBean private JwtDecoder jwtDecoder;

    private UUID school;
    private UUID courseUuid;
    private UUID assessmentUuid;
    private UUID lineItemUuid;
    private UUID sessionUuid;
    private UUID presentSeat;
    private UUID absentSeat;
    private UUID presentEnrollment;
    private final List<UUID> criteria = new ArrayList<>();
    private UUID topLevel;

    @BeforeEach
    void seed() {
        jdbc.execute("TRUNCATE session_report_revisions, session_reports, organisation_settings, user_notifications, "
                + "course_assessment_grade_revisions, course_assessment_line_item_rubric_evaluation_rows, "
                + "course_assessment_line_item_rubric_evaluations, course_assessment_line_item_scores, course_assessment_scores, "
                + "course_assessment_line_items, course_assessments, rubric_scoring_levels, rubric_criteria, assessment_rubrics, "
                + "course_enrollments, class_enrollments, scheduled_instances, course_training_applications, class_definitions, "
                + "lessons, courses, course_creators, instructors, students, user_organisation_domain_mapping, organisation, "
                + "user_domain_mapping, users RESTART IDENTITY CASCADE");
        criteria.clear();

        UUID creatorUser = user("kc-report-creator");
        grantDomain(creatorUser, "course_creator");
        UUID creator = uuidInsert("INSERT INTO course_creators (uuid, user_uuid, full_name, created_by) VALUES (?, ?, 'Creator', 'test')", creatorUser);
        courseUuid = uuidInsert("INSERT INTO courses (uuid, name, course_creator_uuid, status, active, created_by) "
                + "VALUES (?, 'Introduction to Piano', ?, 'published', true, 'test')", creator);
        UUID lesson = uuidInsert("INSERT INTO lessons (uuid, course_uuid, lesson_number, title, status, active, created_by) "
                + "VALUES (?, ?, 1, 'Grand staff', 'published', true, 'test')", courseUuid);
        UUID rubric = uuidInsert("INSERT INTO assessment_rubrics (uuid, title, rubric_type, course_creator_uuid, is_public, status, "
                + "is_active, uses_custom_levels, created_by) VALUES (?, 'Performance', 'performance', ?, false, 'published', true, true, 'test')", creator);
        String[] names = {"Technique", "Tonal Quality", "Rhythm", "Pitch", "Synchronisation", "Presentation"};
        for (int i = 0; i < names.length; i++) {
            criteria.add(uuidInsert("INSERT INTO rubric_criteria (uuid, rubric_uuid, component_name, display_order, created_by) "
                    + "VALUES (?, ?, '" + names[i] + "', " + (i + 1) + ", 'test')", rubric));
        }
        int[] points = {5, 4, 3, 2, 0};
        for (int i = 0; i < points.length; i++) {
            UUID level = uuidInsert("INSERT INTO rubric_scoring_levels (uuid, rubric_uuid, name, points, level_order, is_passing, created_by) "
                    + "VALUES (?, ?, 'Level " + points[i] + "', " + points[i] + ", " + (i + 1) + ", " + (points[i] >= 3) + ", 'test')", rubric);
            if (i == 0) {
                topLevel = level;
            }
        }
        assessmentUuid = uuidInsert("INSERT INTO course_assessments (uuid, course_uuid, assessment_type, title, weight_percentage, "
                + "aggregation_strategy, rubric_uuid, is_required, active, per_lesson, sync_class_attendance, created_by) "
                + "VALUES (?, ?, 'performance', 'Lesson performance', 100, 'points_sum', ?, true, true, true, false, 'test')",
                courseUuid, rubric);
        lineItemUuid = uuidInsert("INSERT INTO course_assessment_line_items (uuid, course_assessment_uuid, title, lesson_uuid, item_type, "
                + "max_score, display_order, active, created_by) VALUES (?, ?, 'Class 1', ?, 'performance', 30, 1, true, 'test')",
                assessmentUuid, lesson);

        school = uuidInsert("INSERT INTO organisation (uuid, name, created_by) VALUES (?, 'St. Scholastica', 'test')");
        jdbc.update("INSERT INTO course_training_applications (uuid, course_uuid, applicant_type, applicant_uuid, status, rate_currency, "
                + "private_online_hourly_rate, private_inperson_hourly_rate, group_online_hourly_rate, group_inperson_hourly_rate, created_by) "
                + "VALUES (?, ?, 'organisation', ?, 'approved', 'KES', 1000, 1000, 1000, 1000, 'test')", UUID.randomUUID(), courseUuid, school);

        UUID trainerUser = user(TRAINER);
        grantDomain(trainerUser, "instructor");
        membership(trainerUser, "instructor");
        UUID trainer = uuidInsert("INSERT INTO instructors (uuid, user_uuid, full_name, admin_verified, created_by) VALUES (?, ?, 'Trainer', true, 'test')", trainerUser);
        UUID colleagueUser = user(COLLEAGUE);
        grantDomain(colleagueUser, "instructor");
        membership(colleagueUser, "instructor");
        uuidInsert("INSERT INTO instructors (uuid, user_uuid, full_name, admin_verified, created_by) VALUES (?, ?, 'Colleague', true, 'test')", colleagueUser);
        UUID adminUser = user(SCHOOL_ADMIN);
        grantDomain(adminUser, "organisation_user");
        membership(adminUser, "organisation_user");

        UUID classUuid = uuidInsert("INSERT INTO class_definitions (uuid, title, default_instructor_uuid, course_uuid, organisation_uuid, "
                + "default_start_time, default_end_time, location_type, class_visibility, session_format, max_participants, is_active, "
                + "registration_period_start_date, registration_period_end_date, created_by) VALUES (?, 'Piano 2B', ?, ?, ?, "
                + "TIMESTAMPTZ '2026-05-05 09:00:00+00', TIMESTAMPTZ '2026-05-05 09:45:00+00', 'IN_PERSON', 'PUBLIC', 'GROUP', 20, true, "
                + "(now() AT TIME ZONE 'UTC')::date - 30, (now() AT TIME ZONE 'UTC')::date + 365, 'test')", trainer, courseUuid, school);
        sessionUuid = uuidInsert("INSERT INTO scheduled_instances (uuid, class_definition_uuid, instructor_uuid, start_time, end_time, title, "
                + "location_type, max_participants, status, lesson_uuid, created_by) VALUES (?, ?, ?, NOW() - INTERVAL '2 hours', "
                + "NOW() - INTERVAL '75 minutes', 'Class 1', 'IN_PERSON', 20, 'SCHEDULED', ?, 'test')", classUuid, trainer, lesson);

        UUID[] present = learner("present");
        presentSeat = present[0];
        presentEnrollment = present[1];
        absentSeat = learner("absent")[0];
    }

    @Test
    @DisplayName("the checklist blocks submission until attendance, scores and the topic are done")
    void checklistBlocksAnIncompleteReport() throws Exception {
        mockMvc.perform(get(reportUrl()).with(jwt(TRAINER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("draft"))
                .andExpect(jsonPath("$.data.can_submit").value(false))
                .andExpect(jsonPath("$.data.checklist[?(@.code == 'ATTENDANCE_NOT_MARKED')]").exists())
                .andExpect(jsonPath("$.data.checklist[?(@.code == 'TOPIC_NOT_RECORDED')]").exists());

        mockMvc.perform(post(reportUrl() + "/submit").with(jwt(TRAINER))).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("final submission turns a complete draft into a score, freezes revision 1 and opens the school's grace window")
    void submitFinalisesDraftsAndOpensGraceWindow() throws Exception {
        captureSession();

        mockMvc.perform(get(reportUrl()).with(jwt(TRAINER)))
                .andExpect(jsonPath("$.data.can_submit").value(true))
                .andExpect(jsonPath("$.data.checklist[?(@.code == 'DRAFT_WILL_BE_SUBMITTED')]").exists())
                .andExpect(jsonPath("$.data.aggregates.present").value(1))
                .andExpect(jsonPath("$.data.aggregates.absent").value(1));

        mockMvc.perform(post(reportUrl() + "/submit").with(jwt(TRAINER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("submitted"))
                .andExpect(jsonPath("$.data.revision_count").value(1))
                .andExpect(jsonPath("$.data.rows[?(@.attendance == 'ATTENDED')].score_status").value("submitted"))
                .andExpect(jsonPath("$.data.aggregates.class_average").value(30.0));

        String evaluationStatus = jdbc.queryForObject("SELECT status FROM course_assessment_line_item_rubric_evaluations "
                + "WHERE enrollment_uuid = ?", String.class, presentEnrollment);
        assertThat(evaluationStatus).isEqualToIgnoringCase("completed");
        LocalDateTime editableUntil = jdbc.queryForObject("SELECT editable_until FROM session_reports", LocalDateTime.class);
        assertThat(editableUntil).isCloseTo(LocalDateTime.now(ZoneOffset.UTC).plusHours(48), within(5, ChronoUnit.MINUTES));
    }

    @Test
    @DisplayName("only the school's managers set the grace period, and submission uses it")
    void schoolGracePeriodApplies() throws Exception {
        mockMvc.perform(put("/api/v1/organisations/" + school + "/settings").with(jwt(TRAINER))
                .contentType(MediaType.APPLICATION_JSON).content("{\"session_report_grace_hours\":1}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/organisations/" + school + "/settings").with(jwt(SCHOOL_ADMIN))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"session_report_grace_hours\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.session_report_grace_hours").value(1));

        captureSession();
        mockMvc.perform(post(reportUrl() + "/submit").with(jwt(TRAINER))).andExpect(status().isOk());

        LocalDateTime editableUntil = jdbc.queryForObject("SELECT editable_until FROM session_reports", LocalDateTime.class);
        assertThat(editableUntil).isCloseTo(LocalDateTime.now(ZoneOffset.UTC).plusHours(1), within(5, ChronoUnit.MINUTES));
    }

    @Test
    @DisplayName("attendance sent as a body keeps the rich-text comment and shows on the report")
    void attendanceBodyKeepsRichTextComment() throws Exception {
        mockMvc.perform(put("/api/v1/enrollment/" + absentSeat + "/attendance").with(jwt(TRAINER))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"attended\":false,\"instrument_condition\":\"needs_repair\",\"comment\":\"<p><strong>Sick</strong> today</p>\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get(reportUrl()).with(jwt(TRAINER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rows[?(@.attendance == 'ABSENT')].attendance_comment").value("<p><strong>Sick</strong> today</p>"))
                .andExpect(jsonPath("$.data.rows[?(@.attendance == 'ABSENT')].instrument_condition").value("NEEDS_REPAIR"));
    }

    @Test
    @DisplayName("after the grace window, attendance, the session record and lesson scores are refused")
    void lockedReportRefusesChanges() throws Exception {
        captureSession();
        mockMvc.perform(post(reportUrl() + "/submit").with(jwt(TRAINER))).andExpect(status().isOk());
        jdbc.update("UPDATE session_reports SET editable_until = ?", Timestamp.valueOf(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1)));

        mockMvc.perform(patch("/api/v1/enrollment/" + absentSeat + "/attendance").param("attended", "true").with(jwt(TRAINER)))
                .andExpect(status().isConflict());
        mockMvc.perform(put("/api/v1/enrollment/" + absentSeat + "/attendance").with(jwt(TRAINER))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"attended\":true}"))
                .andExpect(status().isConflict());
        mockMvc.perform(put("/api/v1/timetable/schedule/" + sessionUuid + "/record").with(jwt(TRAINER))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"topic_covered\":\"Changed\"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(put(rubricUrl()).with(jwt(TRAINER)).contentType(MediaType.APPLICATION_JSON).content(marks("completed", 6)))
                .andExpect(status().isConflict());

        scheduler.run();
        String status = jdbc.queryForObject("SELECT status FROM session_reports", String.class);
        assertThat(status).isEqualTo("locked");
    }

    @Test
    @DisplayName("the school sees only the submitted report; a colleague trainer sees nothing")
    void schoolSeesSubmittedOnly() throws Exception {
        captureSession();
        mockMvc.perform(get(reportUrl()).with(jwt(SCHOOL_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rows.length()").value(0));
        mockMvc.perform(get(reportUrl()).with(jwt(COLLEAGUE))).andExpect(status().isForbidden());

        mockMvc.perform(post(reportUrl() + "/submit").with(jwt(TRAINER))).andExpect(status().isOk());
        mockMvc.perform(get(reportUrl()).with(jwt(SCHOOL_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rows.length()").value(2))
                .andExpect(jsonPath("$.data.status").value("submitted"));
        mockMvc.perform(get("/api/v1/enrollment/organisations/" + school + "/session-reports").param("status", "submitted")
                        .with(jwt(SCHOOL_ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1));
    }

    @Test
    @DisplayName("the reminder job claims each reminder and overdue notice once, however often it runs")
    void reminderJobClaimsOnce() {
        scheduler.run();
        scheduler.run();
        Integer reports = jdbc.queryForObject("SELECT count(*) FROM session_reports WHERE reminded_at IS NOT NULL", Integer.class);
        assertThat(reports).isEqualTo(1);

        jdbc.update("UPDATE session_reports SET due_at = ?", Timestamp.valueOf(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1)));
        scheduler.run();
        LocalDateTime first = jdbc.queryForObject("SELECT overdue_notified_at FROM session_reports", LocalDateTime.class);
        scheduler.run();
        LocalDateTime second = jdbc.queryForObject("SELECT overdue_notified_at FROM session_reports", LocalDateTime.class);
        assertThat(first).isNotNull().isEqualTo(second);
    }

    @Test
    @DisplayName("the notification tables accept the three session report types")
    void notificationTypesAreAllowed() {
        UUID recipient = user("kc-report-recipient");
        for (String type : List.of("SESSION_REPORT_DUE", "SESSION_REPORT_OVERDUE", "SESSION_REPORT_OVERDUE_ORG")) {
            jdbc.update("INSERT INTO user_notifications (recipient_uuid, notification_id, notification_type, category, priority, title, body) "
                    + "VALUES (?, ?, ?, 'COURSE_MANAGEMENT', 'NORMAL', 't', 'b')", recipient, UUID.randomUUID(), type);
        }
        Integer rows = jdbc.queryForObject("SELECT count(*) FROM user_notifications WHERE notification_type LIKE 'SESSION_REPORT%'", Integer.class);
        assertThat(rows).isEqualTo(3);
    }

    // ===== capture helpers =====

    /** Present learner scored in full as a draft, absent learner marked, topic recorded. */
    private void captureSession() throws Exception {
        mockMvc.perform(patch("/api/v1/enrollment/" + presentSeat + "/attendance").param("attended", "true")
                .param("instrument_condition", "good").with(jwt(TRAINER))).andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/enrollment/" + absentSeat + "/attendance").param("attended", "false").with(jwt(TRAINER)))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/v1/timetable/schedule/" + sessionUuid + "/record").with(jwt(TRAINER))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"topic_covered\":\"<p>Treble and <strong>bass</strong> clefs</p>\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(put(rubricUrl()).with(jwt(TRAINER)).contentType(MediaType.APPLICATION_JSON).content(marks("pending", 6)))
                .andExpect(status().isOk());
    }

    private String marks(String status, int count) {
        StringBuilder rows = new StringBuilder();
        for (int i = 0; i < count; i++) {
            rows.append(i == 0 ? "" : ",").append("{\"criteria_uuid\":\"").append(criteria.get(i))
                    .append("\",\"scoring_level_uuid\":\"").append(topLevel).append("\"}");
        }
        return "{\"status\":\"" + status + "\",\"comments\":\"<p>Good tone</p>\",\"criteria_selections\":[" + rows + "]}";
    }

    private String reportUrl() {
        return "/api/v1/timetable/schedule/" + sessionUuid + "/report";
    }

    private String rubricUrl() {
        return "/api/v1/courses/" + courseUuid + "/assessments/" + assessmentUuid + "/line-items/" + lineItemUuid
                + "/rubric-evaluations/" + presentEnrollment;
    }

    private RequestPostProcessor jwt(String subject) {
        return org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                .jwt().jwt(builder -> builder.subject(subject).claim("sub", subject));
    }

    /** A learner with a seat on the session and the course enrolment that holds grades: [seat, course enrolment]. */
    private UUID[] learner(String key) {
        UUID userUuid = user("kc-report-" + key);
        grantDomain(userUuid, "student");
        UUID student = uuidInsert("INSERT INTO students (uuid, user_uuid, full_name, created_by) VALUES (?, ?, '" + key + "', 'test')", userUuid);
        UUID seat = uuidInsert("INSERT INTO class_enrollments (uuid, scheduled_instance_uuid, student_uuid, status, created_by) "
                + "VALUES (?, ?, ?, 'ENROLLED', 'test')", sessionUuid, student);
        UUID enrollment = uuidInsert("INSERT INTO course_enrollments (uuid, student_uuid, course_uuid, status, created_by) "
                + "VALUES (?, ?, ?, 'active', 'test')", student, courseUuid);
        return new UUID[]{seat, enrollment};
    }

    private UUID user(String keycloakId) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO users (uuid, user_no, first_name, last_name, email, keycloak_id, created_by) "
                        + "VALUES (?, ?, 'Test', 'User', ?, ?, 'test')",
                uuid, String.format("%09d", Math.abs(uuid.hashCode()) % 1000000000), keycloakId + "@report.test", keycloakId);
        return uuid;
    }

    private void grantDomain(UUID userUuid, String domainName) {
        jdbc.update("INSERT INTO user_domain_mapping (user_uuid, domain_uuid) "
                + "VALUES (?, (SELECT uuid FROM user_domain WHERE domain_name = ?))", userUuid, domainName);
    }

    private void membership(UUID userUuid, String domainName) {
        jdbc.update("INSERT INTO user_organisation_domain_mapping (uuid, user_uuid, organisation_uuid, domain_uuid, active, created_by) "
                + "VALUES (?, ?, ?, (SELECT uuid FROM user_domain WHERE domain_name = ?), true, 'test')",
                UUID.randomUUID(), userUuid, school, domainName);
    }

    private UUID uuidInsert(String sql, Object... args) {
        UUID uuid = UUID.randomUUID();
        Object[] all = new Object[args.length + 1];
        all[0] = uuid;
        System.arraycopy(args, 0, all, 1, args.length);
        jdbc.update(sql, all);
        return uuid;
    }
}
