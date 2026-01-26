package apps.sarafrika.elimika.commerce.internal.service;

import apps.sarafrika.elimika.commerce.cart.dto.CartLineItemRequest;
import apps.sarafrika.elimika.commerce.cart.dto.CartResponse;
import apps.sarafrika.elimika.commerce.cart.dto.CreateCartRequest;
import apps.sarafrika.elimika.commerce.cart.dto.SelectPaymentSessionRequest;
import apps.sarafrika.elimika.commerce.cart.dto.UpdateCartRequest;
import apps.sarafrika.elimika.shared.dto.commerce.OrderResponse;

/**
 * Internal cart operations backed by the in-house commerce stack.
 */
public interface InternalCartService {

    CartResponse createCart(CreateCartRequest request);

    CartResponse addItem(String cartId, CartLineItemRequest request);

    CartResponse removeItem(String cartId, String itemId);

    CartResponse getCart(String cartId);

    CartResponse updateCart(String cartId, UpdateCartRequest request);

    CartResponse selectPaymentSession(String cartId, SelectPaymentSessionRequest request);

    OrderResponse completeCart(String cartId);
}
