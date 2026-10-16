package apps.sarafrika.elimika.course.integration;

import apps.sarafrika.elimika.course.dto.CourseAssessmentGradeRevisionDTO;
import apps.sarafrika.elimika.course.dto.CourseAssessmentLineItemRubricEvaluationDTO;
import apps.sarafrika.elimika.course.dto.CourseAssessmentLineItemRubricEvaluationRowDTO;
import apps.sarafrika.elimika.course.dto.CourseGradeBookDTO;
import apps.sarafrika.elimika.course.model.AssessmentRubric;
import apps.sarafrika.elimika.course.model.Course;
import apps.sarafrika.elimika.course.model.CourseAssessment;
import apps.sarafrika.elimika.course.model.CourseAssessmentLineItem;
import apps.sarafrika.elimika.course.model.CourseEnrollment;
import apps.sarafrika.elimika.course.model.Lesson;
import apps.sarafrika.elimika.course.model.RubricCriteria;
import apps.sarafrika.elimika.course.model.RubricScoringLevel;
import apps.sarafrika.elimika.course.repository.AssessmentRubricRepository;
import apps.sarafrika.elimika.course.repository.CourseAssessmentLineItemRepository;
import apps.sarafrika.elimika.course.repository.CourseAssessmentLineItemScoreRepository;
import apps.sarafrika.elimika.course.repository.CourseAssessmentRepository;
import apps.sarafrika.elimika.course.repository.CourseEnrollmentRepository;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.course.repository.LessonRepository;
import apps.sarafrika.elimika.course.repository.RubricCriteriaRepository;
import apps.sarafrika.elimika.course.repository.RubricScoringLevelRepository;
import apps.sarafrika.elimika.course.service.impl.CourseGradeBookServiceImpl;
import apps.sarafrika.elimika.course.util.enums.ContentStatus;
import apps.sarafrika.elimika.course.util.enums.CourseAssessmentAggregationStrategy;
import apps.sarafrika.elimika.course.util.enums.CourseAssessmentLineItemRubricEvaluationStatus;
import apps.sarafrika.elimika.course.util.enums.CourseAssessmentLineItemType;
import apps.sarafrika.elimika.course.util.enums.CourseResultStatus;
import apps.sarafrika.elimika.course.util.enums.EnrollmentStatus;
import apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Replays the St. Scholastica Term 2 progress-report workbook through the real gradebook.
 * Six criteria scored 0–5 (workbook validation "5,4,3,2,0"), lesson total = SUM out of 30.
 * Tests named "red" encode workbook behaviour the system does not support yet.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import({CourseGradeBookServiceImpl.class, apps.sarafrika.elimika.course.internal.CourseResultService.class,
        apps.sarafrika.elimika.course.internal.GradeRevisionRecorder.class,
        WorkbookLessonGradingIntegrationTest.TestConfig.class})
@DisplayName("Instructor lesson grading against the Term 2 workbook")
class WorkbookLessonGradingIntegrationTest {

    private static final List<String> CRITERIA = List.of(
            "Technique", "Tonal Quality", "Rhythm", "Pitch", "Synchronisation", "Presentation/Expression");

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

    @Autowired private CourseGradeBookServiceImpl gradeBookService;
    @Autowired private CourseRepository courseRepository;
    @Autowired private LessonRepository lessonRepository;
    @Autowired private CourseAssessmentRepository courseAssessmentRepository;
    @Autowired private CourseAssessmentLineItemRepository lineItemRepository;
    @Autowired private CourseAssessmentLineItemScoreRepository lineItemScoreRepository;
    @Autowired private CourseEnrollmentRepository courseEnrollmentRepository;
    @Autowired private AssessmentRubricRepository rubricRepository;
    @Autowired private RubricCriteriaRepository criteriaRepository;
    @Autowired private RubricScoringLevelRepository scoringLevelRepository;
    @Autowired private JdbcTemplate jdbc;

    private UUID courseCreatorUuid;
    private UUID studentUuid;

    /** One per-lesson performance component with a six-criterion rubric. */
    private record Setup(Course course, CourseAssessment assessment, AssessmentRubric rubric,
                         List<RubricCriteria> criteria, List<RubricScoringLevel> levels,
                         List<CourseAssessmentLineItem> lessons, CourseEnrollment enrollment) {

        RubricScoringLevel level(int points) {
            return levels.stream()
                    .filter(l -> l.getPoints().compareTo(BigDecimal.valueOf(points)) == 0)
                    .findFirst().orElseThrow(() -> new IllegalArgumentException("no level worth " + points));
        }
    }

