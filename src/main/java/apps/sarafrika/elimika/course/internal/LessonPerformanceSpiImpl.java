package apps.sarafrika.elimika.course.internal;

import apps.sarafrika.elimika.course.dto.CourseAssessmentLineItemRubricEvaluationDTO;
import apps.sarafrika.elimika.course.dto.CourseAssessmentLineItemRubricEvaluationRowDTO;
import apps.sarafrika.elimika.course.model.Assignment;
import apps.sarafrika.elimika.course.model.AssignmentSubmission;
import apps.sarafrika.elimika.course.model.CourseAssessment;
import apps.sarafrika.elimika.course.model.CourseAssessmentLineItem;
import apps.sarafrika.elimika.course.model.CourseAssessmentLineItemRubricEvaluation;
import apps.sarafrika.elimika.course.model.CourseAssessmentLineItemRubricEvaluationRow;
import apps.sarafrika.elimika.course.model.CourseEnrollment;
import apps.sarafrika.elimika.course.model.Lesson;
import apps.sarafrika.elimika.course.model.RubricCriteria;
import apps.sarafrika.elimika.course.model.RubricScoringLevel;
import apps.sarafrika.elimika.course.repository.AssignmentRepository;
import apps.sarafrika.elimika.course.repository.AssignmentSubmissionRepository;
import apps.sarafrika.elimika.course.repository.CourseAssessmentGradeRevisionRepository;
import apps.sarafrika.elimika.course.repository.CourseAssessmentLineItemRepository;
import apps.sarafrika.elimika.course.repository.CourseAssessmentLineItemRubricEvaluationRepository;
import apps.sarafrika.elimika.course.repository.CourseAssessmentLineItemRubricEvaluationRowRepository;
import apps.sarafrika.elimika.course.repository.CourseAssessmentRepository;
import apps.sarafrika.elimika.course.repository.CourseEnrollmentRepository;
import apps.sarafrika.elimika.course.repository.CourseGradeBandRepository;
import apps.sarafrika.elimika.course.repository.LessonRepository;
import apps.sarafrika.elimika.course.repository.RubricCriteriaRepository;
import apps.sarafrika.elimika.course.repository.RubricScoringLevelRepository;
import apps.sarafrika.elimika.course.service.CourseGradeBookService;
import apps.sarafrika.elimika.course.spi.LessonPerformanceSpi;
import apps.sarafrika.elimika.course.util.enums.CourseAssessmentLineItemRubricEvaluationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Reads one lesson's marks for a session report from the gradebook; owns nothing new. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LessonPerformanceSpiImpl implements LessonPerformanceSpi {

    private final LessonRepository lessonRepository;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final CourseAssessmentRepository assessmentRepository;
    private final CourseAssessmentLineItemRepository lineItemRepository;
    private final CourseAssessmentLineItemRubricEvaluationRepository evaluationRepository;
    private final CourseAssessmentLineItemRubricEvaluationRowRepository evaluationRowRepository;
    private final RubricCriteriaRepository criteriaRepository;
    private final RubricScoringLevelRepository levelRepository;
    private final CourseGradeBandRepository bandRepository;
    private final CourseAssessmentGradeRevisionRepository revisionRepository;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository submissionRepository;
    private final CourseGradeBookService gradeBookService;

    @Override
    public Optional<LessonRef> resolveLesson(UUID courseUuid, UUID lessonUuid, Integer sessionNumber) {
        if (courseUuid == null) {
            return Optional.empty();
        }
        List<Lesson> lessons = lessonRepository.findByCourseUuidOrderByLessonNumberAsc(courseUuid).stream()
                .filter(lesson -> !Boolean.FALSE.equals(lesson.getActive()))
                .toList();
        Optional<Lesson> named = lessonUuid == null ? Optional.empty()
                : lessons.stream().filter(lesson -> lessonUuid.equals(lesson.getUuid())).findFirst();
        Optional<Lesson> lesson = named.isPresent() ? named
                : sessionNumber != null && sessionNumber >= 1 && sessionNumber <= lessons.size()
                ? Optional.of(lessons.get(sessionNumber - 1)) : Optional.empty();
        return lesson.map(l -> new LessonRef(l.getUuid(), l.getLessonNumber(), l.getTitle()));
    }

    @Override
    public LessonPerformance findLessonPerformance(UUID courseUuid, UUID lessonUuid, Collection<UUID> studentUuids) {
        LessonRef lesson = (lessonUuid == null ? Optional.<Lesson>empty() : lessonRepository.findByUuid(lessonUuid))
                .map(l -> new LessonRef(l.getUuid(), l.getLessonNumber(), l.getTitle()))
                .orElse(new LessonRef(lessonUuid, null, null));
        Optional<Cell> cell = lessonCell(courseUuid, lessonUuid);
        List<Criterion> criteria = cell.map(c -> criteriaRepository.findByRubricUuidOrderByDisplayOrderAsc(c.rubricUuid()).stream()
                        .map(rc -> new Criterion(rc.getUuid(), rc.getComponentName(), rc.getDisplayOrder()))
                        .toList())
                .orElse(List.of());
        BigDecimal maxScore = cell.map(c -> maxScore(c.rubricUuid(), criteria.size())).orElse(null);
        List<Band> bands = bandRepository.findByCourseUuidOrderByMinPercentageDesc(courseUuid).stream()
                .map(band -> new Band(band.getName(), band.getMinPercentage()))
                .toList();
        List<Assignment> assignments = lessonUuid == null ? List.of() : assignmentRepository.findByLessonUuid(lessonUuid);

        List<StudentLesson> students = new ArrayList<>();
        for (UUID studentUuid : studentUuids) {
            Optional<CourseEnrollment> enrollment = enrollmentRepository.findByStudentUuidAndCourseUuid(studentUuid, courseUuid);
            if (enrollment.isEmpty()) {
                students.add(new StudentLesson(studentUuid, null, "none", null, null, List.of(), null, 0, null, List.of()));
                continue;
            }
            UUID enrollmentUuid = enrollment.get().getUuid();
            List<AssignmentStatus> assignmentStatuses = assignments.stream()
                    .map(assignment -> submissionRepository.findByEnrollmentUuidAndAssignmentUuid(enrollmentUuid, assignment.getUuid())
                            .map(submission -> new AssignmentStatus(assignment.getTitle(), statusOf(submission),
                                    submission.getScore(), submission.getMaxScore()))
                            .orElse(new AssignmentStatus(assignment.getTitle(), "not_submitted", null, assignment.getMaxPoints())))
                    .toList();
            students.add(cell.map(c -> studentLesson(studentUuid, enrollmentUuid, c, assignmentStatuses))
                    .orElse(new StudentLesson(studentUuid, enrollmentUuid, "none", null, null, List.of(), null, 0, null,
                            assignmentStatuses)));
        }
        return new LessonPerformance(lesson, cell.isPresent(), maxScore, criteria, bands,
                assignments.stream().map(Assignment::getTitle).filter(Objects::nonNull).toList(), students);
    }

    @Override
    @Transactional
    public int submitLessonDrafts(UUID courseUuid, UUID lessonUuid, Collection<UUID> studentUuids, UUID graderUserUuid) {
        Optional<Cell> cell = lessonCell(courseUuid, lessonUuid);
        if (cell.isEmpty()) {
            return 0;
        }
        Cell c = cell.get();
        int criteriaCount = criteriaRepository.findByRubricUuidOrderByDisplayOrderAsc(c.rubricUuid()).size();
        int submitted = 0;
        for (UUID studentUuid : studentUuids) {
            Optional<CourseEnrollment> enrollment = enrollmentRepository.findByStudentUuidAndCourseUuid(studentUuid, courseUuid);
            if (enrollment.isEmpty()) {
                continue;
            }
            Optional<CourseAssessmentLineItemRubricEvaluation> draft = evaluationRepository
                    .findByLineItemUuidAndEnrollmentUuid(c.lineItem().getUuid(), enrollment.get().getUuid())
                    .filter(e -> e.getStatus() == CourseAssessmentLineItemRubricEvaluationStatus.PENDING);
            if (draft.isEmpty()) {
                continue;
            }
            List<CourseAssessmentLineItemRubricEvaluationRow> rows = evaluationRowRepository.findByEvaluationUuid(draft.get().getUuid());
            if (rows.size() < criteriaCount) {
                continue;
            }
            List<CourseAssessmentLineItemRubricEvaluationRowDTO> selections = rows.stream()
                    .map(row -> new CourseAssessmentLineItemRubricEvaluationRowDTO(null, row.getCriteriaUuid(), null,
                            row.getScoringLevelUuid(), null, null, row.getComments()))
                    .toList();
            gradeBookService.upsertLineItemRubricEvaluation(courseUuid, c.assessment().getUuid(), c.lineItem().getUuid(),
                    enrollment.get().getUuid(), new CourseAssessmentLineItemRubricEvaluationDTO(null, null, null, null,
                            CourseAssessmentLineItemRubricEvaluationStatus.COMPLETED, null, null, null, null,
                            draft.get().getComments(), LocalDateTime.now(ZoneOffset.UTC), graderUserUuid, selections));
            submitted++;
        }
        return submitted;
    }

    private StudentLesson studentLesson(UUID studentUuid, UUID enrollmentUuid, Cell cell, List<AssignmentStatus> assignments) {
        Optional<CourseAssessmentLineItemRubricEvaluation> evaluation = evaluationRepository
                .findByLineItemUuidAndEnrollmentUuid(cell.lineItem().getUuid(), enrollmentUuid);
        int revisions = revisionRepository.findLatestRevisionNumber(cell.lineItem().getUuid(), enrollmentUuid);
        if (evaluation.isEmpty()) {
            return new StudentLesson(studentUuid, enrollmentUuid, "none", null, null, List.of(), null, revisions, null, assignments);
        }
        CourseAssessmentLineItemRubricEvaluation e = evaluation.get();
        Map<UUID, RubricCriteria> criteria = criteriaRepository.findByRubricUuidOrderByDisplayOrderAsc(cell.rubricUuid()).stream()
                .collect(Collectors.toMap(RubricCriteria::getUuid, Function.identity()));
        Map<UUID, RubricScoringLevel> levels = levelRepository.findByRubricUuidOrderByLevelOrder(cell.rubricUuid()).stream()
                .collect(Collectors.toMap(RubricScoringLevel::getUuid, Function.identity()));
        List<Pick> picks = evaluationRowRepository.findByEvaluationUuid(e.getUuid()).stream()
                .sorted(Comparator.comparing(row -> Optional.ofNullable(criteria.get(row.getCriteriaUuid()))
                        .map(RubricCriteria::getDisplayOrder).orElse(Integer.MAX_VALUE)))
                .map(row -> new Pick(row.getCriteriaUuid(),
                        Optional.ofNullable(criteria.get(row.getCriteriaUuid())).map(RubricCriteria::getComponentName).orElse(null),
                        Optional.ofNullable(levels.get(row.getScoringLevelUuid())).map(RubricScoringLevel::getName).orElse(null),
                        row.getPoints()))
                .toList();
        boolean submitted = e.getStatus() == CourseAssessmentLineItemRubricEvaluationStatus.COMPLETED;
        boolean draft = e.getStatus() == CourseAssessmentLineItemRubricEvaluationStatus.PENDING && !picks.isEmpty();
        return new StudentLesson(studentUuid, enrollmentUuid, submitted ? "submitted" : draft ? "draft" : "none",
                e.getScore(), e.getMaxScore(), picks, e.getComments(), revisions, e.getGradedAt(), assignments);
    }

    /** The lesson's cell on the course's active per-lesson rubric component, if it has one. */
    private Optional<Cell> lessonCell(UUID courseUuid, UUID lessonUuid) {
        List<CourseAssessment> perLesson = assessmentRepository.findByCourseUuidOrderByCreatedDateAsc(courseUuid).stream()
                .filter(a -> !Boolean.FALSE.equals(a.getActive()) && Boolean.TRUE.equals(a.getPerLesson()))
                .toList();
        if (perLesson.isEmpty() || lessonUuid == null) {
            return Optional.empty();
        }
        Map<UUID, CourseAssessment> byUuid = perLesson.stream().collect(Collectors.toMap(CourseAssessment::getUuid, Function.identity()));
        return lineItemRepository.findByCourseAssessmentUuidInOrderByDisplayOrderAscCreatedDateAsc(byUuid.keySet()).stream()
                .filter(item -> !Boolean.FALSE.equals(item.getActive()) && lessonUuid.equals(item.getLessonUuid()))
                .map(item -> {
                    CourseAssessment assessment = byUuid.get(item.getCourseAssessmentUuid());
                    UUID rubric = item.getRubricUuid() != null ? item.getRubricUuid() : assessment.getRubricUuid();
                    return rubric == null ? null : new Cell(assessment, item, rubric);
                })
                .filter(Objects::nonNull)
                .findFirst();
    }

    private BigDecimal maxScore(UUID rubricUuid, int criteriaCount) {
        return levelRepository.findByRubricUuidOrderByLevelOrder(rubricUuid).stream()
                .map(RubricScoringLevel::getPoints)
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .map(points -> points.multiply(BigDecimal.valueOf(criteriaCount)).setScale(2, RoundingMode.HALF_UP))
                .orElse(null);
    }

    private static String statusOf(AssignmentSubmission submission) {
        return submission.getStatus() == null ? "submitted" : submission.getStatus().name().toLowerCase(java.util.Locale.ROOT);
    }

    private record Cell(CourseAssessment assessment, CourseAssessmentLineItem lineItem, UUID rubricUuid) {
    }
}
