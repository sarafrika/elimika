package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** A learner's current level for one programme skill, taken from their latest live evidence. */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "learner_skill_levels")
public class LearnerSkillLevel extends BaseEntity {

    @Column(name = "student_uuid")
    private UUID studentUuid;

    @Column(name = "program_uuid")
    private UUID programUuid;

    @Column(name = "skill_uuid")
    private UUID skillUuid;

    @Column(name = "evidence_uuid")
    private UUID evidenceUuid;

    @Column(name = "score")
    private BigDecimal score;

    @Column(name = "level_name")
    private String levelName;

    @Column(name = "level_rank")
    private Integer levelRank;

    @Column(name = "counts_as_achieved")
    private Boolean countsAsAchieved;

    @Column(name = "first_achieved_at")
    private LocalDateTime firstAchievedAt;

    @Column(name = "graded_at")
    private LocalDateTime gradedAt;
}
