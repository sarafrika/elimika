package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(name = "CourseEvaluationPlanCell", description = "Turns one lesson x component cell on (with its rubric) or off")
public record CourseEvaluationPlanCellRequest(

        @NotNull
        @JsonProperty("lesson_uuid")
        UUID lessonUuid,

        @NotNull
        @JsonProperty("assessment_uuid")
        UUID assessmentUuid,

        @Schema(description = "false removes the cell from the plan (\"None\")")
        @JsonProperty("enabled")
        boolean enabled,

        @JsonProperty("rubric_uuid")
        UUID rubricUuid,

        @Schema(description = "Optional quiz from the same lesson that this cell grades")
        @JsonProperty("quiz_uuid")
        UUID quizUuid,

        @Schema(description = "Optional assignment from the same lesson that this cell grades")
        @JsonProperty("assignment_uuid")
        UUID assignmentUuid
) {
}
