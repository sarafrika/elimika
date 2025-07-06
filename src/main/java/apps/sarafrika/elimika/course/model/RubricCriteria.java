package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.common.model.BaseEntity;
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
    private UUID rubricUuid;

    @Column(name = "component_name")
    private String componentName;

    @Column(name = "description")
    private String description;

    @Column(name = "display_order")
    private Integer displayOrder;
}