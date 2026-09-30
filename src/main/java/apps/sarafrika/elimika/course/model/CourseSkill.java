package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import apps.sarafrika.elimika.shared.utils.converter.ProficiencyLevelConverter;
import apps.sarafrika.elimika.shared.utils.enums.ProficiencyLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/** A skill the course teaches, tagged by its creator from the skills taxonomy. */
@Entity
@Table(name = "course_skills")
@Getter
@Setter
@NoArgsConstructor
public class CourseSkill extends BaseEntity {

    @Column(name = "course_uuid")
    private UUID courseUuid;

    @Column(name = "skill_uuid")
    private UUID skillUuid;

    @Column(name = "level")
    @Convert(converter = ProficiencyLevelConverter.class)
    private ProficiencyLevel level;

    @Column(name = "weight")
    private Integer weight;

    public CourseSkill(UUID courseUuid, UUID skillUuid, ProficiencyLevel level, Integer weight) {
        this.courseUuid = courseUuid;
        this.skillUuid = skillUuid;
        this.level = level;
        this.weight = weight;
    }
}
