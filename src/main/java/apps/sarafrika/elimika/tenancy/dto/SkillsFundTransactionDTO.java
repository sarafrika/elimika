package apps.sarafrika.elimika.tenancy.dto;

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

        @Schema(description = "Recipient / target of the movement.", nullable = true)
        @JsonProperty("target_name")
        String targetName,

        @Schema(description = "Amount.")
        @JsonProperty("amount")
        BigDecimal amount,

        @Schema(description = "Type: Allocation, Disbursement, Adjustment.")
        @JsonProperty("transaction_type")
        String transactionType,

        @Schema(description = "Status: Pending, Allocated, Approved, Completed.")
        @JsonProperty("status")
        String status,

        @Schema(nullable = true)
        @JsonProperty("transaction_date")
        LocalDateTime transactionDate,

        @Schema(accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "created_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime createdDate
) {
}
