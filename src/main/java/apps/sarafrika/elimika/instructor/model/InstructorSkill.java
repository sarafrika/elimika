package apps.sarafrika.elimika.instructor.model;

import apps.sarafrika.elimika.common.model.BaseEntity;
import apps.sarafrika.elimika.instructor.util.enums.ProficiencyLevel;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.ENUM)
    @Column(name = "proficiency_level", nullable = false, columnDefinition = "proficiency_level_enum")
    private ProficiencyLevel proficiencyLevel;
}