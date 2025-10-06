package apps.sarafrika.elimika.commerce.order.controller;

import apps.sarafrika.elimika.commerce.order.dto.CheckoutRequest;
import apps.sarafrika.elimika.commerce.order.dto.OrderResponse;
import apps.sarafrika.elimika.commerce.order.service.OrderService;
import apps.sarafrika.elimika.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing order management endpoints.
 */
@RestController
@RequestMapping(OrderController.API_ROOT_PATH)
@RequiredArgsConstructor
@Tag(name = "Commerce Orders", description = "Endpoints for orchestrating and tracking Medusa orders")
public class OrderController {

    public static final String API_ROOT_PATH = "/api/v1/commerce/orders";

    private final OrderService orderService;

    @Operation(
            summary = "Complete checkout",
            description = "Performs the full Medusa checkout flow including customer association and payment selection",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Order created",
                            content = @Content(schema = @Schema(implementation = OrderResponse.class)))
            }
    )
    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<OrderResponse>> completeCheckout(
            @Valid @RequestBody CheckoutRequest request) {
        OrderResponse order = orderService.completeCheckout(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(order, "Order created successfully"));
    }

    @Operation(
            summary = "Get order details",
            description = "Retrieves an order from Medusa to support order tracking",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order retrieved",
                            content = @Content(schema = @Schema(implementation = OrderResponse.class)))
            }
    )
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(
            @Parameter(description = "Medusa order identifier", required = true)
            @PathVariable String orderId) {
        OrderResponse order = orderService.getOrder(orderId);
        return ResponseEntity.ok(ApiResponse.success(order, "Order retrieved successfully"));
    }
}
