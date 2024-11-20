package apps.sarafrika.elimika.instructor.persistence;

import apps.sarafrika.elimika.shared.audit.model.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class Instructor extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @ElementCollection
    @CollectionTable(
            name = "course_instructor",
            joinColumns = @JoinColumn(name = "instructor_id")
    )
    @Column(name = "course_id")
    private Set<Long> courseIds;

}

