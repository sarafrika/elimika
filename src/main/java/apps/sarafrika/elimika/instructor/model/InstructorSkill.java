package apps.sarafrika.elimika.instructor.model;

import apps.sarafrika.elimika.common.model.BaseEntity;
import apps.sarafrika.elimika.instructor.util.converter.ProficiencyLevelConverter;
import apps.sarafrika.elimika.instructor.util.enums.ProficiencyLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(name = "instructor_skills")
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class InstructorSkill extends BaseEntity {

    @Column(name = "instructor_uuid")
    private UUID instructorUuid;

    @Column(name = "skill_name")
    private String skillName;

    @Column(name = "proficiency_level")
    @Convert(converter = ProficiencyLevelConverter.class)
    private ProficiencyLevel proficiencyLevel;
}