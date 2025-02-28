package apps.sarafrika.elimika.instructor.persistence;

import apps.sarafrika.elimika.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class Instructor extends BaseEntity {

    @Column(name = "name")
    private String name;

    @Column(name = "email")
    private String email;

    @Column(name = "bio")
    private String bio;

    @ElementCollection
    @CollectionTable(
            name = "course_instructor",
            joinColumns = @JoinColumn(name = "instructor_id")
    )
    @Column(name = "course_id")
    private Set<Long> courseIds;

}

