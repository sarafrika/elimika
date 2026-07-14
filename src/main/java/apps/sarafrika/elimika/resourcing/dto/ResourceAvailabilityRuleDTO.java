package apps.sarafrika.elimika.resourcing.dto;

import apps.sarafrika.elimika.resourcing.spi.AvailabilityRuleType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Schema(
        name = "ResourceAvailabilityRule",
        description = """
                Calendar rule for a resource. Recurring rules use start_time/end_time (with optional
                days_of_week and effective dates); one-off BLACKOUT rules use specific_start/specific_end.
                A resource with no OPEN_HOURS rules is open at all times."""
)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResourceAvailabilityRuleDTO(

        @Schema(description = "**[READ-ONLY]** Unique identifier of the rule", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "uuid", access = JsonProperty.Access.READ_ONLY)
        UUID uuid,

        @Schema(description = "**[READ-ONLY]** Resource the rule belongs to (taken from the request path)", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "resource_uuid", access = JsonProperty.Access.READ_ONLY)
        UUID resourceUuid,

        @Schema(description = "Rule kind", example = "OPEN_HOURS", allowableValues = {"OPEN_HOURS", "BLACKOUT"})
        @NotNull(message = "Rule type is required")
        @JsonProperty("rule_type")
        AvailabilityRuleType ruleType,

        @Schema(description = "Comma separated day names the recurring rule applies to; empty = every day", example = "MONDAY,WEDNESDAY,FRIDAY", nullable = true)
        @JsonProperty("days_of_week")
        String daysOfWeek,

        @Schema(description = "Daily window start (recurring rules)", example = "08:00:00", nullable = true)
        @JsonProperty("start_time")
        LocalTime startTime,

        @Schema(description = "Daily window end (recurring rules)", example = "18:00:00", nullable = true)
        @JsonProperty("end_time")
        LocalTime endTime,

        @Schema(description = "One-off window start (BLACKOUT only)", nullable = true)
        @JsonProperty("specific_start")
        LocalDateTime specificStart,

        @Schema(description = "One-off window end (BLACKOUT only)", nullable = true)
        @JsonProperty("specific_end")
        LocalDateTime specificEnd,

        @Schema(description = "First date the recurring rule applies; null = unbounded", nullable = true)
        @JsonProperty("effective_start_date")
        LocalDate effectiveStartDate,

        @Schema(description = "Last date the recurring rule applies (inclusive); null = unbounded", nullable = true)
        @JsonProperty("effective_end_date")
        LocalDate effectiveEndDate,

        @Schema(description = "Free-form notes (e.g. 'Public holiday')", nullable = true)
        @JsonProperty("notes")
        String notes
) {
}
