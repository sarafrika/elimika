package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Rubric Scoring Level Entity
 * <p>
 * Represents custom scoring levels defined per rubric for flexible matrix configurations.
 * These levels form the columns of the rubric matrix (e.g., Excellent, Good, Fair, Poor).
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2024-08-13
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "rubric_scoring_levels")
public class RubricScoringLevel extends BaseEntity {

    @Column(name = "rubric_uuid")
    private UUID rubricUuid;

    @Column(name = "name", length = 50)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "points", precision = 5, scale = 2)
    private BigDecimal points;

    @Column(name = "level_order")
    private Integer levelOrder;

    @Column(name = "color_code", length = 7)
    private String colorCode;

    @Column(name = "is_passing")
    private Boolean isPassing;
}