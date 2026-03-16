package apps.sarafrika.elimika.course.factory;

import apps.sarafrika.elimika.course.dto.CourseAssessmentLineItemDTO;
import apps.sarafrika.elimika.course.model.CourseAssessmentLineItem;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CourseAssessmentLineItemFactory {

    public static CourseAssessmentLineItemDTO toDTO(CourseAssessmentLineItem entity) {
        if (entity == null) {
            return null;
        }

        return new CourseAssessmentLineItemDTO(
                entity.getUuid(),
                entity.getCourseAssessmentUuid(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getItemType(),
                entity.getAssignmentUuid(),
                entity.getQuizUuid(),
                entity.getRubricUuid(),
                entity.getScheduledInstanceUuid(),
                entity.getMaxScore(),
                entity.getWeightPercentage(),
                entity.getDisplayOrder(),
                entity.getActive(),
                entity.getDueAt(),
                entity.getCreatedDate(),
                entity.getCreatedBy(),
                entity.getLastModifiedDate(),
                entity.getLastModifiedBy()
        );
    }

    public static CourseAssessmentLineItem toEntity(CourseAssessmentLineItemDTO dto) {
        if (dto == null) {
            return null;
        }

        CourseAssessmentLineItem entity = new CourseAssessmentLineItem();
        entity.setUuid(dto.uuid());
        entity.setCourseAssessmentUuid(dto.courseAssessmentUuid());
        entity.setTitle(dto.title());
        entity.setDescription(dto.description());
        entity.setItemType(dto.itemType());
        entity.setAssignmentUuid(dto.assignmentUuid());
        entity.setQuizUuid(dto.quizUuid());
        entity.setRubricUuid(dto.rubricUuid());
        entity.setScheduledInstanceUuid(dto.scheduledInstanceUuid());
        entity.setMaxScore(dto.maxScore());
        entity.setWeightPercentage(dto.weightPercentage());
        entity.setDisplayOrder(dto.displayOrder());
        entity.setActive(dto.active());
        entity.setDueAt(dto.dueAt());
        entity.setCreatedDate(dto.createdDate());
        entity.setCreatedBy(dto.createdBy());
        entity.setLastModifiedDate(dto.updatedDate());
        entity.setLastModifiedBy(dto.updatedBy());
        return entity;
    }
}
