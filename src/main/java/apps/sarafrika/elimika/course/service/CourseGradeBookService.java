package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.CourseAssessmentGradeRevisionDTO;
import apps.sarafrika.elimika.course.dto.CourseEnrollmentFeedbackDTO;
import apps.sarafrika.elimika.course.dto.CourseGradeBandDTO;
import apps.sarafrika.elimika.course.dto.CourseAssessmentLineItemDTO;
import apps.sarafrika.elimika.course.dto.CourseAssessmentLineItemRubricEvaluationDTO;
import apps.sarafrika.elimika.course.dto.CourseAssessmentLineItemScoreDTO;
import apps.sarafrika.elimika.course.dto.CourseGradeBookDTO;
import apps.sarafrika.elimika.course.util.enums.AttemptStatus;
import apps.sarafrika.elimika.course.util.enums.CourseAttendanceStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface CourseGradeBookService {

    CourseAssessmentLineItemDTO createLineItem(UUID courseUuid, UUID assessmentUuid, CourseAssessmentLineItemDTO lineItemDTO);

    List<CourseAssessmentLineItemDTO> getLineItems(UUID courseUuid, UUID assessmentUuid);

    CourseAssessmentLineItemDTO updateLineItem(
            UUID courseUuid,
            UUID assessmentUuid,
            UUID lineItemUuid,
            CourseAssessmentLineItemDTO lineItemDTO
    );

    void deleteLineItem(UUID courseUuid, UUID assessmentUuid, UUID lineItemUuid);

    CourseAssessmentLineItemScoreDTO upsertLineItemScore(
            UUID courseUuid,
            UUID assessmentUuid,
            UUID lineItemUuid,
            UUID enrollmentUuid,
            CourseAssessmentLineItemScoreDTO scoreDTO
    );

    CourseAssessmentLineItemRubricEvaluationDTO getLineItemRubricEvaluation(
            UUID courseUuid,
            UUID assessmentUuid,
            UUID lineItemUuid,
            UUID enrollmentUuid
    );

    CourseAssessmentLineItemRubricEvaluationDTO upsertLineItemRubricEvaluation(
            UUID courseUuid,
            UUID assessmentUuid,
            UUID lineItemUuid,
            UUID enrollmentUuid,
            CourseAssessmentLineItemRubricEvaluationDTO evaluationDTO
    );

    CourseGradeBookDTO getEnrollmentGradeBook(UUID courseUuid, UUID enrollmentUuid);

    /** The course's grade bands, highest minimum first. */
    List<CourseGradeBandDTO> getGradeBands(UUID courseUuid);

    /** Replaces the course's grade bands with this set. */
    List<CourseGradeBandDTO> replaceGradeBands(UUID courseUuid, List<CourseGradeBandDTO> bands);

    /** Appends an instructor comment on the learner's progress; earlier comments are kept. */
    CourseEnrollmentFeedbackDTO addEnrollmentFeedback(UUID courseUuid, UUID enrollmentUuid, String comments, UUID authorUserUuid);

    /** Every instructor comment on the learner's progress, newest first. */
    List<CourseEnrollmentFeedbackDTO> getEnrollmentFeedback(UUID courseUuid, UUID enrollmentUuid);

    /** Every grade the learner has been given on this line item, oldest first. */
    List<CourseAssessmentGradeRevisionDTO> getLineItemRevisions(UUID courseUuid, UUID assessmentUuid, UUID lineItemUuid,
                                                              UUID enrollmentUuid);

    void recalculateCourseAssessment(UUID courseUuid, UUID assessmentUuid);

    void syncAssignmentGrade(
            UUID assignmentUuid,
            UUID enrollmentUuid,
            BigDecimal score,
            BigDecimal maxScore,
            String comments,
            LocalDateTime gradedAt,
            UUID gradedByUuid
    );

    void syncQuizAttemptGrade(
            UUID quizUuid,
            UUID enrollmentUuid,
            BigDecimal score,
            BigDecimal maxScore,
            String comments,
            LocalDateTime gradedAt,
            UUID gradedByUuid,
            AttemptStatus status
    );

    void syncAttendanceMark(
            UUID scheduledInstanceUuid,
            UUID classDefinitionUuid,
            UUID studentUuid,
            String classTitle,
            LocalDateTime markedAt,
            CourseAttendanceStatus attendanceStatus,
            Integer sessionNumber,
            UUID lessonUuid
    );
}
