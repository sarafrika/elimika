package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.course.util.converter.TrainingApplicationTypeConverter;
import apps.sarafrika.elimika.course.util.converter.TrainingRequirementAcquisitionConverter;
import apps.sarafrika.elimika.course.util.enums.TrainingApplicationType;
import apps.sarafrika.elimika.course.util.enums.TrainingRequirementAcquisition;
import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/** The applicant's answer to one training requirement: whether they have it, and if not how they would get it. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "training_application_requirement_answers")
public class TrainingApplicationRequirementAnswer extends BaseEntity {

    @Column(name = "application_type")
    @Convert(converter = TrainingApplicationTypeConverter.class)
    private TrainingApplicationType applicationType;

    @Column(name = "application_uuid")
    private UUID applicationUuid;

    @Column(name = "requirement_uuid")
    private UUID requirementUuid;

    @Column(name = "has_it")
    private Boolean hasIt;

    @Column(name = "acquisition")
    @Convert(converter = TrainingRequirementAcquisitionConverter.class)
    private TrainingRequirementAcquisition acquisition;
}
