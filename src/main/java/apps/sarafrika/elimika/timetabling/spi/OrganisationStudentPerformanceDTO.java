package apps.sarafrika.elimika.timetabling.spi;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A student's performance in one class, scoped to a single organisation.
 * <p>
 * An organisation may only see how a student is doing in <em>its own</em> courses and
 * classes. This shape exists so that constraint is expressed in the contract itself:
 * every row is anchored to a class the organisation owns, and there is no field through
 * which a student's learning elsewhere on the platform could surface.
 *
 * @param classDefinitionUuid the organisation's class
 * @param classTitle          title of that class
 * @param totalSessions       sessions the student is enrolled in (excludes cancelled/waitlisted)
 * @param attended            sessions marked attended
 * @param absent              sessions marked absent
 * @param attendanceRate      attended as a percentage of total sessions, 0 when there are none
 * @param lastSessionAt       most recent session, or null when none has been scheduled
 */
@Schema(
        name = "OrganisationStudentPerformance",
        description = "A student's performance in one of the organisation's own classes."
)
public record OrganisationStudentPerformanceDTO(

        @Schema(description = "The organisation's class", format = "uuid")
        @JsonProperty("class_definition_uuid") UUID classDefinitionUuid,

        @Schema(description = "Title of the class", example = "Beginner Piano")
        @JsonProperty("class_title") String classTitle,

        @Schema(description = "Sessions enrolled in, excluding cancelled and waitlisted", example = "12")
        @JsonProperty("total_sessions") long totalSessions,

        @Schema(description = "Sessions attended", example = "10")
        @JsonProperty("attended") long attended,

        @Schema(description = "Sessions missed", example = "2")
        @JsonProperty("absent") long absent,

        @Schema(description = "Attended as a percentage of total sessions", example = "83.3")
        @JsonProperty("attendance_rate") double attendanceRate,

        @Schema(description = "Most recent session", nullable = true)
        @JsonProperty("last_session_at") LocalDateTime lastSessionAt
) {
}
