package apps.sarafrika.elimika.timetabling.spi;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(
        name = "StudentClassEnrollmentSummary",
        description = "Overall class enrollment summary for a student, grouped by class definition"
)
public record StudentClassEnrollmentSummaryDTO(

        @Schema(description = "Class definition identifier", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("class_definition_uuid")
        UUID class_definition_uuid,

        @Schema(description = "Class definition title")
        @JsonProperty("class_title")
        String class_title,

        @Schema(description = "Most recent scheduled-instance enrollment identifier for this class")
        @JsonProperty("latest_enrollment_uuid")
        UUID latest_enrollment_uuid,

        @Schema(description = "Most recent scheduled-instance enrollment status for this class")
        @JsonProperty("latest_enrollment_status")
        EnrollmentStatus latest_enrollment_status,

        @Schema(description = "Number of scheduled-instance enrollments aggregated under this class")
        @JsonProperty("scheduled_instance_count")
        int scheduled_instance_count,

        @Schema(description = "Latest scheduled instance start time found for this class", format = "date-time")
        @JsonProperty("latest_scheduled_instance_start_time")
        LocalDateTime latest_scheduled_instance_start_time,

        @Schema(description = "Most recent class enrollment activity timestamp", format = "date-time")
        @JsonProperty("latest_activity_date")
        LocalDateTime latest_activity_date
) {
}
