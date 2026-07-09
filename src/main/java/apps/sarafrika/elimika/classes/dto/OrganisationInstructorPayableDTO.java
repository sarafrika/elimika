package apps.sarafrika.elimika.classes.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * What an organisation owes a single instructor for delivered class sessions.
 * <p>
 * Amount is accrued from the per-class {@code training_fee} multiplied by the number
 * of completed sessions across the organisation's classes assigned to that instructor.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2026-07-10
 */
@Schema(
        name = "OrganisationInstructorPayable",
        description = "Amount an organisation owes an instructor for delivered class sessions"
)
public record OrganisationInstructorPayableDTO(

        @Schema(description = "UUID of the instructor owed")
        @JsonProperty("instructor_uuid")
        UUID instructorUuid,

        @Schema(description = "Total amount owed = sum(training_fee x completed sessions)")
        @JsonProperty("amount_owed")
        BigDecimal amountOwed,

        @Schema(description = "Number of the organisation's classes assigned to this instructor")
        @JsonProperty("class_count")
        long classCount,

        @Schema(description = "Total completed sessions across those classes")
        @JsonProperty("session_count")
        long sessionCount

) {
}
