package apps.sarafrika.elimika.wallet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "RejectWithdrawalRequest", description = "Why a withdrawal is not approved; shown to the earner")
public record RejectWithdrawalRequestDTO(
        @NotBlank @Size(min = 10, max = 500) @JsonProperty("reason") String reason
) {
}
