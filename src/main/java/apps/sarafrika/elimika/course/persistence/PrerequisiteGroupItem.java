package apps.sarafrika.elimika.course.persistence;

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
public class PrerequisiteGroupItem extends BaseEntity {

    @Column(name = "prerequisite_group_id")
    private Long prerequisiteGroupId;

    @Column(name = "prerequisite_id")
    private Long prerequisiteId;
}

