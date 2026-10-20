package apps.sarafrika.elimika.timetabling.spi;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(name = "ClassReport", description = "A class across all its sessions: each learner's lesson marks per session, their average and band")
public record ClassReportDTO(
        @JsonProperty("class_definition_uuid") UUID classDefinitionUuid,
        @JsonProperty("class_title") String classTitle,
        @Schema(description = "False when the viewer only sees submitted session reports")
        @JsonProperty("live") boolean live,
        @JsonProperty("sessions") List<SessionColumnDTO> sessions,
        @JsonProperty("students") List<StudentRowDTO> students
) {

    public record SessionColumnDTO(
            @JsonProperty("instance_uuid") UUID instanceUuid,
            @JsonProperty("start_time") LocalDateTime startTime,
            @JsonProperty("lesson_number") Integer lessonNumber,
            @JsonProperty("lesson_title") String lessonTitle,
            @JsonProperty("report_status") SessionReportStatus reportStatus,
            @JsonProperty("overdue") boolean overdue,
            @JsonProperty("class_average") BigDecimal classAverage,
            @JsonProperty("max_score") BigDecimal maxScore,
            @JsonProperty("attendance_rate") BigDecimal attendanceRate
    ) {
    }

    public record StudentRowDTO(
            @JsonProperty("student_uuid") UUID studentUuid,
            @JsonProperty("cells") List<CellDTO> cells,
            @JsonProperty("sessions_scored") int sessionsScored,
            @JsonProperty("average") BigDecimal average,
            @JsonProperty("average_percentage") BigDecimal averagePercentage,
            @JsonProperty("band") String band,
            @JsonProperty("attended") int attended,
            @JsonProperty("absent") int absent
    ) {
    }

    /** score_status is none, draft or submitted; attendance is ATTENDED, ABSENT, NOT_MARKED or NOT_ENROLLED. */
    public record CellDTO(
            @JsonProperty("instance_uuid") UUID instanceUuid,
            @JsonProperty("attendance") String attendance,
            @JsonProperty("score_status") String scoreStatus,
            @JsonProperty("score") BigDecimal score
    ) {
    }
}
