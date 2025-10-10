package apps.sarafrika.elimika.commerce.medusa.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Getter;
import lombok.Setter;

/**
 * Minimal representation of a cart response from Medusa Store API.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class MedusaCartResponse {

    @JsonProperty("id")
    private String id;

    @JsonProperty("region_id")
    private String regionId;

    @JsonProperty("customer_id")
    private String customerId;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    @JsonProperty("updated_at")
    private OffsetDateTime updatedAt;

    @JsonProperty("items")
    private List<MedusaLineItemResponse> items;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class MedusaLineItemResponse {

        @JsonProperty("id")
        private String id;

        @JsonProperty("title")
        private String title;

        @JsonProperty("quantity")
        private int quantity;

        @JsonProperty("variant_id")
        private String variantId;

        @JsonProperty("metadata")
        private Map<String, Object> metadata;
    }
}
