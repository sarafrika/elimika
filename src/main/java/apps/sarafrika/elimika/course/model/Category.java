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
@Table(name = "course_categories")
public class Category extends BaseEntity {

    @Column(name = "name")
    @Filterable
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "parent_uuid")
    @Filterable
    private UUID parentUuid;

    @Column(name = "is_active")
    @Filterable
    private Boolean isActive;
}