package apps.sarafrika.elimika.commerce.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Tells the web client whether a learner has to pay for an order at all.
 */
@Schema(name = "PaymentModeResponse", description = "Whether checkout requires a payment on this environment")
public record PaymentModeResponse(

        @Schema(description = "False on an environment that captures orders without a gateway, in which"
                + " case the client must not send the learner to the payment page and no STK Push is"
                + " ever initiated. True wherever money is actually collected.", example = "true")
        @JsonProperty("payment_required")
        boolean paymentRequired
) {
}
