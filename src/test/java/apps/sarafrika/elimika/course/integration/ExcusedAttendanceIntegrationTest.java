package apps.sarafrika.elimika.course.integration;

import apps.sarafrika.elimika.course.model.Course;
import apps.sarafrika.elimika.course.model.CourseAssessment;
import apps.sarafrika.elimika.course.model.CourseAssessmentScore;
import apps.sarafrika.elimika.course.model.CourseEnrollment;
import apps.sarafrika.elimika.course.repository.CourseAssessmentRepository;
import apps.sarafrika.elimika.course.repository.CourseAssessmentScoreRepository;
import apps.sarafrika.elimika.course.repository.CourseEnrollmentRepository;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.course.service.impl.CourseGradeBookServiceImpl;
import apps.sarafrika.elimika.course.util.enums.ContentStatus;
import apps.sarafrika.elimika.course.util.enums.CourseAssessmentAggregationStrategy;
import apps.sarafrika.elimika.course.util.enums.CourseAttendanceStatus;
import apps.sarafrika.elimika.course.util.enums.CourseResultStatus;
import apps.sarafrika.elimika.course.util.enums.EnrollmentStatus;
import apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
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
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/** Excused sessions are left out of the attendance average and still let the course reach a result. */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import({CourseGradeBookServiceImpl.class, apps.sarafrika.elimika.course.internal.CourseResultService.class,
        apps.sarafrika.elimika.course.internal.GradeRevisionRecorder.class,
        apps.sarafrika.elimika.course.internal.CourseProgressReportBuilder.class,
        ExcusedAttendanceIntegrationTest.TestConfig.class})
@DisplayName("Excused attendance")
class ExcusedAttendanceIntegrationTest {

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

        @Bean
        ClassDefinitionLookupService classDefinitionLookupService() {
            return Mockito.mock(ClassDefinitionLookupService.class);
        }

        @Bean
        apps.sarafrika.elimika.shared.spi.enrollment.EnrollmentLookupService enrollmentLookupService() {
            return Mockito.mock(apps.sarafrika.elimika.shared.spi.enrollment.EnrollmentLookupService.class);
        }
    }

    @Autowired private CourseGradeBookServiceImpl gradeBookService;
    @Autowired private ClassDefinitionLookupService classDefinitionLookupService;
    @Autowired private CourseRepository courseRepository;
    @Autowired private CourseAssessmentRepository courseAssessmentRepository;
    @Autowired private CourseAssessmentScoreRepository assessmentScoreRepository;
    @Autowired private CourseEnrollmentRepository courseEnrollmentRepository;
    @Autowired private JdbcTemplate jdbc;

    private final UUID classUuid = UUID.randomUUID();
    private UUID studentUuid;
    private CourseAssessment attendance;
    private CourseEnrollment enrollment;

    @BeforeEach
    void seed() {
        UUID creatorUser = user("creator");
        UUID creatorUuid = UUID.randomUUID();
        jdbc.update("INSERT INTO course_creators (uuid, user_uuid, full_name, admin_verified, created_by) "
                + "VALUES (?, ?, 'Creator', true, 'test')", creatorUuid, creatorUser);
        UUID studentUser = user("student");
        studentUuid = UUID.randomUUID();
        jdbc.update("INSERT INTO students (uuid, user_uuid, full_name, created_by) VALUES (?, ?, 'Amani', 'test')",
                studentUuid, studentUser);

        Course course = new Course();
        course.setName("Violin Fundamentals");
        course.setCourseCreatorUuid(creatorUuid);
        course.setDescription("Course");
        course.setPrice(new BigDecimal("1500.00"));
        course.setMinimumTrainingFee(new BigDecimal("500.00"));
        course.setCreatorSharePercentage(new BigDecimal("60.00"));
        course.setInstructorSharePercentage(new BigDecimal("40.00"));
        course.setDurationHours(2);
        course.setDurationMinutes(0);
        course.setStatus(ContentStatus.PUBLISHED);
        course.setActive(true);
        course.setAdminApproved(true);
        course.setPassMark(new BigDecimal("40"));
        course = courseRepository.saveAndFlush(course);

        attendance = new CourseAssessment();
        attendance.setCourseUuid(course.getUuid());
        attendance.setAssessmentType("attendance");
        attendance.setTitle("Attendance");
        attendance.setWeightPercentage(new BigDecimal("100.00"));
        attendance.setAggregationStrategy(CourseAssessmentAggregationStrategy.POINTS_SUM);
        attendance.setPerLesson(false);
        attendance.setIsRequired(true);
        attendance.setActive(true);
        attendance.setSyncClassAttendance(true);
        attendance = courseAssessmentRepository.saveAndFlush(attendance);

        enrollment = new CourseEnrollment();
        enrollment.setStudentUuid(studentUuid);
        enrollment.setCourseUuid(course.getUuid());
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        enrollment = courseEnrollmentRepository.saveAndFlush(enrollment);

        ClassDefinitionLookupService.ClassDefinitionSnapshot snapshot =
                Mockito.mock(ClassDefinitionLookupService.ClassDefinitionSnapshot.class);
        when(snapshot.courseUuid()).thenReturn(course.getUuid());
        when(classDefinitionLookupService.findByUuid(classUuid)).thenReturn(Optional.of(snapshot));
    }

    @Test
    @DisplayName("an excused session is left out of the denominator, not scored zero")
    void excusedSessionIsLeftOut() {
        mark(UUID.randomUUID(), CourseAttendanceStatus.ATTENDED, 1);
        mark(UUID.randomUUID(), CourseAttendanceStatus.ABSENT, 2);
        mark(UUID.randomUUID(), CourseAttendanceStatus.EXCUSED, 3);

        assertThat(attendancePercentage()).isEqualByComparingTo("50.00");
        CourseEnrollment reloaded = courseEnrollmentRepository.findByUuid(enrollment.getUuid()).orElseThrow();
        assertThat(reloaded.getResultStatus()).isNotEqualTo(CourseResultStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("re-marking an excused session as attended scores it again")
    void excusedCanBeCorrected() {
        UUID session = UUID.randomUUID();
        mark(UUID.randomUUID(), CourseAttendanceStatus.ABSENT, 1);
        mark(session, CourseAttendanceStatus.EXCUSED, 2);
        assertThat(attendancePercentage()).isEqualByComparingTo("0.00");

        mark(session, CourseAttendanceStatus.ATTENDED, 2);
        assertThat(attendancePercentage()).isEqualByComparingTo("50.00");
    }

    private void mark(UUID session, CourseAttendanceStatus status, int number) {
        gradeBookService.syncAttendanceMark(session, classUuid, studentUuid, "Violin " + number,
                LocalDateTime.now(), status, number, null);
    }

    private BigDecimal attendancePercentage() {
        return assessmentScoreRepository.findByEnrollmentUuidAndAssessmentUuid(enrollment.getUuid(), attendance.getUuid())
                .map(CourseAssessmentScore::getPercentage).orElseThrow();
    }

    private UUID user(String role) {
        UUID uuid = UUID.randomUUID();
        jdbc.update("INSERT INTO users (uuid, first_name, last_name, email, user_no, created_by) VALUES (?, 'Test', ?, ?, ?, 'test')",
                uuid, role, role + Long.toHexString(System.nanoTime()) + "@example.com",
                String.valueOf(100_000_000 + (int) (Math.random() * 899_999_999)));
        return uuid;
    }
}
