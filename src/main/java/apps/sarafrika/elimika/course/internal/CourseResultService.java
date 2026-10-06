package apps.sarafrika.elimika.course.internal;

import apps.sarafrika.elimika.course.model.CourseAssessment;
import apps.sarafrika.elimika.course.model.CourseAssessmentLineItem;
import apps.sarafrika.elimika.course.model.CourseAssessmentLineItemRubricEvaluation;
import apps.sarafrika.elimika.course.model.CourseAssessmentLineItemScore;
import apps.sarafrika.elimika.course.model.CourseEnrollment;
import apps.sarafrika.elimika.course.repository.CourseAssessmentLineItemRepository;
import apps.sarafrika.elimika.course.repository.CourseAssessmentLineItemRubricEvaluationRepository;
import apps.sarafrika.elimika.course.repository.CourseAssessmentLineItemScoreRepository;
import apps.sarafrika.elimika.course.repository.CourseEnrollmentRepository;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.course.util.enums.CourseAssessmentLineItemRubricEvaluationStatus;
import apps.sarafrika.elimika.course.util.enums.CourseResultStatus;
import apps.sarafrika.elimika.course.util.enums.EnrollmentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Decides a course result once every required item is graded; a pass completes the enrolment. */
@Component
@RequiredArgsConstructor
@Slf4j
public class CourseResultService {

    private final CourseRepository courseRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final CourseAssessmentLineItemRepository lineItemRepository;
    private final CourseAssessmentLineItemScoreRepository lineItemScoreRepository;
    private final CourseAssessmentLineItemRubricEvaluationRepository rubricEvaluationRepository;
    private final ApplicationEventPublisher eventPublisher;

    public CourseResultStatus decide(CourseEnrollment enrollment, List<CourseAssessment> activeAssessments) {
        CourseResultStatus previous = enrollment.getResultStatus();
        CourseResultStatus result = resultFor(enrollment, activeAssessments);
        if (result == previous) {
            return result;
        }

        enrollment.setResultStatus(result);
        enrollment.setResultDecidedAt(result == CourseResultStatus.IN_PROGRESS ? null : now());
        if (result == CourseResultStatus.PASSED) {
            enrollment.setStatus(EnrollmentStatus.COMPLETED);
            if (enrollment.getCompletionDate() == null) {
                enrollment.setCompletionDate(now());
            }
        } else if (enrollment.getStatus() == EnrollmentStatus.COMPLETED) {
            // A regrade that undoes a pass also undoes the completion it granted.
            enrollment.setStatus(EnrollmentStatus.ACTIVE);
            enrollment.setCompletionDate(null);
        }
        courseEnrollmentRepository.save(enrollment);
        log.info("Course enrolment {} result is now {}", enrollment.getUuid(), result);
        eventPublisher.publishEvent(new CourseResultDecidedEvent(enrollment.getUuid(), enrollment.getStudentUuid(),
                enrollment.getCourseUuid(), result));
        return result;
    }

    public BigDecimal passMarkOf(UUID courseUuid) {
        return courseRepository.findByUuid(courseUuid).map(course -> course.getPassMark()).orElse(null);
    }

    private CourseResultStatus resultFor(CourseEnrollment enrollment, List<CourseAssessment> activeAssessments) {
        if (activeAssessments.isEmpty() || !allRequiredItemsGraded(enrollment.getUuid(), activeAssessments)) {
            return CourseResultStatus.IN_PROGRESS;
        }
        BigDecimal passMark = passMarkOf(enrollment.getCourseUuid());
        if (passMark == null) {
            return CourseResultStatus.PASSED;
        }
        BigDecimal finalGrade = enrollment.getFinalGrade();
        return finalGrade != null && finalGrade.compareTo(passMark) >= 0 ? CourseResultStatus.PASSED : CourseResultStatus.FAILED;
    }

    private boolean allRequiredItemsGraded(UUID enrollmentUuid, List<CourseAssessment> activeAssessments) {
        List<UUID> requiredAssessments = activeAssessments.stream()
                .filter(assessment -> Boolean.TRUE.equals(assessment.getIsRequired()))
                .map(CourseAssessment::getUuid)
                .toList();
        if (requiredAssessments.isEmpty()) {
            return true;
        }
        List<UUID> requiredItems = lineItemRepository.findByCourseAssessmentUuidInOrderByDisplayOrderAscCreatedDateAsc(requiredAssessments)
                .stream()
                .filter(item -> !Boolean.FALSE.equals(item.getActive()))
                .map(CourseAssessmentLineItem::getUuid)
                .toList();
        if (requiredItems.isEmpty()) {
            return false;
        }
        Set<UUID> graded = lineItemScoreRepository.findByEnrollmentUuidAndLineItemUuidIn(enrollmentUuid, requiredItems)
                .stream()
                .filter(score -> score.getPercentage() != null)
                .map(CourseAssessmentLineItemScore::getLineItemUuid)
                .collect(Collectors.toSet());
        rubricEvaluationRepository.findByEnrollmentUuidAndLineItemUuidIn(enrollmentUuid, requiredItems).stream()
                .filter(evaluation -> evaluation.getStatus() == CourseAssessmentLineItemRubricEvaluationStatus.COMPLETED)
                .map(CourseAssessmentLineItemRubricEvaluation::getLineItemUuid)
                .forEach(graded::add);
        return graded.containsAll(requiredItems);
    }

    private static LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
