package apps.sarafrika.elimika.timetabling.spi;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(
        name = "StudentCourseEnrollmentSummary",
        description = "Overall course enrollment summary for a student"
)
public record StudentCourseEnrollmentSummaryDTO(

        @Schema(description = "Course enrollment identifier")
        @JsonProperty("enrollment_uuid")
        UUID enrollment_uuid,

        @Schema(description = "Course identifier", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("course_uuid")
        UUID course_uuid,

        @Schema(description = "Course name")
        @JsonProperty("course_name")
        String course_name,

        @Schema(description = "Course enrollment status")
        @JsonProperty("enrollment_status")
        String enrollment_status,

        @Schema(description = "Course progress percentage")
        @JsonProperty("progress_percentage")
        BigDecimal progress_percentage,

        @Schema(description = "Most recent course enrollment update time", format = "date-time")
        @JsonProperty("updated_date")
        LocalDateTime updated_date
) {
}
