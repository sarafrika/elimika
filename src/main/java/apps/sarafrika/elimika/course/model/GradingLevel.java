package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import apps.sarafrika.elimika.shared.utils.Filterable;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "grading_levels")
public class GradingLevel extends BaseEntity {

    @Column(name = "name")
    @Filterable
    private String name;

    @Column(name = "points")
    private Integer points;

    @Column(name = "level_order")
    @Filterable
    private Integer levelOrder;
}