package apps.sarafrika.elimika.resourcing.model;

import apps.sarafrika.elimika.resourcing.spi.ResourceBookingSourceType;
import apps.sarafrika.elimika.resourcing.spi.ResourceBookingStatus;
import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "resource_bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResourceBooking extends BaseEntity {

    @Column(name = "resource_uuid")
    private UUID resourceUuid;

    @Column(name = "organisation_uuid")
    private UUID organisationUuid;

    @Column(name = "status")
    private ResourceBookingStatus status;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "source_type")
    private ResourceBookingSourceType sourceType;

    @Column(name = "job_uuid")
    private UUID jobUuid;

    @Column(name = "class_definition_uuid")
    private UUID classDefinitionUuid;

    @Column(name = "scheduled_instance_uuid")
    private UUID scheduledInstanceUuid;

    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    @Column(name = "release_reason")
    private String releaseReason;
}
