package apps.sarafrika.elimika.timetabling.dto;

import apps.sarafrika.elimika.timetabling.spi.EnrollmentStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

import java.util.Locale;

@Schema(name = "AttendanceRequest", description = "One learner's attendance for a session, with the instructor's rich-text comment")
public record AttendanceRequest(
        @Schema(description = "Whether the student attended (true) or was absent (false); ignored when attendance_status is set")
        @JsonProperty("attended")
        Boolean attended,

        @Schema(description = "ATTENDED, ABSENT or EXCUSED; excused sessions are left out of the learner's attendance",
                allowableValues = {"ATTENDED", "ABSENT", "EXCUSED"})
        @JsonProperty("attendance_status")
        String attendanceStatus,

        @Schema(description = "Condition of the learner's instrument: GOOD, NEEDS_REPAIR or NO_EQUIPMENT")
        @JsonProperty("instrument_condition")
        String instrumentCondition,

        @Schema(description = "The instructor's comment on this learner for the session (HTML)")
        @Size(max = 20000, message = "comment must be at most 20000 characters")
        @JsonProperty("comment")
        String comment
) {

    /** The explicit status when given, else the attended flag; one of the two is required. */
    public EnrollmentStatus resolvedStatus() {
        if (attendanceStatus != null && !attendanceStatus.isBlank()) {
            return EnrollmentStatus.fromValue(attendanceStatus.trim().toUpperCase(Locale.ROOT));
        }
        if (attended == null) {
            throw new IllegalArgumentException("Send attended or attendance_status");
        }
        return attended ? EnrollmentStatus.ATTENDED
                : EnrollmentStatus.ABSENT;
    }
}
