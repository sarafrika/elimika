package apps.sarafrika.elimika.classes.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * What an organisation owes a single instructor for delivered class sessions.
 * <p>
 * Aggregated from persisted obligation rows, one per delivered session, each carrying the training
 * fee that stood on the day it was delivered. It is no longer
 * {@code current_training_fee x completed_sessions} evaluated on request — that number changed
 * retroactively whenever a class was re-rated, and had no notion of the money having been paid.
 * <p>
 * The four original fields keep their names and positions because the organisation revenue screen
 * reads them. {@code amount_owed} now means <em>outstanding</em>: settled sessions leave it and
 * appear under {@code amount_settled}, which is what "owed" always should have meant.
 *
 * @author Wilfred Njuguna
 * @version 2.0
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

        @Schema(description = "Still outstanding: delivered sessions that have not been settled")
        @JsonProperty("amount_owed")
        BigDecimal amountOwed,

        @Schema(description = "Number of the organisation's classes that generated these obligations")
        @JsonProperty("class_count")
        long classCount,

        @Schema(description = "Total delivered sessions behind these obligations, settled or not")
        @JsonProperty("session_count")
        long sessionCount,

        @Schema(description = "Currency the obligations were accrued in", example = "KES")
        @JsonProperty("currency_code")
        String currencyCode,

        @Schema(description = "Already recorded as paid off-platform by the organisation")
        @JsonProperty("amount_settled")
        BigDecimal amountSettled,

        @Schema(description = "Lifetime total earned: outstanding plus settled")
        @JsonProperty("amount_accrued")
        BigDecimal amountAccrued,

        @Schema(description = "Of the delivered sessions, the ones still unpaid")
        @JsonProperty("outstanding_session_count")
        long outstandingSessionCount

) {
}
