package apps.sarafrika.elimika.course.factory;

import apps.sarafrika.elimika.course.dto.*;
import apps.sarafrika.elimika.course.model.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AssessmentRubricFactory {

    // Convert AssessmentRubric entity to AssessmentRubricDTO
    public static AssessmentRubricDTO toDTO(AssessmentRubric assessmentRubric) {
        if (assessmentRubric == null) {
            return null;
        }
        return new AssessmentRubricDTO(
                assessmentRubric.getUuid(),
                assessmentRubric.getTitle(),
                assessmentRubric.getDescription(),
                assessmentRubric.getCourseUuid(),
                assessmentRubric.getRubricType(),
                assessmentRubric.getInstructorUuid(),
                assessmentRubric.getIsPublic(),
                assessmentRubric.getStatus(),
                assessmentRubric.getIsActive(),
                assessmentRubric.getTotalWeight(),
                assessmentRubric.getWeightUnit(),
                assessmentRubric.getIsWeighted(),
                assessmentRubric.getCreatedDate(),
                assessmentRubric.getCreatedBy(),
                assessmentRubric.getLastModifiedDate(),
                assessmentRubric.getLastModifiedBy()
        );
    }

    // Convert AssessmentRubricDTO to AssessmentRubric entity
    public static AssessmentRubric toEntity(AssessmentRubricDTO dto) {
        if (dto == null) {
            return null;
        }
        AssessmentRubric assessmentRubric = new AssessmentRubric();
        assessmentRubric.setUuid(dto.uuid());
        assessmentRubric.setTitle(dto.title());
        assessmentRubric.setDescription(dto.description());
        assessmentRubric.setCourseUuid(dto.courseUuid());
        assessmentRubric.setRubricType(dto.rubricType());
        assessmentRubric.setInstructorUuid(dto.instructorUuid());
        assessmentRubric.setIsPublic(dto.isPublic());
        assessmentRubric.setStatus(dto.status());
        assessmentRubric.setIsActive(dto.active());
        assessmentRubric.setTotalWeight(dto.totalWeight());
        assessmentRubric.setWeightUnit(dto.weightUnit());
        assessmentRubric.setIsWeighted(dto.isWeighted());
        assessmentRubric.setCreatedDate(dto.createdDate());
        assessmentRubric.setCreatedBy(dto.createdBy());
        assessmentRubric.setLastModifiedDate(dto.updatedDate());
        assessmentRubric.setLastModifiedBy(dto.updatedBy());
        return assessmentRubric;
    }
}