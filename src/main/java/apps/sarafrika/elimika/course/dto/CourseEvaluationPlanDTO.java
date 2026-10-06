package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Schema(name = "CourseEvaluationPlan",
        description = "Lessons by per-lesson assessment components; each cell is the line item grading that lesson, or null")
public record CourseEvaluationPlanDTO(

        @JsonProperty("course_uuid")
        UUID courseUuid,

        @JsonProperty("pass_mark")
        BigDecimal passMark,

        @JsonProperty("components")
        List<Component> components,

        @JsonProperty("lessons")
        List<LessonRow> lessons
) {

    public record Component(
            @JsonProperty("assessment_uuid") UUID assessmentUuid,
            @JsonProperty("title") String title,
            @JsonProperty("assessment_type") String assessmentType,
            @JsonProperty("weight_percentage") BigDecimal weightPercentage,
            @JsonProperty("rubric_uuid") UUID rubricUuid,
            @JsonProperty("sync_class_attendance") Boolean syncClassAttendance
    ) {
    }

    public record LessonRow(
            @JsonProperty("lesson_uuid") UUID lessonUuid,
            @JsonProperty("lesson_number") Integer lessonNumber,
            @JsonProperty("title") String title,
            @Schema(description = "One entry per component, in component order; null where the lesson is not graded")
            @JsonProperty("cells") List<CourseAssessmentLineItemDTO> cells
    ) {
    }
}
