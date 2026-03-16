package apps.sarafrika.elimika.course.factory;

import apps.sarafrika.elimika.course.dto.CourseAssessmentLineItemRubricEvaluationRowDTO;
import apps.sarafrika.elimika.course.model.CourseAssessmentLineItemRubricEvaluationRow;
import apps.sarafrika.elimika.course.model.RubricCriteria;
import apps.sarafrika.elimika.course.model.RubricScoringLevel;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CourseAssessmentLineItemRubricEvaluationRowFactory {

    public static CourseAssessmentLineItemRubricEvaluationRowDTO toDTO(
            CourseAssessmentLineItemRubricEvaluationRow entity,
            RubricCriteria criteria,
            RubricScoringLevel scoringLevel
    ) {
        if (entity == null) {
            return null;
        }

        return new CourseAssessmentLineItemRubricEvaluationRowDTO(
                entity.getUuid(),
                entity.getCriteriaUuid(),
                criteria != null ? criteria.getComponentName() : null,
                entity.getScoringLevelUuid(),
                scoringLevel != null ? scoringLevel.getName() : null,
                entity.getPoints(),
                entity.getComments()
        );
    }

    public static CourseAssessmentLineItemRubricEvaluationRow toEntity(CourseAssessmentLineItemRubricEvaluationRowDTO dto) {
        if (dto == null) {
            return null;
        }

        CourseAssessmentLineItemRubricEvaluationRow entity = new CourseAssessmentLineItemRubricEvaluationRow();
        entity.setUuid(dto.uuid());
        entity.setCriteriaUuid(dto.criteriaUuid());
        entity.setScoringLevelUuid(dto.scoringLevelUuid());
        entity.setPoints(dto.points());
        entity.setComments(dto.comments());
        return entity;
    }
}
