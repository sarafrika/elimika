package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.course.util.converter.TrainingApplicationTypeConverter;
import apps.sarafrika.elimika.course.util.enums.TrainingApplicationType;
import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/** A named age band an instructor applicant would teach, with its own hours per lesson. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "training_application_learner_groups")
public class TrainingApplicationLearnerGroup extends BaseEntity {

    @Column(name = "application_type")
    @Convert(converter = TrainingApplicationTypeConverter.class)
    private TrainingApplicationType applicationType;

    @Column(name = "application_uuid")
    private UUID applicationUuid;

    @Column(name = "name")
    private String name;

    @Column(name = "min_age")
    private Integer minAge;

    @Column(name = "max_age")
    private Integer maxAge;

    @Column(name = "position")
    private Integer position;
}
