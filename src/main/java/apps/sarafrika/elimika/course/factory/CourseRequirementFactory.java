package apps.sarafrika.elimika.course.factory;

import apps.sarafrika.elimika.course.dto.*;
import apps.sarafrika.elimika.course.model.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CourseRequirementFactory {

    // Convert CourseRequirement entity to CourseRequirementDTO
    public static CourseRequirementDTO toDTO(CourseRequirement courseRequirement) {
        if (courseRequirement == null) {
            return null;
        }
        return new CourseRequirementDTO(
                courseRequirement.getUuid(),
                courseRequirement.getCourseUuid(),
                courseRequirement.getRequirementType(),
                courseRequirement.getRequirementText(),
                courseRequirement.getIsMandatory(),
                courseRequirement.getCreatedDate(),
                courseRequirement.getCreatedBy(),
                courseRequirement.getLastModifiedDate(),
                courseRequirement.getLastModifiedBy()
        );
    }

    // Convert CourseRequirementDTO to CourseRequirement entity
    public static CourseRequirement toEntity(CourseRequirementDTO dto) {
        if (dto == null) {
            return null;
        }
        CourseRequirement courseRequirement = new CourseRequirement();
        courseRequirement.setUuid(dto.uuid());
        courseRequirement.setCourseUuid(dto.courseUuid());
        courseRequirement.setRequirementType(dto.requirementType());
        courseRequirement.setRequirementText(dto.requirementText());
        courseRequirement.setIsMandatory(dto.isMandatory());
        courseRequirement.setCreatedDate(dto.createdDate());
        courseRequirement.setCreatedBy(dto.createdBy());
        courseRequirement.setLastModifiedDate(dto.updatedDate());
        courseRequirement.setLastModifiedBy(dto.updatedBy());
        return courseRequirement;
    }
}
