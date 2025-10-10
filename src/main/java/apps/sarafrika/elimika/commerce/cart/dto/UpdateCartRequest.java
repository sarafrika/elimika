package apps.sarafrika.elimika.commerce.cart.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.util.StringUtils;

/**
 * Represents partial updates that can be applied to a Medusa cart.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(name = "UpdateCartRequest", description = "Fields that can be patched on an existing cart")
public class UpdateCartRequest {

    @Schema(description = "Email address of the customer", example = "learner@example.com")
    @Email(message = "Email address must be valid")
    @JsonProperty("email")
    private String email;

    @Schema(description = "Medusa customer identifier to associate with the cart", example = "cus_01HZX1X6QAQCCYT11S3R6G9KVN")
    @JsonProperty("customer_id")
    private String customerId;

    @Schema(description = "Medusa shipping address identifier", example = "addr_01HZX2F2Z6E0Y0T8VJX3W6PC66")
    @JsonProperty("shipping_address_id")
    private String shippingAddressId;

    @Schema(description = "Medusa billing address identifier", example = "addr_01HZX2F7GZ92P2X9YBQB0NQ9E3")
    @JsonProperty("billing_address_id")
    private String billingAddressId;

    @Schema(description = "Optional metadata map forwarded to Medusa", example = "{\"cohort\":\"Q3-2024\"}")
    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    public boolean hasUpdates() {
        return StringUtils.hasText(email)
                || StringUtils.hasText(customerId)
                || StringUtils.hasText(shippingAddressId)
                || StringUtils.hasText(billingAddressId)
                || (metadata != null && !metadata.isEmpty());
    }
}
