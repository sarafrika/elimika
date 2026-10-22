package apps.sarafrika.elimika.wallet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "PayoutMethodRequest", description = "An M-Pesa number to add for withdrawals")
public record PayoutMethodRequestDTO(
        @Schema(description = "Safaricom number in any common form: 07…, 01…, 2547…, +254 7….", example = "0712345678",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @JsonProperty("msisdn") String msisdn,
        @Schema(description = "Optional nickname.", example = "Personal")
        @Size(max = 40)
        @JsonProperty("label") String label,
        @Schema(description = "The caller confirms the number is registered in their name.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("confirms_registered_name") boolean confirmsRegisteredName
) {
}
