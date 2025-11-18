package apps.sarafrika.elimika.commerce.cart.service.impl;

import apps.sarafrika.elimika.commerce.cart.dto.CartLineItemRequest;
import apps.sarafrika.elimika.commerce.cart.dto.CartResponse;
import apps.sarafrika.elimika.commerce.cart.dto.CreateCartRequest;
import apps.sarafrika.elimika.commerce.cart.dto.SelectPaymentSessionRequest;
import apps.sarafrika.elimika.commerce.cart.dto.UpdateCartRequest;
import apps.sarafrika.elimika.commerce.cart.service.CartService;
import apps.sarafrika.elimika.commerce.internal.service.InternalCartService;
import apps.sarafrika.elimika.shared.dto.commerce.OrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Default implementation of {@link CartService} delegating to the internal commerce stack.
 */
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final InternalCartService internalCartService;

    @Override
    public CartResponse createCart(CreateCartRequest request) {
        return internalCartService.createCart(request);
    }

    @Override
    public CartResponse addItem(String cartId, CartLineItemRequest request) {
        return internalCartService.addItem(cartId, request);
    }

    @Override
    public CartResponse getCart(String cartId) {
        return internalCartService.getCart(cartId);
    }

    @Override
    public CartResponse updateCart(String cartId, UpdateCartRequest request) {
        return internalCartService.updateCart(cartId, request);
    }

    @Override
    public CartResponse selectPaymentSession(String cartId, SelectPaymentSessionRequest request) {
        return internalCartService.selectPaymentSession(cartId, request);
    }

    @Override
    public OrderResponse completeCart(String cartId) {
        return internalCartService.completeCart(cartId);
    }

}
