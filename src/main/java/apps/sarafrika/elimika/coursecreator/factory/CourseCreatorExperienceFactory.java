package apps.sarafrika.elimika.coursecreator.factory;

import apps.sarafrika.elimika.coursecreator.dto.CourseCreatorExperienceDTO;
import apps.sarafrika.elimika.coursecreator.model.CourseCreatorExperience;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CourseCreatorExperienceFactory {

    public static CourseCreatorExperienceDTO toDTO(CourseCreatorExperience experience) {
        if (experience == null) {
            return null;
        }
        return new CourseCreatorExperienceDTO(
                experience.getUuid(),
                experience.getCourseCreatorUuid(),
                experience.getPosition(),
                experience.getOrganizationName(),
                experience.getResponsibilities(),
                experience.getYearsOfExperience(),
                experience.getStartDate(),
                experience.getEndDate(),
                experience.getIsCurrentPosition(),
                experience.getCreatedDate(),
                experience.getCreatedBy(),
                experience.getLastModifiedDate(),
                experience.getLastModifiedBy()
        );
    }

    public static CourseCreatorExperience toEntity(CourseCreatorExperienceDTO dto) {
        if (dto == null) {
            return null;
        }
        CourseCreatorExperience experience = new CourseCreatorExperience();
        experience.setUuid(dto.uuid());
        experience.setCourseCreatorUuid(dto.courseCreatorUuid());
        experience.setPosition(dto.position());
        experience.setOrganizationName(dto.organizationName());
        experience.setResponsibilities(dto.responsibilities());
        experience.setYearsOfExperience(dto.yearsOfExperience());
        experience.setStartDate(dto.startDate());
        experience.setEndDate(dto.endDate());
        experience.setIsCurrentPosition(dto.isCurrentPosition());
        experience.setCreatedDate(dto.createdDate());
        experience.setCreatedBy(dto.createdBy());
        experience.setLastModifiedDate(dto.updatedDate());
        experience.setLastModifiedBy(dto.updatedBy());
        return experience;
    }
}
