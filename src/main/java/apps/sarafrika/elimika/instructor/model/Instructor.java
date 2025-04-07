package apps.sarafrika.elimika.instructor.model;

import apps.sarafrika.elimika.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor @Table(name = "instructors")
public class Instructor extends BaseEntity {

    @Column(name = "user_uuid")
    private UUID userUuid;

    @ElementCollection
    @CollectionTable(
            name = "course_instructor",
            joinColumns = @JoinColumn(name = "instructor_id")
    )
    @Column(name = "course_id")
    private Set<Long> courseIds;

}

