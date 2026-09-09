package apps.sarafrika.elimika.timetabling.model;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import apps.sarafrika.elimika.timetabling.spi.InstructorTimeHoldStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A claim on an instructor's diary raised by a marketplace job application. Deliberately not a
 * {@link ScheduledInstance}, which is a real session carrying attendance, enrolment, reminders
 * and pay accrual, none of which a hold should ever attract.
 */
@Entity
@Table(name = "instructor_time_holds")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InstructorTimeHold extends BaseEntity {

    @Column(name = "instructor_uuid")
    private UUID instructorUuid;

    @Column(name = "instructor_user_uuid")
    private UUID instructorUserUuid;

    @Column(name = "organisation_uuid")
    private UUID organisationUuid;

    @Column(name = "job_uuid")
    private UUID jobUuid;

    @Column(name = "application_uuid")
    private UUID applicationUuid;

    @Column(name = "title")
    private String title;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "timezone")
    private String timezone;

    @Column(name = "status")
    private InstructorTimeHoldStatus status;

    @Column(name = "class_definition_uuid")
    private UUID classDefinitionUuid;

    @Column(name = "scheduled_instance_uuid")
    private UUID scheduledInstanceUuid;

    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    @Column(name = "release_reason")
    private String releaseReason;
}
