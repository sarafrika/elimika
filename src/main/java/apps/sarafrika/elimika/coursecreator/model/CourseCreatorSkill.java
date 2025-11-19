package apps.sarafrika.elimika.coursecreator.model;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import apps.sarafrika.elimika.shared.utils.converter.ProficiencyLevelConverter;
import apps.sarafrika.elimika.shared.utils.enums.ProficiencyLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Convert;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "course_creator_skills")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CourseCreatorSkill extends BaseEntity {

    @Column(name = "course_creator_uuid")
    private UUID courseCreatorUuid;

    @Column(name = "skill_name")
    private String skillName;

    @Column(name = "proficiency_level")
    @Convert(converter = ProficiencyLevelConverter.class)
    private ProficiencyLevel proficiencyLevel;
}
