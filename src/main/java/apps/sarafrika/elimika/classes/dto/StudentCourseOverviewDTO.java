package apps.sarafrika.elimika.classes.dto;

import apps.sarafrika.elimika.shared.dto.PagedDTO;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(
        name = "StudentCourseOverview",
        description = "Everything a learner's course list needs in one response."
)
public record StudentCourseOverviewDTO(

        @Schema(description = "Student identifier", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("student_uuid")
        UUID studentUuid,

        @Schema(description = "Enrolled classes, most recently active first")
        @JsonProperty("enrollments")
        PagedDTO<StudentCourseOverviewItemDTO> enrollments
) {
}
