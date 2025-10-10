package apps.sarafrika.elimika.commerce.medusa.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request payload for adding a product variant to a Medusa cart.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class MedusaLineItemRequest {

    @NotBlank
    @JsonProperty("variant_id")
    private String variantId;

    @Builder.Default
    @Min(1)
    @JsonProperty("quantity")
    private int quantity = 1;

    @Builder.Default
    @JsonProperty("metadata")
    private Map<String, Object> metadata = Map.of();
}
