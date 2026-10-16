package apps.sarafrika.elimika.course.dto;

import apps.sarafrika.elimika.course.util.enums.CourseResultStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(name = "CourseProgressReport", description = "One learner's lesson-by-lesson progress report: what was taught, attendance, marks and the final result")
public record CourseProgressReportDTO(
        @JsonProperty("course_uuid") UUID courseUuid,
        @JsonProperty("course_name") String courseName,
        @JsonProperty("enrollment_uuid") UUID enrollmentUuid,
        @JsonProperty("student_uuid") UUID studentUuid,
        @Schema(description = "Weighted final grade as a percentage of graded work; ungraded lessons are not counted")
        @JsonProperty("final_grade") BigDecimal finalGrade,
        @JsonProperty("final_grade_label") String finalGradeLabel,
        @JsonProperty("result_status") CourseResultStatus resultStatus,
        @JsonProperty("pass_mark") BigDecimal passMark,
        @JsonProperty("progress_percentage") BigDecimal progressPercentage,
        @Schema(description = "Average lesson mark on the per-lesson component, e.g. 27.5")
        @JsonProperty("average_lesson_score") BigDecimal averageLessonScore,
        @Schema(description = "What each lesson is marked out of, e.g. 30")
        @JsonProperty("lesson_max_score") BigDecimal lessonMaxScore,
        @JsonProperty("instructor_comments") String instructorComments,
        @JsonProperty("lessons") List<LessonRowDTO> lessons
) {

    public record LessonRowDTO(
            @JsonProperty("lesson_uuid") UUID lessonUuid,
            @JsonProperty("lesson_number") Integer lessonNumber,
            @JsonProperty("lesson_title") String lessonTitle,
            @JsonProperty("topic_covered") String topicCovered,
            @JsonProperty("instructor_remarks") String instructorRemarks,
            @JsonProperty("assignments") List<String> assignments,
            @JsonProperty("session_start") LocalDateTime sessionStart,
            @JsonProperty("duration_minutes") Long durationMinutes,
            @Schema(description = "ATTENDED, ABSENT, or null when not marked")
            @JsonProperty("attendance") String attendance,
            @JsonProperty("instrument_condition") String instrumentCondition,
            @JsonProperty("marks") List<MarkDTO> marks
    ) {
    }

    public record MarkDTO(
            @JsonProperty("line_item_uuid") UUID lineItemUuid,
            @JsonProperty("component") String component,
            @Schema(description = "graded, draft or ungraded; a draft's marks are never shown")
            @JsonProperty("status") String status,
            @JsonProperty("score") BigDecimal score,
            @JsonProperty("max_score") BigDecimal maxScore,
            @JsonProperty("comments") String comments,
            @JsonProperty("criteria") List<CourseAssessmentGradeRevisionDTO.CriterionDTO> criteria,
            @JsonProperty("revision_count") Integer revisionCount,
            @JsonProperty("graded_at") LocalDateTime gradedAt
    ) {
    }
}
