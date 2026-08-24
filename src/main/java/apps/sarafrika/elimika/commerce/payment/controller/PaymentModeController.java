package apps.sarafrika.elimika.commerce.payment.controller;

import apps.sarafrika.elimika.commerce.internal.config.CommerceCaptureProperties;
import apps.sarafrika.elimika.commerce.payment.dto.PaymentModeResponse;
import apps.sarafrika.elimika.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Publishes whether this environment collects money, so the web client routes learners the same way
 * the server settles them.
 *
 * <h2>Why the client cannot decide this for itself</h2>
 * The web client is built and deployed separately from this service, and its build-time environment
 * is baked into the image. A flag compiled into the client can therefore disagree with the server it
 * ends up talking to, and the failure is silent in the worst direction: the client sends a learner to
 * the payment page on an environment with no working till, or skips it on one that never captures,
 * leaving an order nobody will ever settle. Reading it from the server at runtime removes the
 * possibility - there is one value, and it is the one the order service acts on.
 */
@RestController
@RequestMapping(PaymentModeController.API_ROOT_PATH)
@RequiredArgsConstructor
@Tag(name = "Commerce Payment Mode", description = "Whether checkout requires a payment on this environment")
public class PaymentModeController {

    public static final String API_ROOT_PATH = "/api/v1/commerce/payment-mode";

    private final CommerceCaptureProperties captureProperties;

    @Operation(
            summary = "Is a payment required to check out?",
            description = "Returns payment_required=false on an environment that captures orders on"
                    + " checkout completion. Clients must skip the payment page and initiate no STK"
                    + " Push when this is false, because the order is already settled by then and"
                    + " asking to pay for it is rejected."
    )
    @GetMapping
    public ResponseEntity<ApiResponse<PaymentModeResponse>> getPaymentMode() {
        PaymentModeResponse mode = new PaymentModeResponse(!captureProperties.isAutoOnComplete());
        return ResponseEntity.ok(ApiResponse.success(mode, "Payment mode retrieved"));
    }
}
