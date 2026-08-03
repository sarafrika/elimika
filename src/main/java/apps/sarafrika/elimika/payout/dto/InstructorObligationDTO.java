package apps.sarafrika.elimika.payout.dto;

import apps.sarafrika.elimika.payout.enums.InstructorObligationStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One delivered session's pay owed by an organisation to an instructor.
 */
@Schema(
        name = "InstructorObligation",
        description = "A single session's pay owed by an organisation to an instructor, at the rate snapshotted when the session completed"
)
public record InstructorObligationDTO(

        @Schema(description = "UUID of the obligation")
        @JsonProperty("uuid")
        UUID uuid,

        @Schema(description = "Organisation that owes the money")
        @JsonProperty("organisation_uuid")
        UUID organisationUuid,

        @Schema(description = "Instructor profile owed")
        @JsonProperty("instructor_uuid")
        UUID instructorUuid,

        @Schema(description = "Platform user behind the instructor profile, resolved at accrual")
        @JsonProperty("instructor_user_uuid")
        UUID instructorUserUuid,

        @Schema(description = "Class whose session was delivered")
        @JsonProperty("class_definition_uuid")
        UUID classDefinitionUuid,

        @Schema(description = "Scheduled session that was delivered")
        @JsonProperty("session_uuid")
        UUID sessionUuid,

        @Schema(description = "Per-session fee as it stood when the session completed; never recomputed")
        @JsonProperty("rate_amount")
        BigDecimal rateAmount,

        @Schema(description = "Currency the obligation was accrued in")
        @JsonProperty("currency_code")
        String currencyCode,

        @Schema(description = "ACCRUED, SETTLED, CANCELLED or DISPUTED")
        @JsonProperty("status")
        InstructorObligationStatus status,

        @Schema(description = "When the session completed and the obligation arose (UTC)")
        @JsonProperty("accrued_at")
        LocalDateTime accruedAt,

        @Schema(description = "When the organisation recorded that it had paid (UTC)")
        @JsonProperty("settled_at")
        LocalDateTime settledAt,

        @Schema(description = "The organisation's own reference for the payment it made off-platform")
        @JsonProperty("settlement_reference")
        String settlementReference,

        @Schema(description = "User who recorded the settlement")
        @JsonProperty("settled_by")
        String settledBy,

        @Schema(description = "Free-text reason accompanying a settlement, cancellation or dispute")
        @JsonProperty("status_note")
        String statusNote

) {
}
