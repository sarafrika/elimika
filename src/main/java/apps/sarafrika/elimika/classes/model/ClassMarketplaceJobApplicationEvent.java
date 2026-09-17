package apps.sarafrika.elimika.classes.model;

import apps.sarafrika.elimika.classes.util.converter.ClassMarketplaceJobApplicationEventTypeConverter;
import apps.sarafrika.elimika.classes.util.enums.ClassMarketplaceJobApplicationEventType;
import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/** One step in a marketplace job application's history, with who took it. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "class_marketplace_job_application_events")
public class ClassMarketplaceJobApplicationEvent extends BaseEntity {

    @Column(name = "application_uuid")
    private UUID applicationUuid;

    @Column(name = "job_uuid")
    private UUID jobUuid;

    @Column(name = "event_type")
    @Convert(converter = ClassMarketplaceJobApplicationEventTypeConverter.class)
    private ClassMarketplaceJobApplicationEventType eventType;

    @Column(name = "actor_uuid")
    private UUID actorUuid;

    @Column(name = "actor_name")
    private String actorName;

    @Column(name = "note")
    private String note;

    @Column(name = "interview_at")
    private LocalDateTime interviewAt;
}
