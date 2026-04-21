package apps.sarafrika.elimika.timetabling.spi;

import apps.sarafrika.elimika.shared.dto.PagedDTO;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(
        name = "StudentEnrollmentOverview",
        description = "Aggregated course and class enrollments for a student"
)
public record StudentEnrollmentOverviewDTO(

        @Schema(description = "Student identifier", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("student_uuid")
        UUID student_uuid,

        @Schema(description = "Paged overall class enrollments grouped by class definition")
        @JsonProperty("class_enrollments")
        PagedDTO<StudentClassEnrollmentSummaryDTO> class_enrollments,

        @Schema(description = "Paged overall course enrollments independent of scheduled instances")
        @JsonProperty("course_enrollments")
        PagedDTO<StudentCourseEnrollmentSummaryDTO> course_enrollments
) {
}
