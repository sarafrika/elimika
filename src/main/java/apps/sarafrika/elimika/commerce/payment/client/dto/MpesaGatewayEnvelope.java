package apps.sarafrika.elimika.commerce.payment.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Generic response envelope returned by the mpesa-service. Only the fields elimika consumes
 * are mapped; everything else is ignored.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MpesaGatewayEnvelope<T>(
        boolean success,
        T data
) {
}
