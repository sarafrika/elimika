package apps.sarafrika.elimika.course.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Who may set up programme assessment, and who may read programme reports and write remarks. */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@DisplayName("Programme assessment API access (end-to-end)")
class ProgrammeAssessmentApiIntegrationTest {

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
    }

    private static final String CREATOR = "kc-prog-creator";
    private static final String OTHER_CREATOR = "kc-prog-other-creator";
    private static final String TEACHER = "kc-prog-teacher";
    private static final String SUBSTITUTE = "kc-prog-substitute";
    private static final String OTHER_TEACHER = "kc-prog-other-teacher";
    private static final String STUDENT = "kc-prog-student";
    private static final String PARENT = "kc-prog-parent";
    private static final String OUTSIDER = "kc-prog-outsider";
    private static final String ADMIN = "kc-prog-admin";

    @Autowired private MockMvc mockMvc;
    @Autowired private JdbcTemplate jdbc;
    @MockitoBean private JwtDecoder jwtDecoder;

    private UUID programUuid;
    private UUID programCourseUuid;
    private UUID studentUuid;
    private UUID classUuid;
    private UUID otherClassUuid;
    private UUID skillUuid;

    @BeforeEach
    void seed() {
        jdbc.execute("TRUNCATE program_enrollment_remarks, learner_skill_levels, learner_skill_evidence, program_skill_mappings, "
                + "program_target_skills, program_enrollments, program_courses, training_programs, student_guardian_links, "
                + "course_enrollments, class_enrollments, scheduled_instances, class_definitions, courses, course_creators, "
                + "instructors, students, user_domain_mapping, users RESTART IDENTITY CASCADE");

        UUID creatorUser = user(CREATOR);
        grantDomain(creatorUser, "course_creator");
        UUID creator = courseCreator(creatorUser);
        grantDomain(user(OTHER_CREATOR), "course_creator");
        courseCreator(jdbc.queryForObject("SELECT uuid FROM users WHERE keycloak_id = ?", UUID.class, OTHER_CREATOR));

        UUID course = UUID.randomUUID();
        jdbc.update("INSERT INTO courses (uuid, name, course_creator_uuid, status, active, created_by) VALUES (?, 'Introduction to Piano', ?, 'published', true, 'test')",
                course, creator);
        programUuid = UUID.randomUUID();
        jdbc.update("INSERT INTO training_programs (uuid, title, course_creator_uuid, status, pass_mark, created_by) "
                + "VALUES (?, 'Foundation Music Certificate', ?, 'draft', 50, 'test')", programUuid, creator);
        programCourseUuid = UUID.randomUUID();
        jdbc.update("INSERT INTO program_courses (uuid, program_uuid, course_uuid, sequence_order, is_required, weight_percentage, created_by) "
                + "VALUES (?, ?, ?, 1, true, 100, 'test')", programCourseUuid, programUuid, course);
        skillUuid = UUID.randomUUID();
        jdbc.update("INSERT INTO skills (uuid, name, slug, created_by) VALUES (?, 'Performance technique', ?, 'test')",
                skillUuid, "performance-technique-" + Long.toHexString(System.nanoTime()));

        UUID teacher = instructorUser(TEACHER);
        UUID substitute = instructorUser(SUBSTITUTE);
        UUID otherTeacher = instructorUser(OTHER_TEACHER);
        classUuid = classDefinition(course, teacher, "Piano 2B");
        UUID session = session(classUuid, teacher);
        session(classUuid, substitute);
        otherClassUuid = classDefinition(course, otherTeacher, "Piano 3A");
        UUID otherSession = session(otherClassUuid, otherTeacher);

        studentUuid = student(STUDENT);
        seat(session, studentUuid);
        jdbc.update("INSERT INTO course_enrollments (uuid, student_uuid, course_uuid, status, final_grade, result_status, created_by) "
                + "VALUES (?, ?, ?, 'active', 72.5, 'passed', 'test')", UUID.randomUUID(), studentUuid, course);
        jdbc.update("INSERT INTO program_enrollments (uuid, student_uuid, program_uuid, status, final_grade, created_by) "
                + "VALUES (?, ?, ?, 'active', 72.5, 'test')", UUID.randomUUID(), studentUuid, programUuid);

        UUID parentUser = user(PARENT);
        grantDomain(parentUser, "parent");
        jdbc.update("INSERT INTO student_guardian_links (student_uuid, guardian_user_uuid, relationship_type, share_scope, link_status, created_by) "
                + "VALUES (?, ?, 'PARENT', 'ACADEMICS', 'ACTIVE', 'test')", studentUuid, parentUser);

        UUID outsider = student(OUTSIDER);
        seat(otherSession, outsider);
        grantDomain(user(ADMIN), "admin");
    }

    @Test
    @DisplayName("only the programme's creator (or an admin) sets course weights and target skills")
    void setupIsOwnerOnly() throws Exception {
        String weights = "[{\"program_course_uuid\":\"" + programCourseUuid + "\",\"is_required\":true,\"weight_percentage\":100}]";
        mockMvc.perform(put(programUrl() + "/course-weights").with(jwt(OTHER_CREATOR)).contentType(MediaType.APPLICATION_JSON).content(weights))
                .andExpect(status().isForbidden());
        mockMvc.perform(put(programUrl() + "/course-weights").with(jwt(STUDENT)).contentType(MediaType.APPLICATION_JSON).content(weights))
                .andExpect(status().isForbidden());
        mockMvc.perform(put(programUrl() + "/course-weights").with(jwt(CREATOR)).contentType(MediaType.APPLICATION_JSON).content(weights))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].weight_percentage").value(100.0));
        mockMvc.perform(put(programUrl() + "/course-weights").with(jwt(CREATOR)).contentType(MediaType.APPLICATION_JSON)
                        .content(weights.replace("100", "90")))
                .andExpect(status().isBadRequest());

        String skills = "{\"skill_uuids\":[\"" + skillUuid + "\"]}";
        mockMvc.perform(put(programUrl() + "/skills").with(jwt(OTHER_CREATOR)).contentType(MediaType.APPLICATION_JSON).content(skills))
                .andExpect(status().isForbidden());
        mockMvc.perform(put(programUrl() + "/skills").with(jwt(CREATOR)).contentType(MediaType.APPLICATION_JSON).content(skills))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].skill_name").value("Performance technique"))
                .andExpect(jsonPath("$.data[0].ready").value(false));
    }

    @Test
    @DisplayName("anyone signed in reads the skill level scale; only a platform admin replaces it")
    void skillLevelsAreAdminEdited() throws Exception {
        String scale = "[{\"name\":\"Not achieved\",\"min_percentage\":0,\"counts_as_achieved\":false},"
                + "{\"name\":\"Developing\",\"min_percentage\":50,\"counts_as_achieved\":false},"
                + "{\"name\":\"Achieved\",\"min_percentage\":70,\"counts_as_achieved\":true}]";
        mockMvc.perform(get("/api/v1/skill-levels").with(jwt(STUDENT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3));
        mockMvc.perform(put("/api/v1/admin/skill-levels").with(jwt(CREATOR)).contentType(MediaType.APPLICATION_JSON).content(scale))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/admin/skill-levels").with(jwt(ADMIN)).contentType(MediaType.APPLICATION_JSON).content(scale))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("the learner, a guardian sharing academics and the class's instructors read the report; others don't")
    void reportReaders() throws Exception {
        for (String reader : new String[]{STUDENT, PARENT, TEACHER, SUBSTITUTE, ADMIN}) {
            mockMvc.perform(get(reportUrl()).with(jwt(reader)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.program_title").value("Foundation Music Certificate"))
                    .andExpect(jsonPath("$.data.courses[0].result_status").value("PASSED"))
                    .andExpect(jsonPath("$.data.courses[0].contribution").value(72.5));
        }
        for (String stranger : new String[]{OTHER_TEACHER, OUTSIDER, CREATOR}) {
            mockMvc.perform(get(reportUrl()).with(jwt(stranger))).andExpect(status().isForbidden());
        }
    }

    @Test
    @DisplayName("a learner's programme list shows only programmes whose report the caller may open")
    void programmeListFollowsReadRules() throws Exception {
        for (String reader : new String[]{STUDENT, PARENT, TEACHER}) {
            mockMvc.perform(get("/api/v1/programs/students/" + studentUuid + "/reports").with(jwt(reader)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].program_title").value("Foundation Music Certificate"));
        }
        mockMvc.perform(get("/api/v1/programs/students/" + studentUuid + "/reports").with(jwt(OUTSIDER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("only the class's default instructor writes remarks, and the learner reads them")
    void remarksAreWrittenByTheDefaultInstructor() throws Exception {
        String remark = "{\"strengths\":\"<p>Strong left hand</p>\",\"recommendations\":\"<p>Metronome daily</p>\",\"next_review_date\":\"2026-11-30\"}";
        for (String writer : new String[]{SUBSTITUTE, PARENT, STUDENT, OTHER_TEACHER}) {
            mockMvc.perform(post(remarksUrl()).with(jwt(writer)).contentType(MediaType.APPLICATION_JSON).content(remark))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(post(remarksUrl()).with(jwt(TEACHER)).contentType(MediaType.APPLICATION_JSON).content("{\"strengths\":\" \"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(remarksUrl()).with(jwt(TEACHER)).contentType(MediaType.APPLICATION_JSON).content(remark))
                .andExpect(status().isCreated());
        mockMvc.perform(get(remarksUrl()).with(jwt(STUDENT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].strengths").value("<p>Strong left hand</p>"))
                .andExpect(jsonPath("$.data[0].next_review_date").value("2026-11-30"));
    }

    @Test
    @DisplayName("a class's instructor sees its programme progress; another class's instructor doesn't")
    void classProgressIsForTheClassInstructor() throws Exception {
        mockMvc.perform(get("/api/v1/classes/" + classUuid + "/programme-progress").with(jwt(TEACHER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.program_uuid").value(programUuid.toString()))
                .andExpect(jsonPath("$.data.learners.length()").value(1))
                .andExpect(jsonPath("$.data.learners[0].mandatory_passed").value(1));
        mockMvc.perform(get("/api/v1/classes/" + classUuid + "/programme-progress").with(jwt(OTHER_TEACHER)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/classes/" + otherClassUuid + "/programme-progress").with(jwt(OTHER_TEACHER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.learners.length()").value(0));
    }

    private String programUrl() {
        return "/api/v1/programs/" + programUuid;
    }

    private String reportUrl() {
        return programUrl() + "/students/" + studentUuid + "/report";
    }

    private String remarksUrl() {
        return programUrl() + "/students/" + studentUuid + "/remarks";
    }

    private RequestPostProcessor jwt(String subject) {
        return org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                .jwt().jwt(builder -> builder.subject(subject).claim("sub", subject));
    }

    private UUID user(String keycloakId) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO users (uuid, user_no, first_name, last_name, email, keycloak_id, created_by) "
                        + "VALUES (?, ?, 'Test', 'User', ?, ?, 'test')",
                uuid, String.format("%09d", Math.abs(uuid.hashCode()) % 1000000000), keycloakId + "@prog.test", keycloakId);
        return uuid;
    }

    private void grantDomain(UUID userUuid, String domainName) {
        jdbc.update("INSERT INTO user_domain_mapping (user_uuid, domain_uuid) "
                + "VALUES (?, (SELECT uuid FROM user_domain WHERE domain_name = ?))", userUuid, domainName);
    }

    private UUID courseCreator(UUID userUuid) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO course_creators (uuid, user_uuid, full_name, created_by) VALUES (?, ?, 'Course Creator', 'test')",
                uuid, userUuid);
        return uuid;
    }

    private UUID instructorUser(String keycloakId) {
        UUID userUuid = user(keycloakId);
        grantDomain(userUuid, "instructor");
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO instructors (uuid, user_uuid, full_name, admin_verified, created_by) VALUES (?, ?, ?, true, 'test')",
                uuid, userUuid, keycloakId);
        return uuid;
    }

    private UUID student(String keycloakId) {
        UUID userUuid = user(keycloakId);
        grantDomain(userUuid, "student");
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO students (uuid, user_uuid, full_name, created_by) VALUES (?, ?, ?, 'test')", uuid, userUuid, keycloakId);
        return uuid;
    }

    private UUID classDefinition(UUID course, UUID instructorUuid, String title) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO class_definitions (uuid, title, default_instructor_uuid, course_uuid, "
                        + "default_start_time, default_end_time, location_type, class_visibility, session_format, "
                        + "max_participants, is_active, registration_period_start_date, registration_period_end_date, created_by) "
                        + "VALUES (?, ?, ?, ?, TIMESTAMPTZ '2026-05-05 09:00:00+00', TIMESTAMPTZ '2026-05-05 09:45:00+00', "
                        + "'IN_PERSON', 'PUBLIC', 'GROUP', 20, true, (now() AT TIME ZONE 'UTC')::date - 30, "
                        + "(now() AT TIME ZONE 'UTC')::date + 365, 'test')",
                uuid, title, instructorUuid, course);
        return uuid;
    }

    private UUID session(UUID classDefinitionUuid, UUID instructorUuid) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO scheduled_instances (uuid, class_definition_uuid, instructor_uuid, start_time, end_time, title, "
                        + "location_type, max_participants, status, created_by) "
                        + "VALUES (?, ?, ?, NOW(), NOW() + INTERVAL '45 minutes', 'Session', 'IN_PERSON', 20, 'SCHEDULED', 'test')",
                uuid, classDefinitionUuid, instructorUuid);
        return uuid;
    }

    private void seat(UUID sessionUuid, UUID student) {
        jdbc.update("INSERT INTO class_enrollments (uuid, scheduled_instance_uuid, student_uuid, status, created_by) "
                + "VALUES (?, ?, ?, 'ENROLLED', 'test')", UUID.randomUUID(), sessionUuid, student);
    }
}
