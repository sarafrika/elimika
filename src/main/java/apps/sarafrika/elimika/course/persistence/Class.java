package apps.sarafrika.elimika.course.persistence;

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

    private Long instructorId;

    private Long courseId;

    private Long instructorAvailabilityId;

}
