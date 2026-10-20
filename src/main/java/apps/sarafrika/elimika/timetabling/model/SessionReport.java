package apps.sarafrika.elimika.timetabling.model;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import apps.sarafrika.elimika.timetabling.spi.SessionReportStatus;
import apps.sarafrika.elimika.timetabling.util.converter.SessionReportStatusConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/** The mandatory report for one class session; its submitted contents live in {@link SessionReportRevision}. */
@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "session_reports")
public class SessionReport extends BaseEntity {

    @Column(name = "scheduled_instance_uuid")
    private UUID scheduledInstanceUuid;

    @Column(name = "class_definition_uuid")
    private UUID classDefinitionUuid;

    @Column(name = "organisation_uuid")
    private UUID organisationUuid;

    @Column(name = "instructor_uuid")
    private UUID instructorUuid;

    @Column(name = "status")
    @Convert(converter = SessionReportStatusConverter.class)
    private SessionReportStatus status;

    @Column(name = "due_at")
    private LocalDateTime dueAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "submitted_by_user_uuid")
    private UUID submittedByUserUuid;

    @Column(name = "editable_until")
    private LocalDateTime editableUntil;

    @Column(name = "revision_count")
    private Integer revisionCount;

    @Column(name = "reminded_at")
    private LocalDateTime remindedAt;

    @Column(name = "overdue_notified_at")
    private LocalDateTime overdueNotifiedAt;
}
