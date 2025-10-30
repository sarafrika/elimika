package apps.sarafrika.elimika.commerce.medusa.dto;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request payload for creating a digital product with a single SKU in Medusa.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class MedusaDigitalProductRequest {

    @NotBlank
    @JsonProperty("title")
    private String title;

    @JsonProperty("subtitle")
    private String subtitle;

    @JsonProperty("description")
    private String description;

    @NotBlank
    @JsonProperty("sku")
    private String sku;

    @JsonProperty("currency_code")
    @Size(min = 3, max = 3, message = "Currency code must be a 3-letter ISO value")
    @Pattern(regexp = "^[A-Za-z]{3}$", message = "Currency code must be alphabetic")
    private String currencyCode;

    @Positive
    @JsonProperty("amount")
    private long amount;

    @Builder.Default
    @JsonProperty("requires_shipping")
    private boolean requiresShipping = false;

    /**
     * Optional override for the variant title. Defaults to the product title when left blank.
     */
    @JsonProperty("variant_title")
    private String variantTitle;

    @Builder.Default
    @JsonProperty("option_title")
    private String optionTitle = "Format";

    @Builder.Default
    @JsonProperty("option_value")
    private String optionValue = "Digital";

    @Builder.Default
    @JsonProperty("metadata")
    private Map<String, Object> metadata = Map.of();

    @Builder.Default
    @JsonProperty("collection_ids")
    private List<String> collectionIds = List.of();
}
