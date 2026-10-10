package apps.sarafrika.elimika.classes.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(
        name = "ClassAssessmentSchedules",
        description = "Assignment and quiz schedules for several classes at once; each row carries its class_definition_uuid"
)
public record ClassAssessmentSchedulesDTO(

        @Schema(description = "Assignment schedules of every visible requested class")
        @JsonProperty("assignment_schedules")
        List<ClassAssignmentScheduleDTO> assignmentSchedules,

        @Schema(description = "Quiz schedules of every visible requested class")
        @JsonProperty("quiz_schedules")
        List<ClassQuizScheduleDTO> quizSchedules
) {
}