    @BeforeEach
    void seedOwners() {
        UUID creatorUser = UUID.randomUUID();
        jdbc.update("INSERT INTO users (uuid, first_name, last_name, email, user_no, created_by) "
                        + "VALUES (?, 'Test', 'Creator', ?, ?, 'test')",
                creatorUser, "c" + Long.toHexString(System.nanoTime()) + "@example.com", randomUserNo());
        courseCreatorUuid = UUID.randomUUID();
        jdbc.update("INSERT INTO course_creators (uuid, user_uuid, full_name, admin_verified, created_by) "
                        + "VALUES (?, ?, 'Test Creator', true, 'test')", courseCreatorUuid, creatorUser);

        UUID studentUser = UUID.randomUUID();
        jdbc.update("INSERT INTO users (uuid, first_name, last_name, email, user_no, created_by) "
                        + "VALUES (?, 'Test', 'Student', ?, ?, 'test')",
                studentUser, "s" + Long.toHexString(System.nanoTime()) + "@example.com", randomUserNo());
        studentUuid = UUID.randomUUID();
        jdbc.update("INSERT INTO students (uuid, user_uuid, full_name, created_by) "
                + "VALUES (?, ?, 'Test Student', 'test')", studentUuid, studentUser);
    }

    @Nested
    @DisplayName("Behaviour that already matches the workbook")
    class Baseline {

        @Test
        @DisplayName("lesson total is the plain SUM of the six criteria, out of 30 (sheet 08 row 7: 4,4,5,5,4,4 = 26)")
        void lessonTotalIsSumOfSixCriteria() {
            Setup s = setup(1, descendingLevels(), null);

            CourseAssessmentLineItemRubricEvaluationDTO saved = grade(s, 0, 4, 4, 5, 5, 4, 4);

            assertThat(saved.score()).isEqualByComparingTo("26");
            assertThat(saved.maxScore()).isEqualByComparingTo("30");
        }

        @Test
        @DisplayName("fully graded term: final equals the average lesson total (sheet 10 D4 scored lessons 29,29,29,30,27,21 → 27.5/30 = 91.67%)")
        void fullyGradedFinalMatchesWorkbookAverage() {
            Setup s = setup(6, descendingLevels(), null);
            int[][] lessons = {
                    {5, 5, 5, 5, 5, 4}, {5, 5, 5, 5, 4, 5}, {5, 4, 5, 5, 5, 5},
                    {5, 5, 5, 5, 5, 5}, {5, 4, 4, 5, 4, 5}, {4, 3, 4, 3, 3, 4}};
            for (int i = 0; i < lessons.length; i++) {
                grade(s, i, lessons[i]);
            }

            CourseEnrollment reloaded = reload(s);
            assertThat(reloaded.getFinalGrade()).isEqualByComparingTo("91.67");
            assertThat(reloaded.getResultStatus()).isEqualTo(CourseResultStatus.PASSED);
        }

        @Test
        @DisplayName("ungraded lessons are not counted as 0 and the result stays IN_PROGRESS (workbook divides by 9 instead — needs a policy decision)")
        void ungradedLessonsAreNotZeroFilled() {
            Setup s = setup(9, descendingLevels(), null);
            grade(s, 0, 5, 4, 5, 5, 4, 4); // 27, like sheet 14 E6

            CourseEnrollment reloaded = reload(s);
            assertThat(reloaded.getFinalGrade()).isEqualByComparingTo("90.00"); // workbook D4 shows 3.0/30
            assertThat(reloaded.getResultStatus()).isEqualTo(CourseResultStatus.IN_PROGRESS);
        }
    }

    @Nested
    @DisplayName("Red: workbook behaviour the system does not support yet")
    class Red {

        @Test
        @DisplayName("an instructor can correct a lesson's scores; the new score replaces the old one")
        void regradeReplacesScore() {
            Setup s = setup(1, descendingLevels(), null);

            grade(s, 0, 3, 3, 3, 3, 3, 3);
            CourseAssessmentLineItemRubricEvaluationDTO regraded = grade(s, 0, 5, 5, 5, 4, 4, 4);

            assertThat(regraded.score()).isEqualByComparingTo("27");
            assertThat(lineItemScoreRepository.findByEnrollmentUuidAndLineItemUuidIn(
                    s.enrollment().getUuid(), List.of(s.lessons().getFirst().getUuid()))).hasSize(1);
        }

