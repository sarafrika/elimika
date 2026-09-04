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
@Table(name = "course_difficulty_levels")
public class DifficultyLevel extends BaseEntity {

    @Column(name = "name")
    @Filterable
    private String name;

    @Column(name = "level_order")
    @Filterable
    private Integer levelOrder;

    @Column(name = "description")
    private String description;
}