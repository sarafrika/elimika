package apps.sarafrika.elimika.coursecreator.model;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import apps.sarafrika.elimika.shared.utils.Filterable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "course_creator_experience")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CourseCreatorExperience extends BaseEntity {

    @Column(name = "course_creator_uuid")
    @Filterable
    private UUID courseCreatorUuid;

    @Column(name = "position")
    @Filterable
    private String position;

    @Column(name = "organization_name")
    @Filterable
    private String organizationName;

    @Column(name = "responsibilities", columnDefinition = "TEXT")
    private String responsibilities;

    @Column(name = "years_of_experience")
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
