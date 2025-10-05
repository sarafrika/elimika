package apps.sarafrika.elimika.commerce.medusa.dto;

import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

/**
 * Minimal representation of a Medusa order returned from the Store API.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class MedusaOrderResponse {

    private String id;

    @JsonProperty("display_id")
    private String displayId;

    @JsonProperty("created_at")
    private OffsetDateTime createdAt;

    private String status;

    @JsonProperty("fulfillment_status")
    private String fulfillmentStatus;

    @JsonProperty("payment_status")
    private String paymentStatus;

    private List<MedusaCartResponse.MedusaLineItemResponse> items;
}
