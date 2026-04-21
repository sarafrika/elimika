package apps.sarafrika.elimika.timetabling.spi;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(
        name = "StudentEnrollmentOverview",
        description = "Aggregated course and class enrollments for a student"
)
public record StudentEnrollmentOverviewDTO(

        @Schema(description = "Student identifier", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("student_uuid")
        UUID student_uuid,

        @Schema(description = "Overall class enrollments grouped by class definition")
        @JsonProperty("class_enrollments")
        List<StudentClassEnrollmentSummaryDTO> class_enrollments,

        @Schema(description = "Overall course enrollments independent of scheduled instances")
        @JsonProperty("course_enrollments")
        List<StudentCourseEnrollmentSummaryDTO> course_enrollments
) {
}
