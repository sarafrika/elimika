package apps.sarafrika.elimika.commerce.cart.controller;

import apps.sarafrika.elimika.commerce.cart.dto.CartLineItemRequest;
import apps.sarafrika.elimika.commerce.cart.dto.CartResponse;
import apps.sarafrika.elimika.commerce.cart.dto.CreateCartRequest;
import apps.sarafrika.elimika.commerce.cart.dto.SelectPaymentSessionRequest;
import apps.sarafrika.elimika.commerce.cart.dto.UpdateCartRequest;
import apps.sarafrika.elimika.commerce.cart.service.CartService;
import apps.sarafrika.elimika.shared.dto.commerce.OrderResponse;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing cart operations for the Elimika commerce module.
 */
@RestController
@RequestMapping(CartController.API_ROOT_PATH)
@RequiredArgsConstructor
@Tag(name = "Commerce Carts", description = "Operations for managing shopping carts synchronised with Medusa")
public class CartController {

    public static final String API_ROOT_PATH = "/api/v1/commerce/carts";

    private final CartService cartService;

    @Operation(
            summary = "Create a new cart",
            description = "Initialises a new cart in Medusa that can be used for checkout flows",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Cart created successfully",
                            content = @Content(schema = @Schema(implementation = CartResponse.class)))
            }
    )
    @PostMapping
    public ResponseEntity<ApiResponse<CartResponse>> createCart(
            @Valid @RequestBody CreateCartRequest request) {
        CartResponse cart = cartService.createCart(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(cart, "Cart created successfully"));
    }

    @Operation(
            summary = "Add an item to a cart",
            description = "Adds or increments a line item on an existing cart",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Item added",
                            content = @Content(schema = @Schema(implementation = CartResponse.class)))
            }
    )
    @PostMapping("/{cartId}/items")
    public ResponseEntity<ApiResponse<CartResponse>> addItem(
            @Parameter(description = "Identifier of the cart to update", required = true)
            @PathVariable String cartId,
            @Valid @RequestBody CartLineItemRequest request) {
        CartResponse cart = cartService.addItem(cartId, request);
        return ResponseEntity.ok(ApiResponse.success(cart, "Cart updated successfully"));
    }

    @Operation(
            summary = "Retrieve cart details",
            description = "Fetches the latest cart representation from Medusa",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cart retrieved",
                            content = @Content(schema = @Schema(implementation = CartResponse.class)))
            }
    )
    @GetMapping("/{cartId}")
    public ResponseEntity<ApiResponse<CartResponse>> getCart(
            @Parameter(description = "Identifier of the cart to load", required = true)
            @PathVariable String cartId) {
        CartResponse cart = cartService.getCart(cartId);
        return ResponseEntity.ok(ApiResponse.success(cart, "Cart retrieved successfully"));
    }

    @Operation(
            summary = "Update cart attributes",
            description = "Updates cart metadata such as customer or addresses",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cart updated",
                            content = @Content(schema = @Schema(implementation = CartResponse.class)))
            }
    )
    @PatchMapping("/{cartId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateCart(
            @Parameter(description = "Identifier of the cart to update", required = true)
            @PathVariable String cartId,
            @Valid @RequestBody UpdateCartRequest request) {
        CartResponse cart = cartService.updateCart(cartId, request);
        return ResponseEntity.ok(ApiResponse.success(cart, "Cart updated successfully"));
    }

    @Operation(
            summary = "Select payment session",
            description = "Locks the cart to a particular Medusa payment provider",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payment session selected",
                            content = @Content(schema = @Schema(implementation = CartResponse.class)))
            }
    )
    @PostMapping("/{cartId}/payment-session")
    public ResponseEntity<ApiResponse<CartResponse>> selectPaymentSession(
            @Parameter(description = "Identifier of the cart to update", required = true)
            @PathVariable String cartId,
            @Valid @RequestBody SelectPaymentSessionRequest request) {
        CartResponse cart = cartService.selectPaymentSession(cartId, request);
        return ResponseEntity.ok(ApiResponse.success(cart, "Payment provider selected"));
    }

    @Operation(
            summary = "Complete cart",
            description = "Finalises the cart in Medusa and creates an order",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cart completed",
                            content = @Content(schema = @Schema(implementation = OrderResponse.class)))
            }
    )
    @PostMapping("/{cartId}/complete")
    public ResponseEntity<ApiResponse<OrderResponse>> completeCart(
            @Parameter(description = "Identifier of the cart to finalise", required = true)
            @PathVariable String cartId) {
        OrderResponse order = cartService.completeCart(cartId);
        return ResponseEntity.ok(ApiResponse.success(order, "Cart completed successfully"));
    }
}
