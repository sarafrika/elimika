package apps.sarafrika.elimika.coursecreator.factory;

import apps.sarafrika.elimika.coursecreator.dto.CourseCreatorSkillDTO;
import apps.sarafrika.elimika.coursecreator.model.CourseCreatorSkill;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CourseCreatorSkillFactory {

    public static CourseCreatorSkillDTO toDTO(CourseCreatorSkill skill) {
        if (skill == null) {
            return null;
        }
        return new CourseCreatorSkillDTO(
                skill.getUuid(),
                skill.getCourseCreatorUuid(),
                skill.getSkillName(),
                skill.getProficiencyLevel(),
                skill.getCreatedDate(),
                skill.getCreatedBy(),
                skill.getLastModifiedDate(),
                skill.getLastModifiedBy()
        );
    }

    public static CourseCreatorSkill toEntity(CourseCreatorSkillDTO dto) {
        if (dto == null) {
            return null;
        }
        CourseCreatorSkill skill = new CourseCreatorSkill();
        skill.setUuid(dto.uuid());
        skill.setCourseCreatorUuid(dto.courseCreatorUuid());
        skill.setSkillName(dto.skillName());
        skill.setProficiencyLevel(dto.proficiencyLevel());
        skill.setCreatedDate(dto.createdDate());
        skill.setCreatedBy(dto.createdBy());
        skill.setLastModifiedDate(dto.updatedDate());
        skill.setLastModifiedBy(dto.updatedBy());
        return skill;
    }
}
