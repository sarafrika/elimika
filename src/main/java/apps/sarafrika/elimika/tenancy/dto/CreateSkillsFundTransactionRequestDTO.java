package apps.sarafrika.elimika.tenancy.dto;

import apps.sarafrika.elimika.tenancy.util.enums.SkillsFundTransactionStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(name = "CreateSkillsFundTransactionRequest", description = "Payload to record a skills fund movement.")
public record CreateSkillsFundTransactionRequestDTO(

        @JsonProperty("description")
        String description,

        @Schema(description = "Display label for the recipient. Kept for rendering; it does not identify anyone.")
        @JsonProperty("target_name")
        String targetName,

        /**
         * Optional, because the dashboard records movements that have no individual behind them —
         * a bulk allocation, a correcting adjustment. A movement that is expected to become money
         * must carry one.
         */
        @Schema(description = "The platform user this movement is for. Required for anything that will "
                + "later be paid out; optional for allocations and adjustments with no individual recipient.")
        @JsonProperty("beneficiary_user_uuid")
        UUID beneficiaryUserUuid,

        @JsonProperty("amount")
        BigDecimal amount,

        @Schema(description = "ISO-4217 currency the amount is denominated in. Defaults to the platform currency (KES).")
        @JsonProperty("currency_code")
        String currencyCode,

        @Schema(description = "Type: Allocation, Disbursement, Adjustment. Defaults to Allocation.")
        @JsonProperty("transaction_type")
        String transactionType,

        @Schema(description = "PENDING, ALLOCATED, APPROVED or DISBURSED. The legacy value 'Completed' is "
                + "accepted and stored as DISBURSED. Defaults to PENDING.")
        @JsonProperty("status")
        SkillsFundTransactionStatus status,

        @JsonProperty("transaction_date")
        LocalDateTime transactionDate
) {
}
