package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.*;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Category extends BaseEntity {

    @Column(name = "name")
    private String name;

    @Column(name = "description")
    private String description;
}