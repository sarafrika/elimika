package apps.sarafrika.elimika.timetabling.spi;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(name = "SessionReport", description = "One class session's report: what was taught, each learner's attendance and lesson marks, class aggregates and what still blocks submission")
public record SessionReportDTO(
        @JsonProperty("report_uuid") UUID reportUuid,
        @JsonProperty("instance_uuid") UUID instanceUuid,
        @JsonProperty("class_definition_uuid") UUID classDefinitionUuid,
        @JsonProperty("class_title") String classTitle,
        @JsonProperty("course_uuid") UUID courseUuid,
        @JsonProperty("lesson_uuid") UUID lessonUuid,
        @JsonProperty("lesson_number") Integer lessonNumber,
        @JsonProperty("lesson_title") String lessonTitle,
        @JsonProperty("instructor_uuid") UUID instructorUuid,
        @JsonProperty("start_time") LocalDateTime startTime,
        @JsonProperty("end_time") LocalDateTime endTime,
        @JsonProperty("duration_minutes") Long durationMinutes,
        @JsonProperty("session_ended") boolean sessionEnded,
        @JsonProperty("topic_covered") String topicCovered,
        @JsonProperty("instructor_remarks") String instructorRemarks,
        @Schema(description = "draft, submitted or locked")
        @JsonProperty("status") SessionReportStatus status,
        @JsonProperty("due_at") LocalDateTime dueAt,
        @JsonProperty("overdue") boolean overdue,
        @JsonProperty("submitted_at") LocalDateTime submittedAt,
        @JsonProperty("editable_until") LocalDateTime editableUntil,
        @JsonProperty("revision_count") int revisionCount,
        @Schema(description = "True when the live data differs from the last submitted revision")
        @JsonProperty("changed_since_submission") boolean changedSinceSubmission,
        @JsonProperty("rubric_scored") boolean rubricScored,
        @JsonProperty("max_score") BigDecimal maxScore,
        @JsonProperty("criteria") List<CriterionDTO> criteria,
        @JsonProperty("assignment_titles") List<String> assignmentTitles,
        @JsonProperty("rows") List<RowDTO> rows,
        @JsonProperty("aggregates") AggregatesDTO aggregates,
        @JsonProperty("checklist") List<ChecklistItemDTO> checklist,
        @JsonProperty("can_submit") boolean canSubmit
) {

    // Named apart from the grade revision's CriterionDTO so the two OpenAPI schemas never merge.
    @Schema(name = "SessionReportCriterion")
    public record CriterionDTO(@JsonProperty("criteria_uuid") UUID criteriaUuid, @JsonProperty("name") String name) {
    }

    public record RowDTO(
            @JsonProperty("student_uuid") UUID studentUuid,
            @JsonProperty("class_enrollment_uuid") UUID classEnrollmentUuid,
            @JsonProperty("course_enrollment_uuid") UUID courseEnrollmentUuid,
            @Schema(description = "ATTENDED, ABSENT, EXCUSED or NOT_MARKED")
            @JsonProperty("attendance") String attendance,
            @JsonProperty("instrument_condition") String instrumentCondition,
            @JsonProperty("attendance_comment") String attendanceComment,
            @Schema(description = "none, draft or submitted")
            @JsonProperty("score_status") String scoreStatus,
            @JsonProperty("score") BigDecimal score,
            @JsonProperty("max_score") BigDecimal maxScore,
            @JsonProperty("picks") List<PickDTO> picks,
            @JsonProperty("lesson_comment") String lessonComment,
            @JsonProperty("revision_count") int revisionCount,
            @JsonProperty("assignments") List<AssignmentDTO> assignments
    ) {
    }

    public record PickDTO(
            @JsonProperty("criteria_uuid") UUID criteriaUuid,
            @JsonProperty("criteria_name") String criteriaName,
            @JsonProperty("level_name") String levelName,
            @JsonProperty("points") BigDecimal points
    ) {
    }

    @Schema(name = "SessionReportAssignment")
    public record AssignmentDTO(
            @JsonProperty("title") String title,
            @JsonProperty("status") String status,
            @JsonProperty("score") BigDecimal score,
            @JsonProperty("max_score") BigDecimal maxScore
    ) {
    }

    public record AggregatesDTO(
            @JsonProperty("enrolled") int enrolled,
            @JsonProperty("present") int present,
            @JsonProperty("absent") int absent,
            @JsonProperty("not_marked") int notMarked,
            @JsonProperty("attendance_rate") BigDecimal attendanceRate,
            @JsonProperty("scored") int scored,
            @JsonProperty("class_average") BigDecimal classAverage,
            @JsonProperty("class_average_percentage") BigDecimal classAveragePercentage,
            @JsonProperty("highest") BigDecimal highest,
            @JsonProperty("lowest") BigDecimal lowest,
            @JsonProperty("criterion_averages") List<CriterionAverageDTO> criterionAverages,
            @JsonProperty("band_counts") List<BandCountDTO> bandCounts
    ) {
    }

    public record CriterionAverageDTO(
            @JsonProperty("criteria_uuid") UUID criteriaUuid,
            @JsonProperty("name") String name,
            @JsonProperty("average") BigDecimal average
    ) {
    }

    public record BandCountDTO(@JsonProperty("name") String name, @JsonProperty("count") int count) {
    }

    /** severity is blocker (stops submission) or info. */
    public record ChecklistItemDTO(
            @JsonProperty("code") String code,
            @JsonProperty("severity") String severity,
            @JsonProperty("student_uuid") UUID studentUuid,
            @JsonProperty("message") String message
    ) {
    }
}
