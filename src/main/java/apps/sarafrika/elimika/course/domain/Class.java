package apps.sarafrika.elimika.course.domain;

import apps.sarafrika.elimika.shared.audit.model.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Getter
@Setter
@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class Class extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date scheduledStartDate;

    @Column(nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date scheduledEndDate;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private Instructor instructor;

    @ManyToOne
    private Course course;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "instructor_availability_id")
    private InstructorAvailability availabilitySlot;
}
