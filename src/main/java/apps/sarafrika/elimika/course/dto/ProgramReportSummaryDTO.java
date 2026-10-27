package apps.sarafrika.elimika.course.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(name = "ProgramReportSummary", description = "One programme a learner is enrolled in, with their current mark and result")
public record ProgramReportSummaryDTO(
        @JsonProperty("program_uuid") UUID programUuid,
        @JsonProperty("program_title") String programTitle,
        @JsonProperty("program_enrollment_uuid") UUID programEnrollmentUuid,
        @JsonProperty("final_grade") BigDecimal finalGrade,
        @JsonProperty("final_grade_provisional") boolean finalGradeProvisional,
        @Schema(description = "IN_PROGRESS, PASSED or FAILED")
        @JsonProperty("result_status") String resultStatus
) {
}
