package apps.sarafrika.elimika.timetabling.spi;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Enrollment Request Data Transfer Object
 * <p>
 * Request object for enrolling a student in a scheduled instance. Contains the necessary
 * information to create a new enrollment in the timetabling system.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2024-09-05
 */
@Schema(
        name = "EnrollmentRequest",
        description = "Request to enroll a student in a scheduled class instance",
        example = """
        {
            "scheduled_instance_uuid": "si123456-7890-abcd-ef01-234567890abc",
            "student_uuid": "st123456-7890-abcd-ef01-234567890abc"
        }
        """
)
public record EnrollmentRequestDTO(

        @Schema(
                description = "**[REQUIRED]** Reference to the scheduled instance UUID to enroll in.",
                example = "si123456-7890-abcd-ef01-234567890abc",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Scheduled instance UUID is required")
        @JsonProperty("scheduled_instance_uuid")
        UUID scheduledInstanceUuid,

        @Schema(
                description = "**[REQUIRED]** Reference to the student UUID who is enrolling.",
                example = "st123456-7890-abcd-ef01-234567890abc",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Student UUID is required")
        @JsonProperty("student_uuid")
        UUID studentUuid

) {}