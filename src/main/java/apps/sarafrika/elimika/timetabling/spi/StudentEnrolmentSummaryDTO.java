package apps.sarafrika.elimika.timetabling.spi;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * A student's enrolment/attendance summary within one organisation. "Completed" is
 * derived from attendance (status = ATTENDED); "total" excludes cancelled and
 * waitlisted enrolments.
 *
 * @param studentUuid the student
 * @param total       active enrolments (not cancelled / waitlisted)
 * @param completed   enrolments the student attended
 */
@Schema(description = "A student's enrolment/attendance summary within an organisation")
public record StudentEnrolmentSummaryDTO(
        @Schema(description = "Student UUID", format = "uuid")
        @JsonProperty("student_uuid") UUID studentUuid,

        @Schema(description = "Active enrolments (excludes cancelled/waitlisted)", example = "4")
        @JsonProperty("total") long total,

        @Schema(description = "Enrolments attended", example = "3")
        @JsonProperty("completed") long completed
) {
}
