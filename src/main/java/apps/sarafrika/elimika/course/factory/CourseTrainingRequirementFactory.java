package apps.sarafrika.elimika.course.factory;

import apps.sarafrika.elimika.course.dto.CourseTrainingRequirementDTO;
import apps.sarafrika.elimika.course.model.CourseTrainingRequirement;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CourseTrainingRequirementFactory {

    public static CourseTrainingRequirementDTO toDTO(CourseTrainingRequirement entity) {
        if (entity == null) {
            return null;
        }
        return new CourseTrainingRequirementDTO(
                entity.getUuid(),
                entity.getCourseUuid(),
                entity.getRequirementType(),
                entity.getName(),
                entity.getDescription(),
                entity.getQuantity(),
                entity.getUnit(),
                entity.getProvidedBy(),
                entity.getIsMandatory(),
                entity.getCreatedDate(),
                entity.getCreatedBy(),
                entity.getLastModifiedDate(),
                entity.getLastModifiedBy()
        );
    }

    public static CourseTrainingRequirement toEntity(CourseTrainingRequirementDTO dto) {
        if (dto == null) {
            return null;
        }
        CourseTrainingRequirement entity = new CourseTrainingRequirement();
        entity.setUuid(dto.uuid());
        entity.setCourseUuid(dto.courseUuid());
        entity.setRequirementType(dto.requirementType());
        entity.setName(dto.name());
        entity.setDescription(dto.description());
        entity.setQuantity(dto.quantity());
        entity.setUnit(dto.unit());
        entity.setProvidedBy(dto.providedBy());
        entity.setIsMandatory(dto.isMandatory());
        entity.setCreatedDate(dto.createdDate());
        entity.setCreatedBy(dto.createdBy());
        entity.setLastModifiedDate(dto.updatedDate());
        entity.setLastModifiedBy(dto.updatedBy());
        return entity;
    }
}
