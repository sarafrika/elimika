package apps.sarafrika.elimika.timetabling.spi;

import apps.sarafrika.elimika.timetabling.spi.EnrollmentStatus;
import apps.sarafrika.elimika.timetabling.spi.SchedulingStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Student Schedule Data Transfer Object
 * <p>
 * Represents a student's view of their scheduled classes, combining scheduled instance
 * information with enrollment details for a comprehensive schedule view.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2024-09-05
 */
@Schema(
        name = "StudentSchedule",
        description = "A student's view of their scheduled classes with enrollment information",
        example = """
        {
            "enrollment_uuid": "en123456-7890-abcd-ef01-234567890abc",
            "scheduled_instance_uuid": "si123456-7890-abcd-ef01-234567890abc",
            "class_definition_uuid": "cd123456-7890-abcd-ef01-234567890abc",
            "instructor_uuid": "inst1234-5678-90ab-cdef-123456789abc",
            "title": "Introduction to Java Programming",
            "start_time": "2024-09-15T09:00:00",
            "end_time": "2024-09-15T10:30:00",
            "timezone": "UTC",
            "location_type": "IN_PERSON",
            "location_name": "Nairobi HQ – Room 101",
            "location_latitude": -1.292066,
            "location_longitude": 36.821945,
            "scheduling_status": "SCHEDULED",
            "enrollment_status": "ENROLLED",
            "attendance_marked_at": null
        }
        """
)
public record StudentScheduleDTO(

        @Schema(
                description = "**[READ-ONLY]** Unique system identifier for the enrollment.",
                example = "en123456-7890-abcd-ef01-234567890abc",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty("enrollment_uuid")
        UUID enrollmentUuid,

        @Schema(
                description = "**[READ-ONLY]** Reference to the scheduled instance.",
                example = "si123456-7890-abcd-ef01-234567890abc",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty("scheduled_instance_uuid")
        UUID scheduledInstanceUuid,

        @Schema(
                description = "**[READ-ONLY]** Reference to the class definition.",
                example = "cd123456-7890-abcd-ef01-234567890abc",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty("class_definition_uuid")
        UUID classDefinitionUuid,

        @Schema(
                description = "**[READ-ONLY]** Reference to the instructor.",
                example = "inst1234-5678-90ab-cdef-123456789abc",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty("instructor_uuid")
        UUID instructorUuid,

        @Schema(
                description = "**[READ-ONLY]** Title of the scheduled class.",
                example = "Introduction to Java Programming",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty("title")
        String title,

        @Schema(
                description = "**[READ-ONLY]** Start date and time of the scheduled class.",
                example = "2024-09-15T09:00:00",
                format = "date-time",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty("start_time")
        LocalDateTime startTime,

        @Schema(
                description = "**[READ-ONLY]** End date and time of the scheduled class.",
                example = "2024-09-15T10:30:00",
                format = "date-time",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty("end_time")
        LocalDateTime endTime,

        @Schema(
                description = "**[READ-ONLY]** Timezone for the scheduled class.",
                example = "UTC",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty("timezone")
        String timezone,

        @Schema(
                description = "**[READ-ONLY]** Location type for the class.",
                example = "IN_PERSON",
                allowableValues = {"ONLINE", "IN_PERSON", "HYBRID"},
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty("location_type")
        String locationType,

        @Schema(
                description = "**[READ-ONLY]** Human-readable location name for the scheduled class.",
                example = "Nairobi HQ – Room 101",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty("location_name")
        String locationName,

        @Schema(
                description = "**[READ-ONLY]** Latitude coordinate for the scheduled class location.",
                example = "-1.292066",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty("location_latitude")
        BigDecimal locationLatitude,

        @Schema(
                description = "**[READ-ONLY]** Longitude coordinate for the scheduled class location.",
                example = "36.821945",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty("location_longitude")
        BigDecimal locationLongitude,

        @Schema(
                description = "**[READ-ONLY]** Current status of the scheduled instance.",
                example = "SCHEDULED",
                allowableValues = {"SCHEDULED", "ONGOING", "COMPLETED", "CANCELLED"},
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty("scheduling_status")
        SchedulingStatus schedulingStatus,

        @Schema(
                description = "**[READ-ONLY]** Current enrollment status for the student.",
                example = "ENROLLED",
                allowableValues = {"ENROLLED", "ATTENDED", "ABSENT", "CANCELLED"},
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty("enrollment_status")
        EnrollmentStatus enrollmentStatus,

        @Schema(
                description = "**[READ-ONLY]** Timestamp when attendance was marked (if applicable).",
                example = "2024-09-15T09:15:00",
                format = "date-time",
                nullable = true,
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty("attendance_marked_at")
        LocalDateTime attendanceMarkedAt

) {

    /**
     * Returns the duration of this scheduled class in minutes.
     *
     * @return Duration in minutes
     */
    @JsonProperty(value = "duration_minutes", access = JsonProperty.Access.READ_ONLY)
    @Schema(
            description = "**[READ-ONLY]** Duration of the scheduled class in minutes.",
            example = "90",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    public long getDurationMinutes() {
        if (startTime == null || endTime == null) {
            return 0;
        }
        return java.time.Duration.between(startTime, endTime).toMinutes();
    }

    /**
     * Checks if the student attended this class.
     *
     * @return true if attended, false otherwise
     */
    @JsonProperty(value = "did_attend", access = JsonProperty.Access.READ_ONLY)
    @Schema(
            description = "**[READ-ONLY]** Indicates if the student attended this class.",
            example = "false",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    public boolean didAttend() {
        return EnrollmentStatus.ATTENDED.equals(enrollmentStatus);
    }

    /**
     * Checks if this class is upcoming (scheduled and in future).
     *
     * @return true if upcoming, false otherwise
     */
    @JsonProperty(value = "is_upcoming", access = JsonProperty.Access.READ_ONLY)
    @Schema(
            description = "**[READ-ONLY]** Indicates if this class is upcoming.",
            example = "true",
            accessMode = Schema.AccessMode.READ_ONLY
    )
    public boolean isUpcoming() {
        return SchedulingStatus.SCHEDULED.equals(schedulingStatus) && 
               startTime != null && 
               startTime.isAfter(LocalDateTime.now());
    }
}
