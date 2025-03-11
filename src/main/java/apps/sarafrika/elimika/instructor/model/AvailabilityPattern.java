package apps.sarafrika.elimika.instructor.model;

import apps.sarafrika.elimika.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AvailabilityPattern extends BaseEntity {

    @Column(name = "pattern_type")
    private String patternType;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "instructor_id")
    private Long instructorId;
}
