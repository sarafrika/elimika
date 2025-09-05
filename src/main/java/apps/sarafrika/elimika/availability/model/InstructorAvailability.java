package apps.sarafrika.elimika.availability.model;

import apps.sarafrika.elimika.availability.util.converter.AvailabilityTypeConverter;
import apps.sarafrika.elimika.availability.util.enums.AvailabilityType;
import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "instructor_availability")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InstructorAvailability extends BaseEntity {
    
    @Column(name = "instructor_uuid")
    private UUID instructorUuid;
    
    @Column(name = "availability_type")
    @Convert(converter = AvailabilityTypeConverter.class)
    private AvailabilityType availabilityType;
    
    @Column(name = "day_of_week")
    private Integer dayOfWeek;
    
    @Column(name = "day_of_month")
    private Integer dayOfMonth;
    
    @Column(name = "specific_date")
    private LocalDate specificDate;
    
    @Column(name = "start_time")
    private LocalTime startTime;
    
    @Column(name = "end_time")
    private LocalTime endTime;
    
    @Column(name = "custom_pattern")
    private String customPattern;
    
    @Column(name = "is_available")
    private Boolean isAvailable = true;
    
    @Column(name = "recurrence_interval")
    private Integer recurrenceInterval;
    
    @Column(name = "effective_start_date")
    private LocalDate effectiveStartDate;
    
    @Column(name = "effective_end_date")
    private LocalDate effectiveEndDate;
}