package apps.sarafrika.elimika.course.domain;

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

    @ManyToMany(mappedBy = "instructors")
    private Set<Course> courses;

    @OneToMany(mappedBy = "instructor", fetch = FetchType.LAZY)
    private Set<InstructorAvailability> availableSlots;

    @OneToMany(mappedBy = "instructor", fetch = FetchType.LAZY)
    private Set<Class> classes;

}
