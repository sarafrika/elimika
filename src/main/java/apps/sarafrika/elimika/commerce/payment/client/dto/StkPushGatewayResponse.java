package apps.sarafrika.elimika.commerce.payment.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * STK Push initiation response payload from the mpesa-service gateway.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record StkPushGatewayResponse(
        @JsonProperty("CheckoutRequestID") String checkoutRequestId,
        @JsonProperty("MerchantRequestID") String merchantRequestId
) {
}
