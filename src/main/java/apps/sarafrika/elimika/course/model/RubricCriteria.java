package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import apps.sarafrika.elimika.shared.utils.Filterable;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "rubric_criteria")
public class RubricCriteria extends BaseEntity {

    @Column(name = "rubric_uuid")
    @Filterable
    private UUID rubricUuid;

    @Column(name = "component_name")
    @Filterable
    private String componentName;

    @Column(name = "description")
    private String description;

    @Column(name = "display_order")
    @Filterable
    private Integer displayOrder;

}