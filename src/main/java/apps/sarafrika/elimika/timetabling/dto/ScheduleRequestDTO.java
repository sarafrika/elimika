package apps.sarafrika.elimika.timetabling.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Schedule Request Data Transfer Object
 * <p>
 * Request object for scheduling a class instance. Contains the necessary information
 * to create a new scheduled instance in the timetabling system.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2024-09-05
 */
@Schema(
        name = "ScheduleRequest",
        description = "Request to schedule a new class instance",
        example = """
        {
            "class_definition_uuid": "cd123456-7890-abcd-ef01-234567890abc",
            "instructor_uuid": "inst1234-5678-90ab-cdef-123456789abc",
            "start_time": "2024-09-15T09:00:00",
            "end_time": "2024-09-15T10:30:00",
            "timezone": "UTC"
        }
        """
)
public record ScheduleRequestDTO(

        @Schema(
                description = "**[REQUIRED]** Reference to the class definition UUID to schedule.",
                example = "cd123456-7890-abcd-ef01-234567890abc",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Class definition UUID is required")
        @JsonProperty("class_definition_uuid")
        UUID classDefinitionUuid,

        @Schema(
                description = "**[REQUIRED]** Reference to the instructor UUID who will conduct the session.",
                example = "inst1234-5678-90ab-cdef-123456789abc",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Instructor UUID is required")
        @JsonProperty("instructor_uuid")
        UUID instructorUuid,

        @Schema(
                description = "**[REQUIRED]** Start date and time for the scheduled session.",
                example = "2024-09-15T09:00:00",
                format = "date-time",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Start time is required")
        @JsonProperty("start_time")
        LocalDateTime startTime,

        @Schema(
                description = "**[REQUIRED]** End date and time for the scheduled session.",
                example = "2024-09-15T10:30:00",
                format = "date-time",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "End time is required")
        @JsonProperty("end_time")
        LocalDateTime endTime,

        @Schema(
                description = "**[OPTIONAL]** Timezone for the scheduled session. Defaults to UTC.",
                example = "UTC",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @JsonProperty("timezone")
        String timezone

) {}