package apps.sarafrika.elimika.commerce.cart.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request payload used when selecting the payment provider for a cart.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(name = "SelectPaymentSessionRequest", description = "Specifies the payment provider to use for a cart")
public class SelectPaymentSessionRequest {

    @Schema(
            description = "Identifier of the payment provider (e.g. 'manual', 'stripe')",
            example = "manual",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Provider identifier is required")
    @JsonProperty("provider_id")
    private String providerId;
}
