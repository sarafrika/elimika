package apps.sarafrika.elimika.course.domain;

import apps.sarafrika.elimika.shared.audit.model.AuditableEntity;
import ch.qos.logback.core.model.INamedModel;
import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

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

    @ManyToMany
    @JoinTable(
            joinColumns = @JoinColumn(name = "course_id"),
            inverseJoinColumns = @JoinColumn(name = "instructor_id")
    )
    Set<Instructor> instructors;

}
