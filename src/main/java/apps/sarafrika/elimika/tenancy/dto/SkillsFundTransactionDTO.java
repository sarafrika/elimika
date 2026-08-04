package apps.sarafrika.elimika.tenancy.dto;

import apps.sarafrika.elimika.tenancy.util.enums.SkillsFundTransactionStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(name = "SkillsFundTransaction", description = "A movement within an organisation's skills fund.")
public record SkillsFundTransactionDTO(

        @Schema(accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "uuid", access = JsonProperty.Access.READ_ONLY)
        UUID uuid,

        @JsonProperty("organisation_uuid")
        UUID organisationUuid,

        @Schema(description = "Human-readable description.", nullable = true)
        @JsonProperty("description")
        String description,

        @Schema(description = "Display label for the recipient. Not an identity — never resolve money against this.",
                nullable = true)
        @JsonProperty("target_name")
        String targetName,

        @Schema(description = "The platform user this movement is for. Null on rows recorded before beneficiaries "
                + "were identifiable, and on movements with no individual recipient.", nullable = true)
        @JsonProperty("beneficiary_user_uuid")
        UUID beneficiaryUserUuid,

        @Schema(description = "Amount.")
        @JsonProperty("amount")
        BigDecimal amount,

        @Schema(description = "ISO-4217 currency the amount is denominated in, e.g. KES.")
        @JsonProperty("currency_code")
        String currencyCode,

        @Schema(description = "Type: Allocation, Disbursement, Adjustment.")
        @JsonProperty("transaction_type")
        String transactionType,

        @Schema(description = "PENDING (requested), ALLOCATED (earmarked), APPROVED (signed off), "
                + "DISBURSED (money left the fund). The legacy value 'Completed' is accepted on write and "
                + "stored as DISBURSED.")
        @JsonProperty("status")
        SkillsFundTransactionStatus status,

        @Schema(nullable = true)
        @JsonProperty("transaction_date")
        LocalDateTime transactionDate,

        @Schema(accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "created_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime createdDate
) {
}
