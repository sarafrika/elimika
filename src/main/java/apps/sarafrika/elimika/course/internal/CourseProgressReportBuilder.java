package apps.sarafrika.elimika.course.internal;

import apps.sarafrika.elimika.course.dto.CourseAssessmentGradeRevisionDTO;
import apps.sarafrika.elimika.course.dto.CourseGradeBookDTO;
import apps.sarafrika.elimika.course.dto.CourseProgressReportDTO;
import apps.sarafrika.elimika.course.model.Assignment;
import apps.sarafrika.elimika.course.model.CourseAssessment;
import apps.sarafrika.elimika.course.model.CourseAssessmentGradeRevision;
import apps.sarafrika.elimika.course.model.CourseAssessmentLineItem;
import apps.sarafrika.elimika.course.model.CourseAssessmentLineItemRubricEvaluation;
import apps.sarafrika.elimika.course.model.CourseAssessmentLineItemScore;
import apps.sarafrika.elimika.course.model.CourseEnrollment;
import apps.sarafrika.elimika.course.model.Lesson;
import apps.sarafrika.elimika.course.repository.AssignmentRepository;
import apps.sarafrika.elimika.course.repository.CourseAssessmentGradeRevisionRepository;
import apps.sarafrika.elimika.course.repository.CourseAssessmentLineItemRepository;
import apps.sarafrika.elimika.course.repository.CourseAssessmentLineItemRubricEvaluationRepository;
import apps.sarafrika.elimika.course.repository.CourseAssessmentLineItemScoreRepository;
import apps.sarafrika.elimika.course.repository.CourseAssessmentRepository;
import apps.sarafrika.elimika.course.repository.CourseEnrollmentRepository;
import apps.sarafrika.elimika.course.repository.CourseRepository;
import apps.sarafrika.elimika.course.repository.LessonRepository;
import apps.sarafrika.elimika.course.service.CourseGradeBookService;
import apps.sarafrika.elimika.course.util.enums.CourseAssessmentLineItemRubricEvaluationStatus;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.spi.enrollment.EnrollmentLookupService;
import apps.sarafrika.elimika.shared.spi.enrollment.EnrollmentLookupService.LearnerSessionSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Builds the per-learner progress report from the gradebook, so the report, the API and any export
 * show the same final grade. Lesson rows add what was taught and how the learner attended.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseProgressReportBuilder {

    private final CourseGradeBookService gradeBookService;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;
    private final AssignmentRepository assignmentRepository;
    private final CourseAssessmentRepository assessmentRepository;
    private final CourseAssessmentLineItemRepository lineItemRepository;
    private final CourseAssessmentLineItemScoreRepository lineItemScoreRepository;
    private final CourseAssessmentLineItemRubricEvaluationRepository rubricEvaluationRepository;
    private final CourseAssessmentGradeRevisionRepository revisionRepository;
    private final EnrollmentLookupService enrollmentLookupService;

    public CourseProgressReportDTO build(UUID courseUuid, UUID enrollmentUuid) {
        CourseEnrollment enrollment = enrollmentRepository.findByUuidAndCourseUuid(enrollmentUuid, courseUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Course enrollment " + enrollmentUuid + " not found"));
        CourseGradeBookDTO gradebook = gradeBookService.getEnrollmentGradeBook(courseUuid, enrollmentUuid);

        Map<UUID, CourseAssessment> assessments = assessmentRepository.findByCourseUuidOrderByCreatedDateAsc(courseUuid)
                .stream()
                .filter(assessment -> !Boolean.FALSE.equals(assessment.getActive()))
                .collect(Collectors.toMap(CourseAssessment::getUuid, Function.identity()));
        List<CourseAssessmentLineItem> lessonItems = assessments.isEmpty() ? List.of()
                : lineItemRepository.findByCourseAssessmentUuidInOrderByDisplayOrderAscCreatedDateAsc(assessments.keySet())
                .stream()
                .filter(item -> !Boolean.FALSE.equals(item.getActive()) && item.getLessonUuid() != null)
                .toList();
        List<UUID> itemUuids = lessonItems.stream().map(CourseAssessmentLineItem::getUuid).toList();
        Map<UUID, CourseAssessmentLineItemScore> scores = itemUuids.isEmpty() ? Map.of()
                : lineItemScoreRepository.findByEnrollmentUuidAndLineItemUuidIn(enrollmentUuid, itemUuids).stream()
                .collect(Collectors.toMap(CourseAssessmentLineItemScore::getLineItemUuid, Function.identity()));
        Map<UUID, CourseAssessmentLineItemRubricEvaluation> evaluations = itemUuids.isEmpty() ? Map.of()
                : rubricEvaluationRepository.findByEnrollmentUuidAndLineItemUuidIn(enrollmentUuid, itemUuids).stream()
                .collect(Collectors.toMap(CourseAssessmentLineItemRubricEvaluation::getLineItemUuid, Function.identity()));
        Map<UUID, List<CourseAssessmentLineItem>> itemsByLesson = lessonItems.stream()
                .collect(Collectors.groupingBy(CourseAssessmentLineItem::getLessonUuid));
        // The latest session for each lesson carries what was taught and the learner's attendance.
        Map<UUID, LearnerSessionSnapshot> sessionByLesson = enrollmentLookupService
                .findSessionsOfStudentForCourse(enrollment.getStudentUuid(), courseUuid).stream()
                .filter(session -> session.lessonUuid() != null)
                .collect(Collectors.toMap(LearnerSessionSnapshot::lessonUuid, Function.identity(), (older, newer) -> newer));

        List<CourseProgressReportDTO.LessonRowDTO> rows = lessonRepository.findByCourseUuidOrderByLessonNumberAsc(courseUuid)
                .stream()
                .filter(lesson -> !Boolean.FALSE.equals(lesson.getActive()))
                .map(lesson -> lessonRow(lesson, sessionByLesson.get(lesson.getUuid()),
                        itemsByLesson.getOrDefault(lesson.getUuid(), List.of()), assessments, scores, evaluations, enrollmentUuid))
                .toList();

        CourseGradeBookDTO.ComponentDTO lessonComponent = gradebook.components().stream()
                .filter(component -> Boolean.TRUE.equals(component.assessment().perLesson()))
                .findFirst()
                .orElse(null);
        return new CourseProgressReportDTO(courseUuid,
                courseRepository.findByUuid(courseUuid).map(course -> course.getName()).orElse(null),
                enrollmentUuid, enrollment.getStudentUuid(), gradebook.finalGrade(), gradebook.finalGradeLabel(),
                gradebook.resultStatus(), gradebook.passMark(), enrollment.getProgressPercentage(),
                lessonComponent != null ? lessonComponent.averageLineItemScore() : null,
                lessonComponent != null ? lessonComponent.lineItemMaxScore() : null,
                gradebook.instructorComments(), rows);
    }

    private CourseProgressReportDTO.LessonRowDTO lessonRow(
            Lesson lesson,
            LearnerSessionSnapshot session,
            List<CourseAssessmentLineItem> items,
            Map<UUID, CourseAssessment> assessments,
            Map<UUID, CourseAssessmentLineItemScore> scores,
            Map<UUID, CourseAssessmentLineItemRubricEvaluation> evaluations,
            UUID enrollmentUuid
    ) {
        List<CourseProgressReportDTO.MarkDTO> marks = items.stream()
                .sorted(Comparator.comparing(CourseAssessmentLineItem::getDisplayOrder, Comparator.nullsLast(Integer::compareTo)))
                .map(item -> mark(item, assessments.get(item.getCourseAssessmentUuid()), scores.get(item.getUuid()),
                        evaluations.get(item.getUuid()), enrollmentUuid))
                .toList();
        List<String> assignments = assignmentRepository.findByLessonUuid(lesson.getUuid()).stream()
                .map(Assignment::getTitle)
                .filter(Objects::nonNull)
                .toList();
        String attendance = session == null ? null : switch (Optional.ofNullable(session.attendanceStatus()).orElse("")) {
            case "ATTENDED", "attended" -> "ATTENDED";
            case "ABSENT", "absent" -> "ABSENT";
            default -> null;
        };
        Long minutes = session != null && session.startTime() != null && session.endTime() != null
                ? Duration.between(session.startTime(), session.endTime()).toMinutes() : null;
        return new CourseProgressReportDTO.LessonRowDTO(lesson.getUuid(), lesson.getLessonNumber(), lesson.getTitle(),
                session != null ? session.topicCovered() : null, session != null ? session.instructorRemarks() : null,
                assignments, session != null ? session.startTime() : null, minutes, attendance,
                session != null ? session.instrumentCondition() : null, marks);
    }

    private CourseProgressReportDTO.MarkDTO mark(
            CourseAssessmentLineItem item,
            CourseAssessment assessment,
            CourseAssessmentLineItemScore score,
            CourseAssessmentLineItemRubricEvaluation evaluation,
            UUID enrollmentUuid
    ) {
        String component = assessment != null ? assessment.getTitle() : item.getTitle();
        int revisionCount = revisionRepository.findLatestRevisionNumber(item.getUuid(), enrollmentUuid);
        boolean draft = evaluation != null && evaluation.getStatus() == CourseAssessmentLineItemRubricEvaluationStatus.PENDING
                && (score == null || score.getScore() == null);
        if (score == null || score.getScore() == null) {
            return new CourseProgressReportDTO.MarkDTO(item.getUuid(), component, draft ? "draft" : "ungraded",
                    null, item.getMaxScore(), null, List.of(), revisionCount, null);
        }
        List<CourseAssessmentGradeRevisionDTO.CriterionDTO> criteria = latestCriteria(item.getUuid(), enrollmentUuid);
        return new CourseProgressReportDTO.MarkDTO(item.getUuid(), component, "graded", score.getScore(),
                score.getMaxScore(), score.getComments(), criteria, revisionCount, score.getGradedAt());
    }

    private List<CourseAssessmentGradeRevisionDTO.CriterionDTO> latestCriteria(UUID lineItemUuid, UUID enrollmentUuid) {
        List<CourseAssessmentGradeRevision> revisions = revisionRepository
                .findByLineItemUuidAndEnrollmentUuidOrderByRevisionNumberAsc(lineItemUuid, enrollmentUuid);
        if (revisions.isEmpty()) {
            return List.of();
        }
        List<CourseAssessmentGradeRevisionDTO.CriterionDTO> criteria = CourseAssessmentGradeRevisionDTO
                .from(revisions.getLast()).criteria();
        return criteria == null ? List.of() : criteria;
    }
}
