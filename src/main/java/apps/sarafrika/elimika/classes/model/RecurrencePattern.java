package apps.sarafrika.elimika.classes.model;

import apps.sarafrika.elimika.classes.util.enums.RecurrenceType;
import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "recurrence_patterns")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RecurrencePattern extends BaseEntity {

    @Column(name = "recurrence_type")
    private RecurrenceType recurrenceType;

    @Column(name = "interval_value")
    private Integer intervalValue = 1;

    @Column(name = "days_of_week")
    private String daysOfWeek;

    @Column(name = "day_of_month")
    private Integer dayOfMonth;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "occurrence_count")
    private Integer occurrenceCount;
}