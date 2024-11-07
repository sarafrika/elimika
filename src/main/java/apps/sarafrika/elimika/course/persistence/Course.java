package apps.sarafrika.elimika.course.persistence;

import apps.sarafrika.elimika.shared.audit.model.AuditableEntity;
import jakarta.persistence.*;
import java.util.Set;
import lombok.*;

@Getter
@Setter
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class Course extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(length = 50)
    private String code;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 50)
    private String difficultyLevel;

    private int minAge;

    private int maxAge;

    @ElementCollection
    @CollectionTable(
        name = "course_instructor",
        joinColumns = @JoinColumn(
            name = "course_id",
            referencedColumnName = "id"
        )
    )
    @Column(name = "instructor_id")
    private Set<Long> instructorIds;
}
