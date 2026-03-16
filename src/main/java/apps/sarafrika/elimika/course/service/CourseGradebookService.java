package apps.sarafrika.elimika.course.service;

import apps.sarafrika.elimika.course.dto.CourseAssessmentLineItemDTO;
import apps.sarafrika.elimika.course.dto.CourseAssessmentLineItemScoreDTO;
import apps.sarafrika.elimika.course.dto.CourseGradebookDTO;
import apps.sarafrika.elimika.course.util.enums.AttemptStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface CourseGradebookService {

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

    CourseGradebookDTO getEnrollmentGradebook(UUID courseUuid, UUID enrollmentUuid);

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
}
