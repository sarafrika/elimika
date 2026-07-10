package apps.sarafrika.elimika.commerce.payment.controller;

import apps.sarafrika.elimika.commerce.payment.dto.MpesaCheckoutResponse;
import apps.sarafrika.elimika.commerce.payment.dto.MpesaPaymentRequest;
import apps.sarafrika.elimika.commerce.payment.dto.PaymentStatusResponse;
import apps.sarafrika.elimika.commerce.payment.service.OrderPaymentService;
import apps.sarafrika.elimika.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing M-Pesa payment orchestration for internal commerce orders.
 */
@RestController
@RequestMapping(OrderPaymentController.API_ROOT_PATH)
@RequiredArgsConstructor
@Tag(name = "Commerce Order Payments", description = "Initiate M-Pesa payments and track capture for orders")
public class OrderPaymentController {

    public static final String API_ROOT_PATH = "/api/v1/commerce/orders";

    private final OrderPaymentService orderPaymentService;

    @Operation(
            summary = "Pay an order via M-Pesa",
            description = "Initiates an M-Pesa STK Push for an order that is awaiting payment",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "STK Push initiated",
                            content = @Content(schema = @Schema(implementation = MpesaCheckoutResponse.class)))
            }
    )
    @PostMapping("/{orderId}/pay/mpesa")
    public ResponseEntity<ApiResponse<MpesaCheckoutResponse>> payWithMpesa(
            @Parameter(description = "Order identifier", required = true)
            @PathVariable String orderId,
            @Valid @RequestBody MpesaPaymentRequest request) {
        MpesaCheckoutResponse response = orderPaymentService.initiateMpesaPayment(orderId, request.phoneNumber());
        return ResponseEntity.ok(ApiResponse.success(response, "M-Pesa STK Push initiated"));
    }

    @Operation(
            summary = "Get order payment status",
            description = "Polls the M-Pesa gateway and captures the order on confirmed payment",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payment status retrieved",
                            content = @Content(schema = @Schema(implementation = PaymentStatusResponse.class)))
            }
    )
    @GetMapping("/{orderId}/payment-status")
    public ResponseEntity<ApiResponse<PaymentStatusResponse>> getPaymentStatus(
            @Parameter(description = "Order identifier", required = true)
            @PathVariable String orderId) {
        PaymentStatusResponse response = orderPaymentService.getPaymentStatus(orderId);
        return ResponseEntity.ok(ApiResponse.success(response, "Payment status retrieved"));
    }
}
