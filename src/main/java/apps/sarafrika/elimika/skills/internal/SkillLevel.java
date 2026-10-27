package apps.sarafrika.elimika.skills.internal;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import apps.sarafrika.elimika.skills.spi.SkillLevelSummary;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "skill_levels")
@Getter
@Setter
@NoArgsConstructor
public class SkillLevel extends BaseEntity {

    @Column(name = "name")
    private String name;

    @Column(name = "min_percentage")
    private BigDecimal minPercentage;

    @Column(name = "counts_as_achieved")
    private Boolean countsAsAchieved;

    @Column(name = "display_order")
    private Integer displayOrder;

    public SkillLevelSummary toSummary(int rank) {
        return new SkillLevelSummary(getUuid(), name, minPercentage, Boolean.TRUE.equals(countsAsAchieved), rank);
    }
}
