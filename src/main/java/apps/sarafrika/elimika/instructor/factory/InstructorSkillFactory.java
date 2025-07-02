package apps.sarafrika.elimika.instructor.factory;

import apps.sarafrika.elimika.instructor.dto.InstructorSkillDTO;
import apps.sarafrika.elimika.instructor.model.InstructorSkill;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class InstructorSkillFactory {

    // Convert InstructorSkill entity to InstructorSkillDTO
    public static InstructorSkillDTO toDTO(InstructorSkill skill) {
        if (skill == null) {
            return null;
        }
        return new InstructorSkillDTO(
                skill.getUuid(),
                skill.getInstructorUuid(),
                skill.getSkillName(),
                skill.getProficiencyLevel(),
                skill.getCreatedDate(),
                skill.getCreatedBy(),
                skill.getLastModifiedDate(),
                skill.getLastModifiedBy()
        );
    }

    // Convert InstructorSkillDTO to InstructorSkill entity
    public static InstructorSkill toEntity(InstructorSkillDTO dto) {
        if (dto == null) {
            return null;
        }
        InstructorSkill skill = new InstructorSkill();
        skill.setUuid(dto.uuid());
        skill.setInstructorUuid(dto.instructorUuid());
        skill.setSkillName(dto.skillName());
        skill.setProficiencyLevel(dto.proficiencyLevel());
        return skill;
    }
}