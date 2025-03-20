package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.common.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "")
public class PrerequisiteGroupItem extends BaseEntity {

    @Column(name = "prerequisite_group_uuid")
    private Long prerequisiteGroupUuid;

    @Column(name = "prerequisite_uuid")
    private Long prerequisiteUuid;


}

