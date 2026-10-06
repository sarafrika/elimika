package apps.sarafrika.elimika.course.internal;

import apps.sarafrika.elimika.course.model.Course;
import apps.sarafrika.elimika.course.model.CourseAssessment;
import apps.sarafrika.elimika.course.model.CourseAssessmentLineItem;
import apps.sarafrika.elimika.course.model.CourseAssessmentLineItemScore;
import apps.sarafrika.elimika.course.model.CourseEnrollment;
import apps.sarafrika.elimika.course.repository.CourseAssessmentLineItemRepository;
import apps.sarafrika.elimika.course.repository.CourseAssessmentLineItemRubricEvaluationRepository;
import apps.sarafrika.elimika.course.repository.CourseAssessmentLineItemScoreRepository;
import apps.sarafrika.elimika.course.repository.CourseEnrollmentRepository;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.course.util.enums.CourseResultStatus;
import apps.sarafrika.elimika.course.util.enums.EnrollmentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CourseResultServiceTest {

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private CourseEnrollmentRepository courseEnrollmentRepository;
    @Mock
    private CourseAssessmentLineItemRepository lineItemRepository;
    @Mock
    private CourseAssessmentLineItemScoreRepository lineItemScoreRepository;
    @Mock
    private CourseAssessmentLineItemRubricEvaluationRepository rubricEvaluationRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CourseResultService service;

    private final UUID courseUuid = UUID.randomUUID();
    private final UUID enrollmentUuid = UUID.randomUUID();
    private final UUID lineItemUuid = UUID.randomUUID();
    private CourseEnrollment enrollment;
    private CourseAssessment component;

    @BeforeEach
    void setUp() {
        enrollment = new CourseEnrollment();
        enrollment.setUuid(enrollmentUuid);
        enrollment.setCourseUuid(courseUuid);
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        component = new CourseAssessment();
        component.setUuid(UUID.randomUUID());
        component.setIsRequired(true);
        CourseAssessmentLineItem item = new CourseAssessmentLineItem();
        item.setUuid(lineItemUuid);
        item.setActive(true);
        when(lineItemRepository.findByCourseAssessmentUuidInOrderByDisplayOrderAscCreatedDateAsc(anyCollection()))
                .thenReturn(List.of(item));
        when(rubricEvaluationRepository.findByEnrollmentUuidAndLineItemUuidIn(eq(enrollmentUuid), anyCollection()))
                .thenReturn(List.of());
        passMark(new BigDecimal("80"));
    }

    @Test
    void staysInProgressUntilEveryRequiredItemIsGraded() {
        when(lineItemScoreRepository.findByEnrollmentUuidAndLineItemUuidIn(eq(enrollmentUuid), anyCollection()))
                .thenReturn(List.of());
        enrollment.setFinalGrade(new BigDecimal("95"));

        assertThat(service.decide(enrollment, List.of(component))).isEqualTo(CourseResultStatus.IN_PROGRESS);
        verify(courseEnrollmentRepository, never()).save(any());
        verify(eventPublisher).publishEvent(any(CourseGradeRecalculatedEvent.class));
    }

    @Test
    void passingTheMarkCompletesTheEnrolment() {
        graded();
        enrollment.setFinalGrade(new BigDecimal("80.00"));

        assertThat(service.decide(enrollment, List.of(component))).isEqualTo(CourseResultStatus.PASSED);
        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.COMPLETED);
        assertThat(enrollment.getCompletionDate()).isNotNull();
        verify(eventPublisher).publishEvent(any(CourseGradeRecalculatedEvent.class));
    }

    @Test
    void fallingShortFailsAndUndoesAnEarlierCompletion() {
        graded();
        enrollment.setFinalGrade(new BigDecimal("79.99"));
        enrollment.setStatus(EnrollmentStatus.COMPLETED);
        enrollment.setResultStatus(CourseResultStatus.PASSED);

        assertThat(service.decide(enrollment, List.of(component))).isEqualTo(CourseResultStatus.FAILED);
        assertThat(enrollment.getStatus()).isEqualTo(EnrollmentStatus.ACTIVE);
        assertThat(enrollment.getCompletionDate()).isNull();
    }

    @Test
    void withoutAPassMarkCompletingEveryItemPasses() {
        passMark(null);
        graded();
        enrollment.setFinalGrade(new BigDecimal("40"));

        assertThat(service.decide(enrollment, List.of(component))).isEqualTo(CourseResultStatus.PASSED);
    }

    private void graded() {
        CourseAssessmentLineItemScore score = new CourseAssessmentLineItemScore();
        score.setLineItemUuid(lineItemUuid);
        score.setPercentage(new BigDecimal("90"));
        when(lineItemScoreRepository.findByEnrollmentUuidAndLineItemUuidIn(eq(enrollmentUuid), anyCollection()))
                .thenReturn(List.of(score));
    }

    private void passMark(BigDecimal passMark) {
        Course course = new Course();
        course.setUuid(courseUuid);
        course.setPassMark(passMark);
        when(courseRepository.findByUuid(courseUuid)).thenReturn(Optional.of(course));
    }
}
