package apps.sarafrika.elimika.course.service.impl;

import apps.sarafrika.elimika.course.dto.CourseAssessmentLineItemDTO;
import apps.sarafrika.elimika.course.dto.CourseAssessmentLineItemScoreDTO;
import apps.sarafrika.elimika.course.model.CourseAssessment;
import apps.sarafrika.elimika.course.model.CourseAssessmentLineItem;
import apps.sarafrika.elimika.course.model.CourseAssessmentLineItemScore;
import apps.sarafrika.elimika.course.model.CourseAssessmentScore;
import apps.sarafrika.elimika.course.model.CourseEnrollment;
import apps.sarafrika.elimika.course.model.Lesson;
import apps.sarafrika.elimika.course.model.Assignment;
import apps.sarafrika.elimika.course.repository.AssignmentRepository;
import apps.sarafrika.elimika.course.repository.CourseAssessmentLineItemRepository;
import apps.sarafrika.elimika.course.repository.CourseAssessmentLineItemScoreRepository;
import apps.sarafrika.elimika.course.repository.CourseAssessmentRepository;
import apps.sarafrika.elimika.course.repository.CourseAssessmentScoreRepository;
import apps.sarafrika.elimika.course.repository.CourseEnrollmentRepository;
import apps.sarafrika.elimika.course.repository.LessonRepository;
import apps.sarafrika.elimika.course.repository.QuizRepository;
import apps.sarafrika.elimika.course.util.enums.AttemptStatus;
import apps.sarafrika.elimika.course.util.enums.CourseAssessmentAggregationStrategy;
import apps.sarafrika.elimika.course.util.enums.CourseAssessmentLineItemType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CourseGradebookServiceImplTest {

    @Mock
    private CourseAssessmentRepository courseAssessmentRepository;

    @Mock
    private CourseAssessmentLineItemRepository lineItemRepository;

    @Mock
    private CourseAssessmentLineItemScoreRepository lineItemScoreRepository;

    @Mock
    private CourseAssessmentScoreRepository courseAssessmentScoreRepository;

    @Mock
    private CourseEnrollmentRepository courseEnrollmentRepository;

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private QuizRepository quizRepository;

    @Mock
    private LessonRepository lessonRepository;

    private CourseGradebookServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CourseGradebookServiceImpl(
                courseAssessmentRepository,
                lineItemRepository,
                lineItemScoreRepository,
                courseAssessmentScoreRepository,
                courseEnrollmentRepository,
                assignmentRepository,
                quizRepository,
                lessonRepository
        );
    }

    @Test
    void upsertLineItemScoreAggregatesWeightedAverageAndFinalGrade() {
        UUID courseUuid = UUID.randomUUID();
        UUID assessmentUuid = UUID.randomUUID();
        UUID enrollmentUuid = UUID.randomUUID();
        UUID firstLineItemUuid = UUID.randomUUID();
        UUID secondLineItemUuid = UUID.randomUUID();

        CourseAssessment assessment = assessment(courseUuid, assessmentUuid, "100.00", CourseAssessmentAggregationStrategy.WEIGHTED_AVERAGE);
        CourseEnrollment enrollment = enrollment(courseUuid, enrollmentUuid);
        CourseAssessmentLineItem firstLineItem = manualLineItem(assessmentUuid, firstLineItemUuid, "Sight-reading quiz", "40.00");
        CourseAssessmentLineItem secondLineItem = manualLineItem(assessmentUuid, secondLineItemUuid, "Rhythm quiz", "60.00");

        Map<String, CourseAssessmentLineItemScore> lineItemScores = new HashMap<>();
        Map<UUID, CourseAssessmentScore> assessmentScores = new HashMap<>();

        stubWeightedAverageScenario(
                courseUuid,
                assessment,
                enrollment,
                List.of(firstLineItem, secondLineItem),
                lineItemScores,
                assessmentScores
        );

        service.upsertLineItemScore(
                courseUuid,
                assessmentUuid,
                firstLineItemUuid,
                enrollmentUuid,
                scoreDTO(firstLineItemUuid, enrollmentUuid, "80.00", "100.00")
        );
        service.upsertLineItemScore(
                courseUuid,
                assessmentUuid,
                secondLineItemUuid,
                enrollmentUuid,
                scoreDTO(secondLineItemUuid, enrollmentUuid, "90.00", "100.00")
        );

        CourseAssessmentScore aggregateScore = assessmentScores.get(assessmentUuid);
        assertThat(aggregateScore).isNotNull();
        assertThat(aggregateScore.getScore()).isEqualByComparingTo("86.00");
        assertThat(aggregateScore.getMaxScore()).isEqualByComparingTo("100.00");
        assertThat(aggregateScore.getPercentage()).isEqualByComparingTo("86.00");
        assertThat(enrollment.getFinalGrade()).isEqualByComparingTo("86.00");
    }

    @Test
    void syncAssignmentGradeCreatesDerivedLineItemScoreAndRecalculatesFinalGrade() {
        UUID courseUuid = UUID.randomUUID();
        UUID assessmentUuid = UUID.randomUUID();
        UUID enrollmentUuid = UUID.randomUUID();
        UUID assignmentUuid = UUID.randomUUID();
        UUID lineItemUuid = UUID.randomUUID();

        CourseAssessment assessment = assessment(courseUuid, assessmentUuid, "100.00", CourseAssessmentAggregationStrategy.POINTS_SUM);
        CourseEnrollment enrollment = enrollment(courseUuid, enrollmentUuid);
        CourseAssessmentLineItem lineItem = assignmentLineItem(assessmentUuid, lineItemUuid, assignmentUuid, "50.00");

        Map<String, CourseAssessmentLineItemScore> lineItemScores = new HashMap<>();
        Map<UUID, CourseAssessmentScore> assessmentScores = new HashMap<>();

        when(lineItemRepository.findByAssignmentUuid(assignmentUuid)).thenReturn(Optional.of(lineItem));
        when(courseAssessmentRepository.findByUuid(lineItem.getCourseAssessmentUuid())).thenReturn(Optional.of(assessment));
        when(courseEnrollmentRepository.findByUuid(enrollmentUuid)).thenReturn(Optional.of(enrollment));

        stubAssessmentAndEnrollmentLookups(courseUuid, assessment, enrollment);
        stubLineItemCollections(List.of(lineItem));
        stubStatefulScoreRepositories(enrollmentUuid, lineItemScores, assessmentScores);

        service.syncAssignmentGrade(
                assignmentUuid,
                enrollmentUuid,
                new BigDecimal("45.00"),
                new BigDecimal("50.00"),
                "Good work",
                LocalDateTime.now(),
                UUID.randomUUID()
        );

        CourseAssessmentLineItemScore lineItemScore = lineItemScores.get(scoreKey(lineItemUuid, enrollmentUuid));
        CourseAssessmentScore aggregateScore = assessmentScores.get(assessmentUuid);

        assertThat(lineItemScore).isNotNull();
        assertThat(lineItemScore.getPercentage()).isEqualByComparingTo("90.00");
        assertThat(aggregateScore).isNotNull();
        assertThat(aggregateScore.getScore()).isEqualByComparingTo("45.00");
        assertThat(aggregateScore.getMaxScore()).isEqualByComparingTo("50.00");
        assertThat(aggregateScore.getPercentage()).isEqualByComparingTo("90.00");
        assertThat(enrollment.getFinalGrade()).isEqualByComparingTo("90.00");
    }

    @Test
    void createLineItemRejectsMissingWeightForWeightedComponent() {
        UUID courseUuid = UUID.randomUUID();
        UUID assessmentUuid = UUID.randomUUID();
        CourseAssessment assessment = assessment(courseUuid, assessmentUuid, "25.00", CourseAssessmentAggregationStrategy.WEIGHTED_AVERAGE);

        when(courseAssessmentRepository.findByUuidAndCourseUuid(assessmentUuid, courseUuid)).thenReturn(Optional.of(assessment));
        when(lineItemRepository.findByCourseAssessmentUuidOrderByDisplayOrderAscCreatedDateAsc(assessmentUuid)).thenReturn(List.of());

        CourseAssessmentLineItemDTO lineItemDTO = new CourseAssessmentLineItemDTO(
                null,
                null,
                "Manual attendance",
                null,
                CourseAssessmentLineItemType.MANUAL,
                null,
                null,
                null,
                new BigDecimal("10.00"),
                null,
                1,
                true,
                null,
                null,
                null,
                null,
                null
        );

        assertThatThrownBy(() -> service.createLineItem(courseUuid, assessmentUuid, lineItemDTO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positive line item weight percentage");
    }

    @Test
    void createLineItemAllowsProjectCategoryLinkedToAssignment() {
        UUID courseUuid = UUID.randomUUID();
        UUID assessmentUuid = UUID.randomUUID();
        UUID assignmentUuid = UUID.randomUUID();
        UUID lessonUuid = UUID.randomUUID();

        CourseAssessment assessment = assessment(courseUuid, assessmentUuid, "40.00", CourseAssessmentAggregationStrategy.POINTS_SUM);
        Assignment assignment = new Assignment();
        assignment.setUuid(assignmentUuid);
        assignment.setLessonUuid(lessonUuid);

        Lesson lesson = new Lesson();
        lesson.setUuid(lessonUuid);
        lesson.setCourseUuid(courseUuid);

        when(courseAssessmentRepository.findByUuidAndCourseUuid(assessmentUuid, courseUuid)).thenReturn(Optional.of(assessment));
        when(lineItemRepository.findByCourseAssessmentUuidOrderByDisplayOrderAscCreatedDateAsc(assessmentUuid)).thenReturn(List.of());
        when(assignmentRepository.findByUuid(assignmentUuid)).thenReturn(Optional.of(assignment));
        when(lessonRepository.findByUuid(lessonUuid)).thenReturn(Optional.of(lesson));
        when(lineItemRepository.findByAssignmentUuid(assignmentUuid)).thenReturn(Optional.empty());
        when(lineItemRepository.save(any(CourseAssessmentLineItem.class)))
                .thenAnswer(invocation -> {
                    CourseAssessmentLineItem saved = invocation.getArgument(0);
                    saved.setUuid(UUID.randomUUID());
                    return saved;
                });
        lenient().when(courseEnrollmentRepository.findByCourseUuid(courseUuid)).thenReturn(List.of());

        CourseAssessmentLineItemDTO lineItemDTO = new CourseAssessmentLineItemDTO(
                null,
                null,
                "Capstone project",
                "Term-long implementation project",
                CourseAssessmentLineItemType.PROJECT,
                assignmentUuid,
                null,
                null,
                new BigDecimal("100.00"),
                null,
                null,
                true,
                null,
                null,
                null,
                null,
                null
        );

        CourseAssessmentLineItemDTO created = service.createLineItem(courseUuid, assessmentUuid, lineItemDTO);

        assertThat(created.itemType()).isEqualTo(CourseAssessmentLineItemType.PROJECT);
        assertThat(created.assignmentUuid()).isEqualTo(assignmentUuid);
        assertThat(created.quizUuid()).isNull();
        assertThat(created.displayOrder()).isEqualTo(1);
    }

    @Test
    void recalculateCourseAssessmentClearsDerivedScoreWhenNoLineItemsRemain() {
        UUID courseUuid = UUID.randomUUID();
        UUID assessmentUuid = UUID.randomUUID();
        UUID enrollmentUuid = UUID.randomUUID();

        CourseAssessment assessment = assessment(courseUuid, assessmentUuid, "100.00", CourseAssessmentAggregationStrategy.POINTS_SUM);
        CourseEnrollment enrollment = enrollment(courseUuid, enrollmentUuid);
        CourseAssessmentScore aggregateScore = new CourseAssessmentScore();
        aggregateScore.setUuid(UUID.randomUUID());
        aggregateScore.setEnrollmentUuid(enrollmentUuid);
        aggregateScore.setAssessmentUuid(assessmentUuid);
        aggregateScore.setScore(new BigDecimal("45.00"));
        aggregateScore.setMaxScore(new BigDecimal("50.00"));
        aggregateScore.setPercentage(new BigDecimal("90.00"));

        Map<UUID, CourseAssessmentScore> assessmentScores = new HashMap<>();
        assessmentScores.put(assessmentUuid, aggregateScore);

        when(courseAssessmentRepository.findByUuidAndCourseUuid(assessmentUuid, courseUuid)).thenReturn(Optional.of(assessment));
        when(courseAssessmentRepository.findByCourseUuidOrderByCreatedDateAsc(courseUuid)).thenReturn(List.of(assessment));
        when(courseEnrollmentRepository.findByCourseUuid(courseUuid)).thenReturn(List.of(enrollment));
        when(courseEnrollmentRepository.findByUuidAndCourseUuid(enrollmentUuid, courseUuid)).thenReturn(Optional.of(enrollment));
        when(lineItemRepository.findByCourseAssessmentUuidOrderByDisplayOrderAscCreatedDateAsc(assessmentUuid)).thenReturn(List.of());
        when(courseAssessmentScoreRepository.findByEnrollmentUuidAndAssessmentUuidIn(eq(enrollmentUuid), anyCollection()))
                .thenAnswer(invocation -> new ArrayList<>(assessmentScores.values()));
        when(courseAssessmentScoreRepository.save(any(CourseAssessmentScore.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(courseEnrollmentRepository.save(any(CourseEnrollment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(courseAssessmentScoreRepository.findByEnrollmentUuidAndAssessmentUuid(enrollmentUuid, assessmentUuid))
                .thenAnswer(invocation -> Optional.ofNullable(assessmentScores.get(assessmentUuid)));
        lenient().doAnswer(invocation -> {
            assessmentScores.remove(invocation.getArgument(1));
            return null;
        }).when(courseAssessmentScoreRepository).deleteByEnrollmentUuidAndAssessmentUuid(eq(enrollmentUuid), eq(assessmentUuid));

        service.recalculateCourseAssessment(courseUuid, assessmentUuid);

        assertThat(assessmentScores).doesNotContainKey(assessmentUuid);
        assertThat(enrollment.getFinalGrade()).isNull();
    }

    private void stubWeightedAverageScenario(
            UUID courseUuid,
            CourseAssessment assessment,
            CourseEnrollment enrollment,
            List<CourseAssessmentLineItem> lineItems,
            Map<String, CourseAssessmentLineItemScore> lineItemScores,
            Map<UUID, CourseAssessmentScore> assessmentScores
    ) {
        stubAssessmentAndEnrollmentLookups(courseUuid, assessment, enrollment);
        for (CourseAssessmentLineItem lineItem : lineItems) {
            lenient().when(lineItemRepository.findByUuidAndCourseAssessmentUuid(lineItem.getUuid(), assessment.getUuid()))
                    .thenReturn(Optional.of(lineItem));
        }
        stubLineItemCollections(lineItems);
        stubStatefulScoreRepositories(enrollment.getUuid(), lineItemScores, assessmentScores);
    }

    private void stubAssessmentAndEnrollmentLookups(UUID courseUuid, CourseAssessment assessment, CourseEnrollment enrollment) {
        lenient().when(courseAssessmentRepository.findByUuidAndCourseUuid(assessment.getUuid(), courseUuid)).thenReturn(Optional.of(assessment));
        lenient().when(courseAssessmentRepository.findByCourseUuidOrderByCreatedDateAsc(courseUuid)).thenReturn(List.of(assessment));
        lenient().when(courseEnrollmentRepository.findByUuidAndCourseUuid(enrollment.getUuid(), courseUuid)).thenReturn(Optional.of(enrollment));
        lenient().when(courseEnrollmentRepository.findByCourseUuid(courseUuid)).thenReturn(List.of(enrollment));
        lenient().when(courseEnrollmentRepository.save(any(CourseEnrollment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void stubLineItemCollections(List<CourseAssessmentLineItem> lineItems) {
        UUID assessmentUuid = lineItems.get(0).getCourseAssessmentUuid();
        lenient().when(lineItemRepository.findByCourseAssessmentUuidOrderByDisplayOrderAscCreatedDateAsc(assessmentUuid))
                .thenReturn(lineItems);
        lenient().when(lineItemRepository.findByCourseAssessmentUuidInOrderByDisplayOrderAscCreatedDateAsc(anyCollection()))
                .thenReturn(lineItems);
    }

    private void stubStatefulScoreRepositories(
            UUID enrollmentUuid,
            Map<String, CourseAssessmentLineItemScore> lineItemScores,
            Map<UUID, CourseAssessmentScore> assessmentScores
    ) {
        lenient().when(lineItemScoreRepository.findByLineItemUuidAndEnrollmentUuid(any(UUID.class), eq(enrollmentUuid)))
                .thenAnswer(invocation -> Optional.ofNullable(
                        lineItemScores.get(scoreKey(invocation.getArgument(0), enrollmentUuid))
                ));
        lenient().when(lineItemScoreRepository.findByEnrollmentUuidAndLineItemUuidIn(eq(enrollmentUuid), anyCollection()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    List<UUID> lineItemUuids = new ArrayList<>((java.util.Collection<UUID>) invocation.getArgument(1));
                    return lineItemScores.values().stream()
                            .filter(score -> enrollmentUuid.equals(score.getEnrollmentUuid()))
                            .filter(score -> lineItemUuids.contains(score.getLineItemUuid()))
                            .toList();
                });
        lenient().when(lineItemScoreRepository.save(any(CourseAssessmentLineItemScore.class)))
                .thenAnswer(invocation -> {
                    CourseAssessmentLineItemScore score = invocation.getArgument(0);
                    if (score.getUuid() == null) {
                        score.setUuid(UUID.randomUUID());
                    }
                    lineItemScores.put(scoreKey(score.getLineItemUuid(), score.getEnrollmentUuid()), score);
                    return score;
                });

        lenient().when(courseAssessmentScoreRepository.findByEnrollmentUuidAndAssessmentUuid(eq(enrollmentUuid), any(UUID.class)))
                .thenAnswer(invocation -> Optional.ofNullable(assessmentScores.get(invocation.getArgument(1))));
        lenient().when(courseAssessmentScoreRepository.findByEnrollmentUuidAndAssessmentUuidIn(eq(enrollmentUuid), anyCollection()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    List<UUID> assessmentUuids = new ArrayList<>((java.util.Collection<UUID>) invocation.getArgument(1));
                    return assessmentScores.values().stream()
                            .filter(score -> enrollmentUuid.equals(score.getEnrollmentUuid()))
                            .filter(score -> assessmentUuids.contains(score.getAssessmentUuid()))
                            .toList();
                });
        lenient().when(courseAssessmentScoreRepository.save(any(CourseAssessmentScore.class)))
                .thenAnswer(invocation -> {
                    CourseAssessmentScore score = invocation.getArgument(0);
                    if (score.getUuid() == null) {
                        score.setUuid(UUID.randomUUID());
                    }
                    assessmentScores.put(score.getAssessmentUuid(), score);
                    return score;
                });
        lenient().doAnswer(invocation -> {
            assessmentScores.remove(invocation.getArgument(1));
            return null;
        }).when(courseAssessmentScoreRepository).deleteByEnrollmentUuidAndAssessmentUuid(eq(enrollmentUuid), any(UUID.class));
    }

    private CourseAssessment assessment(
            UUID courseUuid,
            UUID assessmentUuid,
            String weightPercentage,
            CourseAssessmentAggregationStrategy strategy
    ) {
        CourseAssessment assessment = new CourseAssessment();
        assessment.setUuid(assessmentUuid);
        assessment.setCourseUuid(courseUuid);
        assessment.setAssessmentType("Quiz");
        assessment.setTitle("Weekly Quizzes");
        assessment.setWeightPercentage(new BigDecimal(weightPercentage));
        assessment.setAggregationStrategy(strategy);
        assessment.setIsRequired(true);
        return assessment;
    }

    private CourseEnrollment enrollment(UUID courseUuid, UUID enrollmentUuid) {
        CourseEnrollment enrollment = new CourseEnrollment();
        enrollment.setUuid(enrollmentUuid);
        enrollment.setCourseUuid(courseUuid);
        return enrollment;
    }

    private CourseAssessmentLineItem manualLineItem(
            UUID assessmentUuid,
            UUID lineItemUuid,
            String title,
            String weightPercentage
    ) {
        CourseAssessmentLineItem lineItem = new CourseAssessmentLineItem();
        lineItem.setUuid(lineItemUuid);
        lineItem.setCourseAssessmentUuid(assessmentUuid);
        lineItem.setTitle(title);
        lineItem.setItemType(CourseAssessmentLineItemType.MANUAL);
        lineItem.setWeightPercentage(new BigDecimal(weightPercentage));
        lineItem.setDisplayOrder(1);
        lineItem.setActive(true);
        lineItem.setMaxScore(new BigDecimal("100.00"));
        return lineItem;
    }

    private CourseAssessmentLineItem assignmentLineItem(
            UUID assessmentUuid,
            UUID lineItemUuid,
            UUID assignmentUuid,
            String maxScore
    ) {
        CourseAssessmentLineItem lineItem = new CourseAssessmentLineItem();
        lineItem.setUuid(lineItemUuid);
        lineItem.setCourseAssessmentUuid(assessmentUuid);
        lineItem.setTitle("Technique assignment");
        lineItem.setItemType(CourseAssessmentLineItemType.ASSIGNMENT);
        lineItem.setAssignmentUuid(assignmentUuid);
        lineItem.setDisplayOrder(1);
        lineItem.setActive(true);
        lineItem.setMaxScore(new BigDecimal(maxScore));
        return lineItem;
    }

    private CourseAssessmentLineItemScoreDTO scoreDTO(UUID lineItemUuid, UUID enrollmentUuid, String score, String maxScore) {
        return new CourseAssessmentLineItemScoreDTO(
                null,
                lineItemUuid,
                enrollmentUuid,
                new BigDecimal(score),
                new BigDecimal(maxScore),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private String scoreKey(UUID lineItemUuid, UUID enrollmentUuid) {
        return lineItemUuid + ":" + enrollmentUuid;
    }
}
