package apps.sarafrika.elimika.timetabling.dto;

import apps.sarafrika.elimika.timetabling.spi.SchedulingStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(name = "OrganisationTimetableEntry",
        description = "One session of an organisation's class, carrying what a timetable cell shows without further lookups")
public record OrganisationTimetableEntryDTO(

        @Schema(description = "The scheduled instance", format = "uuid")
        @JsonProperty(value = "uuid", access = JsonProperty.Access.READ_ONLY)
        UUID uuid,

        @Schema(description = "The class this session belongs to", format = "uuid")
        @JsonProperty(value = "class_definition_uuid", access = JsonProperty.Access.READ_ONLY)
        UUID classDefinitionUuid,

        @Schema(description = "Current title of the class", example = "Grade 5 Piano - Term 2")
        @JsonProperty(value = "class_title", access = JsonProperty.Access.READ_ONLY)
        String classTitle,

        @Schema(description = "Instructor delivering the session", format = "uuid")
        @JsonProperty(value = "instructor_uuid", access = JsonProperty.Access.READ_ONLY)
        UUID instructorUuid,

        @Schema(description = "Display name of the instructor, null when the instructor no longer resolves",
                example = "Jane Wanjiku", nullable = true)
        @JsonProperty(value = "instructor_name", access = JsonProperty.Access.READ_ONLY)
        String instructorName,

        @Schema(description = "Session start", example = "2026-10-12T09:00:00", format = "date-time")
        @JsonProperty(value = "start_time", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime startTime,

        @Schema(description = "Session end", example = "2026-10-12T11:00:00", format = "date-time")
        @JsonProperty(value = "end_time", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime endTime,

        @Schema(description = "Timezone of the session", example = "Africa/Nairobi")
        @JsonProperty(value = "timezone", access = JsonProperty.Access.READ_ONLY)
        String timezone,

        @Schema(description = "Location type", example = "IN_PERSON")
        @JsonProperty(value = "location_type", access = JsonProperty.Access.READ_ONLY)
        String locationType,

        @Schema(description = "Location name", example = "Main Campus - Room 4", nullable = true)
        @JsonProperty(value = "location_name", access = JsonProperty.Access.READ_ONLY)
        String locationName,

        @Schema(description = "Seat capacity of the session", example = "20", nullable = true)
        @JsonProperty(value = "max_participants", access = JsonProperty.Access.READ_ONLY)
        Integer maxParticipants,

        @Schema(description = "Session status; cancelled sessions are never listed", example = "SCHEDULED")
        @JsonProperty(value = "status", access = JsonProperty.Access.READ_ONLY)
        SchedulingStatus status,

        @Schema(description = "Enrolments on the session, excluding cancelled and waitlisted ones", example = "12")
        @JsonProperty(value = "enrolled_count", access = JsonProperty.Access.READ_ONLY)
        long enrolledCount
) {
}
