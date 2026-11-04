package apps.sarafrika.elimika.wallet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "RejectPayoutBatchRequest", description = "Why a payout batch is not approved")
public record RejectPayoutBatchRequestDTO(
        @NotBlank @Size(max = 500) @JsonProperty("reason") String reason
) {
}
