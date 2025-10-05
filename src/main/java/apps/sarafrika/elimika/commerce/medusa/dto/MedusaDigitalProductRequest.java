package apps.sarafrika.elimika.commerce.medusa.dto;

import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

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
public class MedusaDigitalProductRequest {

    @NotBlank
    private String title;

    private String subtitle;

    private String description;

    @NotBlank
    private String sku;

    @NotBlank
    private String currencyCode;

    @Positive
    private long amount;

    @Builder.Default
    private boolean requiresShipping = false;

    /**
     * Optional override for the variant title. Defaults to the product title when left blank.
     */
    private String variantTitle;

    @Builder.Default
    private String optionTitle = "Format";

    @Builder.Default
    private String optionValue = "Digital";

    @Builder.Default
    private Map<String, Object> metadata = Map.of();

    @Builder.Default
    private List<String> collectionIds = List.of();
}
