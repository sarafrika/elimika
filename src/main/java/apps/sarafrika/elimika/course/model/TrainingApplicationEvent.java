package apps.sarafrika.elimika.course.model;

import apps.sarafrika.elimika.course.util.converter.TrainingApplicationEventTypeConverter;
import apps.sarafrika.elimika.course.util.converter.TrainingApplicationTypeConverter;
import apps.sarafrika.elimika.course.util.enums.TrainingApplicationEventType;
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

/** One step in a course or program training application's history, with who took it. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "training_application_events")
public class TrainingApplicationEvent extends BaseEntity {

    @Column(name = "application_type")
    @Convert(converter = TrainingApplicationTypeConverter.class)
    private TrainingApplicationType applicationType;

    @Column(name = "application_uuid")
    private UUID applicationUuid;

    @Column(name = "event_type")
    @Convert(converter = TrainingApplicationEventTypeConverter.class)
    private TrainingApplicationEventType eventType;

    @Column(name = "actor_uuid")
    private UUID actorUuid;

    @Column(name = "actor_name")
    private String actorName;

    @Column(name = "note")
    private String note;
}
