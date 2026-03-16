package apps.sarafrika.elimika.course.factory;

import apps.sarafrika.elimika.course.dto.CourseAssessmentLineItemScoreDTO;
import apps.sarafrika.elimika.course.model.CourseAssessmentLineItemScore;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CourseAssessmentLineItemScoreFactory {

    public static CourseAssessmentLineItemScoreDTO toDTO(CourseAssessmentLineItemScore entity) {
        if (entity == null) {
            return null;
        }

        return new CourseAssessmentLineItemScoreDTO(
                entity.getUuid(),
                entity.getLineItemUuid(),
                entity.getEnrollmentUuid(),
                entity.getScore(),
                entity.getMaxScore(),
                entity.getPercentage(),
                entity.getComments(),
                entity.getGradedAt(),
                entity.getGradedByUuid(),
                entity.getCreatedDate(),
                entity.getCreatedBy(),
                entity.getLastModifiedDate(),
                entity.getLastModifiedBy()
        );
    }

    public static CourseAssessmentLineItemScore toEntity(CourseAssessmentLineItemScoreDTO dto) {
        if (dto == null) {
            return null;
        }

        CourseAssessmentLineItemScore entity = new CourseAssessmentLineItemScore();
        entity.setUuid(dto.uuid());
        entity.setLineItemUuid(dto.lineItemUuid());
        entity.setEnrollmentUuid(dto.enrollmentUuid());
        entity.setScore(dto.score());
        entity.setMaxScore(dto.maxScore());
        entity.setPercentage(dto.percentage());
        entity.setComments(dto.comments());
        entity.setGradedAt(dto.gradedAt());
        entity.setGradedByUuid(dto.gradedByUuid());
        entity.setCreatedDate(dto.createdDate());
        entity.setCreatedBy(dto.createdBy());
        entity.setLastModifiedDate(dto.updatedDate());
        entity.setLastModifiedBy(dto.updatedBy());
        return entity;
    }
}
