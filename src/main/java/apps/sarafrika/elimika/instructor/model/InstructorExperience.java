package apps.sarafrika.elimika.instructor.model;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import apps.sarafrika.elimika.shared.utils.Filterable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "instructor_experience")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class InstructorExperience extends BaseEntity {

    @Column(name = "instructor_uuid")
    @Filterable
    private UUID instructorUuid;

    @Column(name = "position")
    @Filterable
    private String position;

    @Column(name = "organization_name")
    @Filterable
    private String organizationName;

    @Column(name = "responsibilities", columnDefinition = "TEXT")
    private String responsibilities;

    @Column(name = "years_of_experience")
    @DecimalMin(value = "0.0")
    @DecimalMax(value = "50.0")
    private BigDecimal yearsOfExperience;

    @Column(name = "start_date")
    @Filterable
    private LocalDate startDate;

    @Column(name = "end_date")
    @Filterable
    private LocalDate endDate;

    @Column(name = "is_current_position")
    @Filterable
    private Boolean isCurrentPosition;
}