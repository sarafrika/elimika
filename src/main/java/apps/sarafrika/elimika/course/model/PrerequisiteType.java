package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.*;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PrerequisiteType extends BaseEntity {

    @Column(name = "name")
    private String name;

}
