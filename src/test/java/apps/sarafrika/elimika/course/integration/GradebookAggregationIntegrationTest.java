package apps.sarafrika.elimika.course.integration;

import apps.sarafrika.elimika.course.dto.CourseGradeBookDTO;
import apps.sarafrika.elimika.course.model.Assignment;
import apps.sarafrika.elimika.course.model.Course;
import apps.sarafrika.elimika.course.model.CourseAssessment;
import apps.sarafrika.elimika.course.model.CourseEnrollment;
import apps.sarafrika.elimika.course.model.Lesson;
import apps.sarafrika.elimika.course.model.Quiz;
import apps.sarafrika.elimika.course.repository.AssignmentRepository;
import apps.sarafrika.elimika.course.repository.CourseAssessmentLineItemRepository;
import apps.sarafrika.elimika.course.repository.CourseAssessmentRepository;
import apps.sarafrika.elimika.course.repository.CourseEnrollmentRepository;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.course.repository.LessonRepository;
import apps.sarafrika.elimika.course.repository.QuizRepository;
import apps.sarafrika.elimika.course.service.impl.CourseGradeBookServiceImpl;
import apps.sarafrika.elimika.course.util.enums.AttemptStatus;
import apps.sarafrika.elimika.course.util.enums.ContentStatus;
import apps.sarafrika.elimika.course.util.enums.CourseAssessmentAggregationStrategy;
import apps.sarafrika.elimika.course.util.enums.EnrollmentStatus;
import apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
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

