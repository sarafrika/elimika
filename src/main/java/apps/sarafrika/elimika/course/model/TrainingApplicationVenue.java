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

/** A venue an organisation applicant offers for delivering the course or program. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "training_application_venues")
public class TrainingApplicationVenue extends BaseEntity {

    @Column(name = "application_type")
    @Convert(converter = TrainingApplicationTypeConverter.class)
    private TrainingApplicationType applicationType;

    @Column(name = "application_uuid")
    private UUID applicationUuid;

    @Column(name = "resource_uuid")
    private UUID resourceUuid;
}