        @Test
        @DisplayName("every grading attempt and regrade of a lesson stays visible, oldest first")
        void regradesKeepFullHistory() {
            Setup s = setup(1, descendingLevels(), null);

            grade(s, 0, 3, 3, 3, 3, 3, 3);
            grade(s, 0, 4, 4, 4, 4, 4, 4);
            grade(s, 0, 5, 5, 5, 4, 4, 4);

            List<CourseAssessmentGradeRevisionDTO> revisions = gradeBookService.getLineItemRevisions(s.course().getUuid(),
                    s.assessment().getUuid(), s.lessons().getFirst().getUuid(), s.enrollment().getUuid());
            assertThat(revisions).extracting(CourseAssessmentGradeRevisionDTO::revisionNumber).containsExactly(1, 2, 3);
            assertThat(revisions).extracting(CourseAssessmentGradeRevisionDTO::score)
                    .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                    .containsExactly(new BigDecimal("18"), new BigDecimal("24"), new BigDecimal("27"));
            assertThat(revisions.getFirst().criteria()).hasSize(6)
                    .allSatisfy(c -> assertThat(c.points()).isEqualByComparingTo("3"));
        }

        @Test
        @DisplayName("a 0–5 scale listed lowest-first still gives a lesson out of 30 (workbook allows 0)")
        void zeroToFiveScaleInAscendingOrder() {
            Setup s = setup(1, ascendingLevels(), null);

            assertThatCode(() -> grade(s, 0, 4, 0, 5, 5, 4, 4)).doesNotThrowAnyException();
            CourseAssessmentLineItemRubricEvaluationDTO saved =
                    gradeBookService.getLineItemRubricEvaluation(s.course().getUuid(), s.assessment().getUuid(),
                            s.lessons().getFirst().getUuid(), s.enrollment().getUuid());
            assertThat(saved.score()).isEqualByComparingTo("22");
            assertThat(saved.maxScore()).isEqualByComparingTo("30");
        }

        @Test
        @DisplayName("lesson maximum follows the rubric's six criteria, not a stale stored max_score")
        void staleRubricMaxScoreIsIgnored() {
            Setup s = setup(1, descendingLevels(), new BigDecimal("25.00"));

            CourseAssessmentLineItemRubricEvaluationDTO saved = grade(s, 0, 5, 5, 5, 5, 5, 5);

            assertThat(saved.maxScore()).isEqualByComparingTo("30");
            assertThat(saved.percentage()).isEqualByComparingTo("100.00");
        }
    }

    private CourseAssessmentLineItemRubricEvaluationDTO grade(Setup s, int lessonIndex, int... points) {
        CourseAssessmentLineItemRubricEvaluationDTO dto = new CourseAssessmentLineItemRubricEvaluationDTO(
                null, null, null, null, null, null, null, null, null, null,
                LocalDateTime.now(), null, selections(s, points));
        return gradeBookService.upsertLineItemRubricEvaluation(s.course().getUuid(), s.assessment().getUuid(),
                s.lessons().get(lessonIndex).getUuid(), s.enrollment().getUuid(), dto);
    }

    private List<CourseAssessmentLineItemRubricEvaluationRowDTO> selections(Setup s, int... points) {
        List<CourseAssessmentLineItemRubricEvaluationRowDTO> rows = new ArrayList<>();
        for (int i = 0; i < points.length; i++) {
            rows.add(new CourseAssessmentLineItemRubricEvaluationRowDTO(
                    null, s.criteria().get(i).getUuid(), null, s.level(points[i]).getUuid(), null, null, null));
        }
        return rows;
    }

    private static List<Integer> descendingLevels() {
        return List.of(5, 4, 3, 2, 0);
    }

    private static List<Integer> ascendingLevels() {
        return List.of(0, 2, 3, 4, 5);
    }

