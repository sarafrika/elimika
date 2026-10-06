package apps.sarafrika.elimika.profile.internal.model;

import apps.sarafrika.elimika.profile.spi.ExperienceType;
import apps.sarafrika.elimika.shared.utils.Filterable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "user_experience")
@Getter
@Setter
@NoArgsConstructor
public class UserExperience extends UserOwnedEntity {

    @Column(name = "position")
    @Filterable
    private String position;

    @Column(name = "organization_name")
    @Filterable
    private String organizationName;

    @Column(name = "responsibilities")
    private String responsibilities;

    @Column(name = "years_of_experience")
    @Filterable
    private BigDecimal yearsOfExperience;

    @Column(name = "start_date")
    @Filterable
    private LocalDate startDate;

    @Column(name = "end_date")
    @Filterable
    private LocalDate endDate;

    @Column(name = "is_current_position")
    @Filterable
    private Boolean isCurrentPosition = Boolean.FALSE;

    @Column(name = "experience_type")
    @Filterable
    private ExperienceType experienceType;
}
