package apps.sarafrika.elimika.resourcing.model;

import apps.sarafrika.elimika.resourcing.spi.AvailabilityRuleType;
import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "resource_availability_rules")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResourceAvailabilityRule extends BaseEntity {

    @Column(name = "resource_uuid")
    private UUID resourceUuid;

    @Column(name = "rule_type")
    private AvailabilityRuleType ruleType;

    @Column(name = "days_of_week")
    private String daysOfWeek;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "specific_start")
    private LocalDateTime specificStart;

    @Column(name = "specific_end")
    private LocalDateTime specificEnd;

    @Column(name = "effective_start_date")
    private LocalDate effectiveStartDate;

    @Column(name = "effective_end_date")
    private LocalDate effectiveEndDate;

    @Column(name = "notes")
    private String notes;
}
