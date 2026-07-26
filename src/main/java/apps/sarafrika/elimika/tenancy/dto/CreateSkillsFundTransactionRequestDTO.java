package apps.sarafrika.elimika.tenancy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(name = "CreateSkillsFundTransactionRequest", description = "Payload to record a skills fund movement.")
public record CreateSkillsFundTransactionRequestDTO(

        @JsonProperty("description")
        String description,

        @JsonProperty("target_name")
        String targetName,

        @JsonProperty("amount")
        BigDecimal amount,

        @Schema(description = "Type: Allocation, Disbursement, Adjustment. Defaults to Allocation.")
        @JsonProperty("transaction_type")
        String transactionType,

        @Schema(description = "Status. Defaults to Pending.")
        @JsonProperty("status")
        String status,

        @JsonProperty("transaction_date")
        LocalDateTime transactionDate
) {
}
