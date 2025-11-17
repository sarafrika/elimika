package apps.sarafrika.elimika.timetabling.model;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import apps.sarafrika.elimika.timetabling.util.converter.SchedulingStatusConverter;
import apps.sarafrika.elimika.timetabling.spi.SchedulingStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a scheduled instance of a class.
 * <p>
 * This entity stores concrete class instances that have been placed on the calendar
 * by the timetabling module. It contains both references to external module data
 * (class definition, instructor) and denormalized information for performance.
 * <p>
 * The entity follows the project's BaseEntity pattern for consistent UUID and audit fields.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2024-09-05
 */
@Entity
@Table(name = "scheduled_instances")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledInstance extends BaseEntity {
    
    @Column(name = "class_definition_uuid")
    private UUID classDefinitionUuid;
    
    @Column(name = "instructor_uuid")
    private UUID instructorUuid;
    
    @Column(name = "start_time")
    private LocalDateTime startTime;
    
    @Column(name = "end_time")
    private LocalDateTime endTime;
    
    @Column(name = "timezone")
    private String timezone;
    
    @Column(name = "title")
    private String title;
    
    @Column(name = "location_type")
    private String locationType;

    @Column(name = "location_name")
    private String locationName;

    @Column(name = "location_latitude")
    private BigDecimal locationLatitude;

    @Column(name = "location_longitude")
    private BigDecimal locationLongitude;
    
    @Column(name = "max_participants")
    private Integer maxParticipants;
    
    @Column(name = "status")
    @Convert(converter = SchedulingStatusConverter.class)
    private SchedulingStatus status;
    
    @Column(name = "cancellation_reason")
    private String cancellationReason;
}
