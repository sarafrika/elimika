package apps.sarafrika.elimika.wallet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;

@Schema(name = "VerifyCodeRequest", description = "The six-digit code from the SMS")
public record VerifyCodeRequestDTO(
        @Schema(description = "Six digits.", example = "482913", requiredMode = Schema.RequiredMode.REQUIRED)
        @Pattern(regexp = "\\d{6}", message = "Enter the 6-digit code from the SMS.")
        @JsonProperty("code") String code
) {
}
