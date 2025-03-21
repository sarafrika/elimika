package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor @Table(name = "prerequisite_group")
public class PrerequisiteGroup extends BaseEntity {

    @Column(name = "course_uuid")
    private UUID courseUuid;

    @Enumerated(EnumType.STRING)
    @Column(name = "group_type")
    private GroupType groupType;

    public enum GroupType {
        AND,
        OR
    }
}