    private Setup setup(int lessonCount, List<Integer> levelPointsInOrder, BigDecimal storedMaxScore) {
        Course course = persistCourse();
        AssessmentRubric rubric = new AssessmentRubric();
        rubric.setTitle("Instrument performance");
        rubric.setRubricType("performance");
        rubric.setCourseCreatorUuid(courseCreatorUuid);
        rubric.setStatus(ContentStatus.PUBLISHED);
        rubric.setIsActive(true);
        rubric.setIsPublic(false);
        rubric.setUsesCustomLevels(true);
        rubric.setMaxScore(storedMaxScore);
        rubric.setTotalWeight(new BigDecimal("100.00"));
        rubric.setWeightUnit("percentage");
        rubric = rubricRepository.saveAndFlush(rubric);

        List<RubricCriteria> criteria = new ArrayList<>();
        for (int i = 0; i < CRITERIA.size(); i++) {
            RubricCriteria c = new RubricCriteria();
            c.setRubricUuid(rubric.getUuid());
            c.setComponentName(CRITERIA.get(i));
            c.setDisplayOrder(i + 1);
            criteria.add(criteriaRepository.saveAndFlush(c));
        }
        List<RubricScoringLevel> levels = new ArrayList<>();
        for (int i = 0; i < levelPointsInOrder.size(); i++) {
            RubricScoringLevel l = new RubricScoringLevel();
            l.setRubricUuid(rubric.getUuid());
            l.setName("Level " + levelPointsInOrder.get(i));
            l.setPoints(BigDecimal.valueOf(levelPointsInOrder.get(i)));
            l.setLevelOrder(i + 1);
            l.setIsPassing(levelPointsInOrder.get(i) >= 3);
            levels.add(scoringLevelRepository.saveAndFlush(l));
        }

        CourseAssessment assessment = new CourseAssessment();
        assessment.setCourseUuid(course.getUuid());
        assessment.setAssessmentType("performance");
        assessment.setTitle("Lesson performance");
        assessment.setWeightPercentage(new BigDecimal("100.00"));
        assessment.setAggregationStrategy(CourseAssessmentAggregationStrategy.POINTS_SUM);
        assessment.setRubricUuid(rubric.getUuid());
        assessment.setPerLesson(true);
        assessment.setIsRequired(true);
        assessment.setActive(true);
        assessment.setSyncClassAttendance(false);
        assessment = courseAssessmentRepository.saveAndFlush(assessment);

        List<CourseAssessmentLineItem> lineItems = new ArrayList<>();
        for (int i = 1; i <= lessonCount; i++) {
            Lesson lesson = new Lesson();
            lesson.setCourseUuid(course.getUuid());
            lesson.setLessonNumber(i);
            lesson.setTitle("Class " + i);
            lesson.setStatus(ContentStatus.PUBLISHED);
            lesson.setActive(true);
            lesson = lessonRepository.saveAndFlush(lesson);

            CourseAssessmentLineItem item = new CourseAssessmentLineItem();
            item.setCourseAssessmentUuid(assessment.getUuid());
            item.setTitle("Class " + i);
            item.setLessonUuid(lesson.getUuid());
            item.setItemType(CourseAssessmentLineItemType.PERFORMANCE);
            item.setMaxScore(new BigDecimal("30.00"));
            item.setDisplayOrder(i);
            item.setActive(true);
            lineItems.add(lineItemRepository.saveAndFlush(item));
        }
        return new Setup(course, assessment, rubric, criteria, levels, lineItems, persistEnrollment(course.getUuid()));
    }

    private Course persistCourse() {
        Course course = new Course();
        course.setName("Introduction to Piano");
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

    private CourseEnrollment persistEnrollment(UUID courseUuid) {
        CourseEnrollment enrollment = new CourseEnrollment();
        enrollment.setStudentUuid(studentUuid);
        enrollment.setCourseUuid(courseUuid);
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        return courseEnrollmentRepository.saveAndFlush(enrollment);
    }

    private CourseEnrollment reload(Setup s) {
        return courseEnrollmentRepository.findByUuid(s.enrollment().getUuid()).orElseThrow();
    }

    private static List<String> componentNames(Class<? extends Record> type) {
        return Arrays.stream(type.getRecordComponents())
                .map(RecordComponent::getName)
                .map(n -> n.toLowerCase(Locale.ROOT))
                .toList();
    }

    private static String randomUserNo() {
        return String.valueOf(100_000_000 + (int) (Math.random() * 899_999_999));
    }
}
