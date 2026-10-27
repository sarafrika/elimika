package apps.sarafrika.elimika.course.integration;

import apps.sarafrika.elimika.course.dto.ProgramCourseWeightDTO;
import apps.sarafrika.elimika.course.internal.ProgramResultService;
import apps.sarafrika.elimika.course.model.ProgramEnrollment;
import apps.sarafrika.elimika.course.repository.ProgramEnrollmentRepository;
import apps.sarafrika.elimika.course.service.impl.ProgramCourseServiceImpl;
import apps.sarafrika.elimika.course.util.enums.CourseResultStatus;
import apps.sarafrika.elimika.shared.utils.GenericSpecificationBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Programme marks from the Skill Wallet worksheet: five weighted mandatory courses plus one optional. */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import({ProgramCourseServiceImpl.class, ProgramResultService.class, GenericSpecificationBuilder.class,
        ProgrammeResultIntegrationTest.TestConfig.class})
@DisplayName("Weighted programme marks")
class ProgrammeResultIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @EnableJpaAuditing
    static class TestConfig {
        @Bean
        @Primary
        AuditorAware<String> auditorAware() {
            return () -> Optional.of("integration-test");
        }
    }

    @Autowired private ProgramCourseServiceImpl programCourseService;
    @Autowired private ProgramEnrollmentRepository programEnrollmentRepository;
    @Autowired private JdbcTemplate jdbc;

    private UUID creatorUuid;
    private UUID studentUuid;
    private UUID programUuid;
    private final List<UUID> courses = new ArrayList<>();
    private final List<UUID> programCourses = new ArrayList<>();
    private UUID enrollmentUuid;

    @BeforeEach
    void seedWorksheetProgramme() {
        courses.clear();
        programCourses.clear();
        UUID creatorUser = user("creator");
        creatorUuid = UUID.randomUUID();
        jdbc.update("INSERT INTO course_creators (uuid, user_uuid, full_name, admin_verified, created_by) "
                + "VALUES (?, ?, 'Creator', true, 'test')", creatorUuid, creatorUser);
        UUID studentUser = user("student");
        studentUuid = UUID.randomUUID();
        jdbc.update("INSERT INTO students (uuid, user_uuid, full_name, created_by) VALUES (?, ?, 'Amani', 'test')",
                studentUuid, studentUser);
        programUuid = UUID.randomUUID();
        jdbc.update("INSERT INTO training_programs (uuid, title, course_creator_uuid, status, pass_mark, created_by) "
                + "VALUES (?, 'Foundation Music Certificate', ?, 'draft', 50, 'test')", programUuid, creatorUuid);
        for (int i = 0; i < 6; i++) {
            UUID course = UUID.randomUUID();
            jdbc.update("INSERT INTO courses (uuid, name, course_creator_uuid, status, active, created_by) "
                    + "VALUES (?, ?, ?, 'published', true, 'test')", course, "MUS10" + (i + 1), creatorUuid);
            UUID programCourse = UUID.randomUUID();
            jdbc.update("INSERT INTO program_courses (uuid, program_uuid, course_uuid, sequence_order, is_required, created_by) "
                    + "VALUES (?, ?, ?, ?, ?, 'test')", programCourse, programUuid, course, i + 1, i < 5);
            courses.add(course);
            programCourses.add(programCourse);
        }
        enrollmentUuid = UUID.randomUUID();
        jdbc.update("INSERT INTO program_enrollments (uuid, student_uuid, program_uuid, status, created_by) "
                + "VALUES (?, ?, ?, 'active', 'test')", enrollmentUuid, studentUuid, programUuid);
    }

    @Test
    @DisplayName("provisional mark re-normalises over decided mandatory courses and ignores the optional one")
    void provisionalMarkIgnoresUndecidedAndOptionalCourses() {
        courseResult(0, "91.67", "passed");
        courseResult(1, "78.40", "passed");
        courseResult(2, "64.10", "passed");
        courseResult(3, "72.00", "in_progress");
        courseResult(5, "20.00", "failed");

        saveWorksheetWeights();

        ProgramEnrollment enrollment = enrollment();
        // (91.67×15 + 78.40×20 + 64.10×25) ÷ 60 = 75.76
        assertThat(enrollment.getFinalGrade()).isEqualByComparingTo("75.76");
        assertThat(enrollment.getFinalGradeProvisional()).isTrue();
        assertThat(enrollment.getResultStatus()).isEqualTo(CourseResultStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("passes once every mandatory course is passed and the mark reaches the pass mark")
    void passesWhenAllMandatoryPassedAndMarkReached() {
        for (int i = 0; i < 5; i++) {
            courseResult(i, "70.00", "passed");
        }
        saveWorksheetWeights();

        ProgramEnrollment enrollment = enrollment();
        assertThat(enrollment.getFinalGrade()).isEqualByComparingTo("70.00");
        assertThat(enrollment.getFinalGradeProvisional()).isFalse();
        assertThat(enrollment.getResultStatus()).isEqualTo(CourseResultStatus.PASSED);
    }

    @Test
    @DisplayName("a failed mandatory course fails the programme even when the weighted mark is above the pass mark")
    void failedMandatoryCourseFailsProgramme() {
        courseResult(0, "95.00", "passed");
        courseResult(1, "95.00", "passed");
        courseResult(2, "95.00", "passed");
        courseResult(3, "40.00", "failed");
        courseResult(4, "95.00", "passed");
        saveWorksheetWeights();

        ProgramEnrollment enrollment = enrollment();
        assertThat(enrollment.getFinalGrade()).isGreaterThan(new BigDecimal("50"));
        assertThat(enrollment.getResultStatus()).isEqualTo(CourseResultStatus.FAILED);
    }

    @Test
    @DisplayName("mandatory weights must total exactly 100% and optional courses can't carry weight")
    void rejectsInvalidWeights() {
        List<ProgramCourseWeightDTO> short90 = weights("15", "20", "25", "15", "15", null);
        assertThatThrownBy(() -> programCourseService.updateCourseWeights(programUuid, short90))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("100%");

        List<ProgramCourseWeightDTO> weightedOptional = weights("15", "20", "25", "15", "25", "10");
        assertThatThrownBy(() -> programCourseService.updateCourseWeights(programUuid, weightedOptional))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Optional");

        List<ProgramCourseWeightDTO> missingRow = weights("15", "20", "25", "15", "25", null).subList(0, 5);
        assertThatThrownBy(() -> programCourseService.updateCourseWeights(programUuid, missingRow))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("every course");
    }

    @Test
    @DisplayName("publishing is blocked until mandatory weights total 100%")
    void publishGateNeedsCompleteWeights() {
        jdbc.update("UPDATE program_courses SET weight_percentage = NULL WHERE program_uuid = ?", programUuid);
        assertThatThrownBy(() -> programCourseService.enforceCompleteWeights(programUuid))
                .isInstanceOf(IllegalStateException.class);

        saveWorksheetWeights();
        programCourseService.enforceCompleteWeights(programUuid);
    }

    private void saveWorksheetWeights() {
        programCourseService.updateCourseWeights(programUuid, weights("15", "20", "25", "15", "25", null));
    }

    private List<ProgramCourseWeightDTO> weights(String... values) {
        List<ProgramCourseWeightDTO> rows = new ArrayList<>();
        for (int i = 0; i < values.length; i++) {
            rows.add(new ProgramCourseWeightDTO(programCourses.get(i), i < 5,
                    values[i] == null ? null : new BigDecimal(values[i])));
        }
        return rows;
    }

    private void courseResult(int index, String finalGrade, String result) {
        jdbc.update("INSERT INTO course_enrollments (uuid, student_uuid, course_uuid, status, final_grade, result_status, created_by) "
                        + "VALUES (?, ?, ?, 'active', ?, ?, 'test')",
                UUID.randomUUID(), studentUuid, courses.get(index), new BigDecimal(finalGrade), result);
    }

    private ProgramEnrollment enrollment() {
        return programEnrollmentRepository.findByUuid(enrollmentUuid).orElseThrow();
    }

    private UUID user(String role) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO users (uuid, first_name, last_name, email, user_no, created_by) VALUES (?, 'Test', ?, ?, ?, 'test')",
                uuid, role, role + Long.toHexString(System.nanoTime()) + "@example.com",
                String.valueOf(100_000_000 + (int) (Math.random() * 899_999_999)));
        return uuid;
    }
}
