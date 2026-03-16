package apps.sarafrika.elimika.course.factory;

import apps.sarafrika.elimika.course.dto.CourseAssessmentLineItemRubricEvaluationDTO;
import apps.sarafrika.elimika.course.dto.CourseAssessmentLineItemRubricEvaluationRowDTO;
import apps.sarafrika.elimika.course.model.CourseAssessmentLineItemRubricEvaluation;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CourseAssessmentLineItemRubricEvaluationFactory {

    public static CourseAssessmentLineItemRubricEvaluationDTO toDTO(
            CourseAssessmentLineItemRubricEvaluation entity,
            List<CourseAssessmentLineItemRubricEvaluationRowDTO> criteriaSelections
    ) {
        if (entity == null) {
            return null;
        }

        return new CourseAssessmentLineItemRubricEvaluationDTO(
                entity.getUuid(),
                entity.getLineItemUuid(),
                entity.getEnrollmentUuid(),
                entity.getRubricUuid(),
                entity.getStatus(),
                entity.getAttendanceStatus(),
                entity.getScore(),
                entity.getMaxScore(),
                entity.getPercentage(),
                entity.getComments(),
                entity.getGradedAt(),
                entity.getGradedByUuid(),
                criteriaSelections
        );
    }
}
