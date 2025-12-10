package apps.sarafrika.elimika.classes.dto;

import apps.sarafrika.elimika.classes.util.enums.RecurrenceType;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(
        name = "ClassRecurrence",
        description = "Inline recurrence rule for class session templates (embedded within class creation)"
)
public record ClassRecurrenceDTO(

        @Schema(description = "Recurrence type to apply for the session template", example = "WEEKLY")
        @JsonProperty("recurrence_type")
        RecurrenceType recurrenceType,

        @Schema(description = "Interval between recurrences (e.g., every 2 weeks)", example = "1")
        @JsonProperty("interval_value")
        Integer intervalValue,

        @Schema(description = "Comma separated days of week (WEEKLY only). Example: MONDAY,WEDNESDAY", example = "MONDAY,WEDNESDAY")
        @JsonProperty("days_of_week")
        String daysOfWeek,

        @Schema(description = "Day of month to repeat on (MONTHLY only)", example = "15")
        @JsonProperty("day_of_month")
        Integer dayOfMonth,

        @Schema(description = "Optional end date (inclusive) for the recurrence series", example = "2025-12-31")
        @JsonProperty("end_date")
        LocalDate endDate,

        @Schema(description = "Number of occurrences to generate", example = "8")
        @JsonProperty("occurrence_count")
        Integer occurrenceCount
) {
}
