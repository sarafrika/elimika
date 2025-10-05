package apps.sarafrika.elimika.commerce.medusa.dto;

import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

/**
 * Minimal representation of a cart response from Medusa Store API.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class MedusaCartResponse {

    private String id;

    @JsonProperty("region_id")
    private String regionId;

    @JsonProperty("customer_id")
    private String customerId;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    @JsonProperty("updated_at")
    private OffsetDateTime updatedAt;

    private List<MedusaLineItemResponse> items;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MedusaLineItemResponse {

        private String id;

        private String title;

        private int quantity;

        @JsonProperty("variant_id")
        private String variantId;
    }
}
