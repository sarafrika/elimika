package apps.sarafrika.elimika.classes.model;

import apps.sarafrika.elimika.classes.util.converter.ClassRecurrenceTypeConverter;
import apps.sarafrika.elimika.classes.util.converter.ConflictResolutionStrategyConverter;
import apps.sarafrika.elimika.classes.util.enums.ClassRecurrenceType;
import apps.sarafrika.elimika.classes.util.enums.ConflictResolutionStrategy;
import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
@Table(name = "class_session_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClassSessionTemplate extends BaseEntity {

    @Column(name = "class_definition_uuid")
    private UUID classDefinitionUuid;

    @Column(name = "template_order")
    private Integer templateOrder;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "recurrence_type")
    @Convert(converter = ClassRecurrenceTypeConverter.class)
    private ClassRecurrenceType recurrenceType;

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
    @Convert(converter = ConflictResolutionStrategyConverter.class)
    private ConflictResolutionStrategy conflictResolution;
}
