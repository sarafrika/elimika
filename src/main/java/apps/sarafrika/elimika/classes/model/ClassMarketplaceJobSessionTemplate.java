package apps.sarafrika.elimika.classes.model;

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
import java.util.UUID;

@Entity
@Table(name = "class_marketplace_job_session_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClassMarketplaceJobSessionTemplate extends BaseEntity {

    @Column(name = "job_uuid")
    private UUID jobUuid;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "recurrence_type")
    private String recurrenceType;

    @Column(name = "interval_value")
    private Integer intervalValue;

    @Column(name = "days_of_week")
    private String daysOfWeek;

    @Column(name = "day_of_month")
    private Integer dayOfMonth;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "occurrence_count")
    private Integer occurrenceCount;

    @Column(name = "conflict_resolution")
    private String conflictResolution;
}
