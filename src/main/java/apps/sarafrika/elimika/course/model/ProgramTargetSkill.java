package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/** A skill a programme promises to develop. */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "program_target_skills")
public class ProgramTargetSkill extends BaseEntity {

    @Column(name = "program_uuid")
    private UUID programUuid;

    @Column(name = "skill_uuid")
    private UUID skillUuid;

    @Column(name = "display_order")
    private Integer displayOrder;
}
