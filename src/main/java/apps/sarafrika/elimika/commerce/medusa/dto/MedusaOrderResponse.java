package apps.sarafrika.elimika.commerce.medusa.dto;

import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Getter;
import lombok.Setter;

/**
 * Minimal representation of a Medusa order returned from the Store API.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class MedusaOrderResponse {

    @JsonProperty("id")
    private String id;

    @JsonProperty("display_id")
    private String displayId;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    @JsonProperty("status")
    private String status;

    @JsonProperty("fulfillment_status")
    private String fulfillmentStatus;

    @JsonProperty("payment_status")
    private String paymentStatus;

    @JsonProperty("items")
    private List<MedusaCartResponse.MedusaLineItemResponse> items;
}
