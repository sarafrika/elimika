package apps.sarafrika.elimika.availability.dto;

import apps.sarafrika.elimika.shared.validation.ValidTimeRange;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for booking an instructor's available time slot.
 * Used by students to reserve time with an instructor.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2025-10-10
 */
@Schema(
        name = "InstructorSlotBookingRequest",
        description = "Request to book an available instructor time slot",
        example = """
        {
            "instructor_uuid": "inst1234-5678-90ab-cdef-123456789abc",
            "start_time": "2025-10-15T10:00:00",
            "end_time": "2025-10-15T11:00:00",
            "purpose": "One-on-one tutoring session for Java programming",
            "student_uuid": "stud1234-5678-90ab-cdef-123456789abc"
        }
        """
)
@ValidTimeRange(
        startField = "startTime",
        endField = "endTime",
        message = "Booking end time must be after start time"
)
public record InstructorSlotBookingRequestDTO(

        @Schema(
                description = "**[REQUIRED]** UUID of the instructor to book",
                example = "inst1234-5678-90ab-cdef-123456789abc",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Instructor UUID is required")
        @JsonProperty("instructor_uuid")
        UUID instructorUuid,

        @Schema(
                description = "**[REQUIRED]** Start date and time for the booking",
                example = "2025-10-15T10:00:00",
                format = "date-time",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Start time is required")
        @JsonProperty("start_time")
        LocalDateTime startTime,

        @Schema(
                description = "**[REQUIRED]** End date and time for the booking",
                example = "2025-10-15T11:00:00",
                format = "date-time",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "End time is required")
        @JsonProperty("end_time")
        LocalDateTime endTime,

        @Schema(
                description = "**[OPTIONAL]** Purpose or note for this booking",
                example = "One-on-one tutoring session for Java programming",
                maxLength = 500,
                nullable = true,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @Size(max = 500, message = "Purpose must not exceed 500 characters")
        @JsonProperty("purpose")
        String purpose,

        @Schema(
                description = "**[OPTIONAL]** UUID of the student making the booking (can be inferred from auth context)",
                example = "stud1234-5678-90ab-cdef-123456789abc",
                nullable = true,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @JsonProperty("student_uuid")
        UUID studentUuid
) {
}