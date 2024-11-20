package apps.sarafrika.elimika.instructor.persistence;

import apps.sarafrika.elimika.shared.audit.model.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;
import java.util.Date;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstructorAvailability extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Temporal(TemporalType.DATE)
    private Date availabilityStart;

    @Temporal(TemporalType.DATE)
    private Date availabilityEnd;

    @Enumerated(EnumType.ORDINAL)
    private DayOfWeek dayOfWeek;

    @Column(nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date timeSlotStart;

    @Column(nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date timeSlotEnd;

    private Long instructorId;

}
