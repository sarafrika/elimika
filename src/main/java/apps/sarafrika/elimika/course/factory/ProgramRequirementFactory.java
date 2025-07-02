package apps.sarafrika.elimika.course.factory;

import apps.sarafrika.elimika.course.dto.ProgramRequirementDTO;
import apps.sarafrika.elimika.course.model.ProgramRequirement;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ProgramRequirementFactory {

    // Convert ProgramRequirement entity to ProgramRequirementDTO
    public static ProgramRequirementDTO toDTO(ProgramRequirement programRequirement) {
        if (programRequirement == null) {
            return null;
        }
        return new ProgramRequirementDTO(
                programRequirement.getUuid(),
                programRequirement.getProgramUuid(),
                programRequirement.getRequirementType(),
                programRequirement.getRequirementText(),
                programRequirement.getIsMandatory(),
                programRequirement.getCreatedDate(),
                programRequirement.getCreatedBy(),
                programRequirement.getLastModifiedDate(),
                programRequirement.getLastModifiedBy()
        );
    }

    // Convert ProgramRequirementDTO to ProgramRequirement entity
    public static ProgramRequirement toEntity(ProgramRequirementDTO dto) {
        if (dto == null) {
            return null;
        }
        ProgramRequirement programRequirement = new ProgramRequirement();
        programRequirement.setUuid(dto.uuid());
        programRequirement.setProgramUuid(dto.programUuid());
        programRequirement.setRequirementType(dto.requirementType());
        programRequirement.setRequirementText(dto.requirementText());
        programRequirement.setIsMandatory(dto.isMandatory());
        programRequirement.setCreatedDate(dto.createdDate());
        programRequirement.setCreatedBy(dto.createdBy());
        programRequirement.setLastModifiedDate(dto.updatedDate());
        programRequirement.setLastModifiedBy(dto.updatedBy());
        return programRequirement;
    }
}