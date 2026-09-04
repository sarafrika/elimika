package apps.sarafrika.elimika.instructor.model;

import apps.sarafrika.elimika.shared.utils.enums.ProficiencyLevel;
import apps.sarafrika.elimika.shared.model.BaseEntity;
import apps.sarafrika.elimika.shared.utils.converter.ProficiencyLevelConverter;
import apps.sarafrika.elimika.shared.utils.Filterable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Convert;
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
    @Filterable
    private UUID instructorUuid;

    @Column(name = "skill_name")
    @Filterable
    private String skillName;

    @Column(name = "proficiency_level")
    @Convert(converter = ProficiencyLevelConverter.class)
    @Filterable
    private ProficiencyLevel proficiencyLevel;
}
