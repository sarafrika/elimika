package apps.sarafrika.elimika.payout.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * What an instructor is owed and what has been paid, broken down by the organisation that owes it.
 * <p>
 * An instructor may teach for several organisations at different rates, so a single total would be
 * unactionable — chasing a payment means knowing who owes it.
 */
@Schema(
        name = "InstructorStatement",
        description = "Per-organisation summary of what an instructor is owed and what has been settled"
)
public record InstructorStatementDTO(

        @Schema(description = "Platform user the statement belongs to")
        @JsonProperty("instructor_user_uuid")
        UUID instructorUserUuid,

        @Schema(description = "One line per organisation and currency")
        @JsonProperty("lines")
        List<Line> lines

) {

    @Schema(name = "InstructorStatementLine", description = "What one organisation owes this instructor in one currency")
    public record Line(

            @Schema(description = "Organisation that owes the money")
            @JsonProperty("organisation_uuid")
            UUID organisationUuid,

            @Schema(description = "Currency the obligations were accrued in")
            @JsonProperty("currency_code")
            String currencyCode,

            @Schema(description = "Still owed: accrued, not settled, not cancelled, not disputed")
            @JsonProperty("amount_outstanding")
            BigDecimal amountOutstanding,

            @Schema(description = "Recorded as paid off-platform by the organisation")
            @JsonProperty("amount_settled")
            BigDecimal amountSettled,

            @Schema(description = "Lifetime total: outstanding plus settled")
            @JsonProperty("amount_accrued")
            BigDecimal amountAccrued,

            @Schema(description = "Sessions that generated these obligations")
            @JsonProperty("session_count")
            long sessionCount,

            @Schema(description = "Of those, the sessions still unpaid")
            @JsonProperty("outstanding_session_count")
            long outstandingSessionCount

    ) {
    }
}
