package apps.sarafrika.elimika.instructor.factory;

import apps.sarafrika.elimika.instructor.dto.InstructorExperienceDTO;
import apps.sarafrika.elimika.instructor.model.InstructorExperience;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class InstructorExperienceFactory {

    // Convert InstructorExperience entity to InstructorExperienceDTO
    public static InstructorExperienceDTO toDTO(InstructorExperience experience) {
        if (experience == null) {
            return null;
        }
        return new InstructorExperienceDTO(
                experience.getUuid(),
                experience.getInstructorUuid(),
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

    // Convert InstructorExperienceDTO to InstructorExperience entity
    public static InstructorExperience toEntity(InstructorExperienceDTO dto) {
        if (dto == null) {
            return null;
        }
        InstructorExperience experience = new InstructorExperience();
        experience.setUuid(dto.uuid());
        experience.setInstructorUuid(dto.instructorUuid());
        experience.setPosition(dto.position());
        experience.setOrganizationName(dto.organizationName());
        experience.setResponsibilities(dto.responsibilities());
        experience.setYearsOfExperience(dto.yearsOfExperience());
        experience.setStartDate(dto.startDate());
        experience.setEndDate(dto.endDate());
        experience.setIsCurrentPosition(dto.isCurrentPosition());
        return experience;
    }
}