package apps.sarafrika.elimika.commerce.medusa.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

/**
 * Subset of fields returned for Medusa product variants.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class MedusaVariantResponse {

    private String id;

    private String title;

    private String sku;

    @JsonProperty("manage_inventory")
    private boolean manageInventory;

    @JsonProperty("allow_backorder")
    private boolean allowBackorder;
}