/**
 * Exercises the graded-work → gradebook → final-grade chain against a real PostgreSQL schema.
 * <p>
 * Aggregation is database-shaped: derived line items are auto-created, their scores are summed
 * or weighted by the real SQL, and the course final grade is persisted onto the enrollment.
 * Mocked repositories cannot show that the migrations, foreign keys and aggregation actually
 * line up, so this runs the real migrations and the real service.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import({CourseGradeBookServiceImpl.class, GradebookAggregationIntegrationTest.TestConfig.class})
@DisplayName("Gradebook aggregation of graded quizzes and assignments")
class GradebookAggregationIntegrationTest {

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
    }

    @Autowired
    private CourseGradeBookServiceImpl gradeBookService;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private LessonRepository lessonRepository;
    @Autowired
    private CourseAssessmentRepository courseAssessmentRepository;
    @Autowired
    private CourseAssessmentLineItemRepository lineItemRepository;
    @Autowired
    private QuizRepository quizRepository;
    @Autowired
    private AssignmentRepository assignmentRepository;
    @Autowired
    private CourseEnrollmentRepository courseEnrollmentRepository;
    @Autowired
    private JdbcTemplate jdbc;

    private UUID courseCreatorUuid;
    private UUID studentUuid;

    @BeforeEach
    void seedOwners() {
        UUID creatorUser = UUID.randomUUID();
        jdbc.update("INSERT INTO users (uuid, first_name, last_name, email, user_no, created_by) "
                        + "VALUES (?, 'Test', 'Creator', ?, ?, 'test')",
                creatorUser, "c" + Long.toHexString(System.nanoTime()) + "@example.com", randomUserNo());
        courseCreatorUuid = UUID.randomUUID();
        jdbc.update("INSERT INTO course_creators (uuid, user_uuid, full_name, admin_verified, created_by) "
                        + "VALUES (?, ?, 'Test Creator', true, 'test')",
                courseCreatorUuid, creatorUser);

        UUID studentUser = UUID.randomUUID();
        jdbc.update("INSERT INTO users (uuid, first_name, last_name, email, user_no, created_by) "
                        + "VALUES (?, 'Test', 'Student', ?, ?, 'test')",
                studentUser, "s" + Long.toHexString(System.nanoTime()) + "@example.com", randomUserNo());
        studentUuid = UUID.randomUUID();
        jdbc.update("INSERT INTO students (uuid, user_uuid, full_name, created_by) "
                        + "VALUES (?, ?, 'Test Student', 'test')",
                studentUuid, studentUser);
    }

    private static String randomUserNo() {
        return String.valueOf(100_000_000 + (int) (Math.random() * 899_999_999));
    }

    @Test
    @DisplayName("points-sum: a graded quiz and assignment auto-create line items and sum into the final grade")
    void pointsSumAggregatesQuizAndAssignmentIntoFinalGrade() {
        Course course = persistCourse();
        Lesson lesson = persistLesson(course.getUuid());
        persistAssessment(course.getUuid(), CourseAssessmentAggregationStrategy.POINTS_SUM);
        Quiz quiz = persistQuiz(lesson.getUuid());
        Assignment assignment = persistAssignment(lesson.getUuid());
        CourseEnrollment enrollment = persistEnrollment(course.getUuid());

        gradeBookService.syncQuizAttemptGrade(quiz.getUuid(), enrollment.getUuid(),
                new BigDecimal("8"), new BigDecimal("10"), null, LocalDateTime.now(), null, AttemptStatus.GRADED);
        gradeBookService.syncAssignmentGrade(assignment.getUuid(), enrollment.getUuid(),
                new BigDecimal("45"), new BigDecimal("50"), "ok", LocalDateTime.now(), null);

        assertThat(lineItemRepository.findByQuizUuid(quiz.getUuid())).isPresent();
        assertThat(lineItemRepository.findByAssignmentUuid(assignment.getUuid())).isPresent();

        CourseEnrollment reloaded = courseEnrollmentRepository.findByUuid(enrollment.getUuid()).orElseThrow();
        // (8 + 45) / (10 + 50) = 88.33%
        assertThat(reloaded.getFinalGrade()).isEqualByComparingTo("88.33");

        CourseGradeBookDTO gradebook = gradeBookService.getEnrollmentGradeBook(course.getUuid(), enrollment.getUuid());
        assertThat(gradebook).isNotNull();
    }

    @Test
    @DisplayName("weighted-average: auto-created line items receive positive weights and average into the final grade")
    void weightedAverageAggregatesQuizAndAssignmentIntoFinalGrade() {
        Course course = persistCourse();
        Lesson lesson = persistLesson(course.getUuid());
        persistAssessment(course.getUuid(), CourseAssessmentAggregationStrategy.WEIGHTED_AVERAGE);
        Quiz quiz = persistQuiz(lesson.getUuid());
        Assignment assignment = persistAssignment(lesson.getUuid());
        CourseEnrollment enrollment = persistEnrollment(course.getUuid());

        gradeBookService.syncQuizAttemptGrade(quiz.getUuid(), enrollment.getUuid(),
                new BigDecimal("8"), new BigDecimal("10"), null, LocalDateTime.now(), null, AttemptStatus.GRADED);
        gradeBookService.syncAssignmentGrade(assignment.getUuid(), enrollment.getUuid(),
                new BigDecimal("45"), new BigDecimal("50"), "ok", LocalDateTime.now(), null);

        CourseEnrollment reloaded = courseEnrollmentRepository.findByUuid(enrollment.getUuid()).orElseThrow();
        // equal auto-weights: (80% + 90%) / 2 = 85.00%
        assertThat(reloaded.getFinalGrade()).isEqualByComparingTo("85.00");
    }

    private Course persistCourse() {
        Course course = new Course();
        course.setName("Music Theory");
        course.setCourseCreatorUuid(courseCreatorUuid);
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
        return courseRepository.saveAndFlush(course);
    }

    private Lesson persistLesson(UUID courseUuid) {
        Lesson lesson = new Lesson();
        lesson.setCourseUuid(courseUuid);
        lesson.setLessonNumber(1);
        lesson.setTitle("Lesson 1");
        lesson.setStatus(ContentStatus.PUBLISHED);
        lesson.setActive(true);
        return lessonRepository.saveAndFlush(lesson);
    }

    private CourseAssessment persistAssessment(UUID courseUuid, CourseAssessmentAggregationStrategy strategy) {
        CourseAssessment assessment = new CourseAssessment();
        assessment.setCourseUuid(courseUuid);
        assessment.setAssessmentType("coursework");
        assessment.setTitle("Coursework");
        assessment.setWeightPercentage(new BigDecimal("100.00"));
        assessment.setAggregationStrategy(strategy);
        assessment.setIsRequired(true);
        assessment.setActive(true);
        assessment.setSyncClassAttendance(false);
        return courseAssessmentRepository.saveAndFlush(assessment);
    }

    private Quiz persistQuiz(UUID lessonUuid) {
        Quiz quiz = new Quiz();
        quiz.setLessonUuid(lessonUuid);
        quiz.setTitle("Chapter quiz");
        quiz.setStatus(ContentStatus.PUBLISHED);
        quiz.setActive(true);
        return quizRepository.saveAndFlush(quiz);
    }

    private Assignment persistAssignment(UUID lessonUuid) {
        Assignment assignment = new Assignment();
        assignment.setLessonUuid(lessonUuid);
        assignment.setTitle("Composition assignment");
        assignment.setMaxPoints(new BigDecimal("50.00"));
        assignment.setIsPublished(true);
        return assignmentRepository.saveAndFlush(assignment);
    }

    private CourseEnrollment persistEnrollment(UUID courseUuid) {
        CourseEnrollment enrollment = new CourseEnrollment();
        enrollment.setStudentUuid(studentUuid);
        enrollment.setCourseUuid(courseUuid);
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        return courseEnrollmentRepository.saveAndFlush(enrollment);
    }
}
