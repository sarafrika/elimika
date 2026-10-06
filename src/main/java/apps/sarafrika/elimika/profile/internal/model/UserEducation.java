package apps.sarafrika.elimika.profile.internal.model;

import apps.sarafrika.elimika.shared.utils.Filterable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_education")
@Getter
@Setter
@NoArgsConstructor
public class UserEducation extends UserOwnedEntity {

    @Column(name = "qualification")
    @Filterable
    private String qualification;

    @Column(name = "field_of_study")
    @Filterable
    private String fieldOfStudy;

    @Column(name = "school_name")
    @Filterable
    private String schoolName;

    @Column(name = "start_year")
    @Filterable
    private Integer startYear;

    @Column(name = "year_completed")
    @Filterable
    private Integer yearCompleted;

    @Column(name = "certificate_number")
    @Filterable
    private String certificateNumber;
}
