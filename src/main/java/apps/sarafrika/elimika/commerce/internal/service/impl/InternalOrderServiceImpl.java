package apps.sarafrika.elimika.commerce.internal.service.impl;

import apps.sarafrika.elimika.commerce.cart.dto.SelectPaymentSessionRequest;
import apps.sarafrika.elimika.commerce.cart.dto.UpdateCartRequest;
import apps.sarafrika.elimika.commerce.internal.config.PaymentCallbackProperties;
import apps.sarafrika.elimika.commerce.internal.entity.CommerceOrder;
import apps.sarafrika.elimika.commerce.internal.enums.PaymentStatus;
import apps.sarafrika.elimika.commerce.internal.mapper.InternalCommerceMapper;
import apps.sarafrika.elimika.commerce.internal.repository.CommerceOrderRepository;
import apps.sarafrika.elimika.commerce.internal.service.InternalCartService;
import apps.sarafrika.elimika.commerce.internal.service.InternalOrderService;
import apps.sarafrika.elimika.shared.dto.commerce.CheckoutRequest;
import apps.sarafrika.elimika.shared.dto.commerce.OrderResponse;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import apps.sarafrika.elimika.commerce.internal.entity.CommercePayment;
import apps.sarafrika.elimika.commerce.internal.enums.OrderStatus;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class InternalOrderServiceImpl implements InternalOrderService {

    private static final String SERVICE_ACCOUNT_PREFIX = "service-account-";

    private final InternalCartService internalCartService;
    private final apps.sarafrika.elimika.commerce.internal.repository.CommercePaymentRepository paymentRepository;
    private final CommerceOrderRepository orderRepository;
    private final InternalCommerceMapper mapper;
    private final DomainSecurityService domainSecurityService;
    private final PaymentCallbackProperties paymentCallbackProperties;

    @Override
    public OrderResponse completeCheckout(CheckoutRequest request) {
        Optional<OrderResponse> existingOrder = findExistingOrderForCart(request.getCartId());
        if (existingOrder.isPresent()) {
            return existingOrder.get();
        }

        UpdateCartRequest updateCartRequest = new UpdateCartRequest();
        updateCartRequest.setEmail(request.getCustomerEmail());
        updateCartRequest.setShippingAddressId(request.getShippingAddressId());
        updateCartRequest.setBillingAddressId(request.getBillingAddressId());
        internalCartService.updateCart(request.getCartId(), updateCartRequest);
        if (StringUtils.hasText(request.getPaymentProviderId())) {
            SelectPaymentSessionRequest paymentRequest = new SelectPaymentSessionRequest();
            paymentRequest.setProviderId(request.getPaymentProviderId());
            internalCartService.selectPaymentSession(request.getCartId(), paymentRequest);
        }
        return internalCartService.completeCart(request.getCartId());
    }

    private Optional<OrderResponse> findExistingOrderForCart(String cartId) {
        if (!StringUtils.hasText(cartId)) {
            return Optional.empty();
        }
        try {
            UUID cartUuid = UUID.fromString(cartId);
            // Another buyer's cart id must not hand back their order. Filtering it out here sends
            // the request on to the cart load, which reports that cart as not found. Checking out is
            // a buyer's act, so the settlement token is no help on this path.
            return orderRepository.findFirstByCart_UuidOrderByCreatedDateDesc(cartUuid)
                    .filter(this::isBuyerOrAdmin)
                    .map(mapper::toOrderResponse);
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrder(String orderId) {
        return mapper.toOrderResponse(loadOrder(orderId));
    }

    @Override
    public OrderResponse markOrderCaptured(String orderId) {
        CommerceOrder order = loadOrder(orderId);
        if (order.getPaymentStatus() != PaymentStatus.CAPTURED) {
            order.setPaymentStatus(PaymentStatus.CAPTURED);
            // OrderStatus.COMPLETED existed but was never assigned anywhere, so every order stayed
            // PENDING for life and no report could tell a finished order from an abandoned one.
            order.setStatus(OrderStatus.COMPLETED);
            order = orderRepository.save(order);
            recordPayment(order);
        }
        return mapper.toOrderResponse(order);
    }

    @Override
    public void storeCheckoutRequestId(String orderId, String checkoutRequestId) {
        // Pointing an order at a checkout decides which M-Pesa payment will be believed to have
        // paid for it, so only the buyer starting that payment may do it. The settlement token
        // reads the checkout id it was given; it never chooses one.
        CommerceOrder order = loadBuyersOrder(orderId);
        order.setCheckoutRequestId(checkoutRequestId);
        orderRepository.save(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<String> findCheckoutRequestId(String orderId) {
        UUID uuid = parseUuid(orderId);
        return orderRepository.findByUuid(uuid)
                .filter(this::canAccess)
                .map(CommerceOrder::getCheckoutRequestId);
    }

    /**
     * Loads an order the caller is entitled to see: its buyer, an administrator, or the settlement
     * machinery finishing a payment already under way on it.
     * <p>
     * Order ids are unguessable but they travel: they sit in the checkout URL, in receipts and in
     * support threads. An order that exists but belongs to another buyer is reported exactly like
     * one that does not exist, so holding the id confirms nothing.
     */
    private CommerceOrder loadOrder(String orderId) {
        return findOrder(orderId, this::canAccess);
    }

    /**
     * Loads an order for a path that only its buyer (or an administrator) may take, closed even to
     * the settlement token.
     */
    private CommerceOrder loadBuyersOrder(String orderId) {
        return findOrder(orderId, this::isBuyerOrAdmin);
    }

    private CommerceOrder findOrder(String orderId, Predicate<CommerceOrder> entitled) {
        UUID uuid = parseUuid(orderId);
        return orderRepository.findByUuid(uuid)
                .filter(entitled)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    }

    private boolean canAccess(CommerceOrder order) {
        return isBuyerOrAdmin(order) || isSettlingThisPayment(order);
    }

    /**
     * The buyer recorded on the order, and platform admins.
     */
    private boolean isBuyerOrAdmin(CommerceOrder order) {
        UUID callerUuid = domainSecurityService.getCurrentUserUuid();
        if (callerUuid == null) {
            return false;
        }
        return callerUuid.equals(order.getUserUuid()) || domainSecurityService.isPlatformAdmin();
    }

    /**
     * The two callers that finish a payment nobody is watching, each held to the order in front of
     * them rather than trusted with orders at large.
     * <p>
     * One is the M-Pesa gateway calling the payment callback: a Keycloak client-credentials token,
     * which {@code UserSyncFilter} deliberately never maps to a user, and which must come from a
     * client named in {@link PaymentCallbackProperties} — being some service account in the realm is
     * not an identity, since every internal integration has one. The other is the reconciliation
     * sweep, which runs on a scheduler thread with no security context at all and so cannot be
     * imitated over HTTP.
     * <p>
     * Either way the order must already carry a checkout request id, which only the buyer's own
     * {@code pay/mpesa} call writes. That ties the token to an order whose payment is genuinely in
     * flight instead of letting it read any order it can name, and capture still happens only when
     * the gateway itself confirms the money — the callback carries no body worth believing.
     */
    private boolean isSettlingThisPayment(CommerceOrder order) {
        if (domainSecurityService.getCurrentUserUuid() != null) {
            return false;
        }
        if (!StringUtils.hasText(order.getCheckoutRequestId())) {
            return false;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return true;
        }
        return isAllowedCallbackClient(authentication);
    }

    private boolean isAllowedCallbackClient(Authentication authentication) {
        if (authentication instanceof AnonymousAuthenticationToken
                || !(authentication instanceof JwtAuthenticationToken jwtToken)) {
            return false;
        }
        String preferredUsername = jwtToken.getToken().getClaimAsString("preferred_username");
        if (preferredUsername == null || !preferredUsername.startsWith(SERVICE_ACCOUNT_PREFIX)) {
            return false;
        }
        // Keycloak names a service account after its client, and states the same client in azp.
        String clientId = jwtToken.getToken().getClaimAsString("azp");
        if (!StringUtils.hasText(clientId)) {
            clientId = preferredUsername.substring(SERVICE_ACCOUNT_PREFIX.length());
        }
        if (paymentCallbackProperties.getClientIds().contains(clientId)) {
            return true;
        }
        log.warn("Refused settlement call from Keycloak client {}. If that is the payment gateway,"
                + " list it in commerce.payment.callback.client-ids"
                + " (COMMERCE_PAYMENT_CALLBACK_CLIENT_IDS).", clientId);
        return false;
    }

    private UUID parseUuid(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid order identifier", ex);
        }
    }

    /**
     * Writes the payment row for a captured order.
     * <p>
     * {@code commerce_payments} and its entity have always existed, and only the query service ever
     * touched them — nothing wrote a row, so every payment report was empty by construction rather
     * than by absence of payments.
     */
    private void recordPayment(CommerceOrder order) {
        if (paymentRepository.existsByOrderUuidAndStatus(order.getUuid(), PaymentStatus.CAPTURED)) {
            return;
        }
        CommercePayment payment = new CommercePayment();
        payment.setOrder(order);
        payment.setProvider(order.getPaymentProviderId());
        payment.setStatus(PaymentStatus.CAPTURED);
        payment.setAmount(order.getTotalAmount());
        payment.setCurrencyCode(order.getCurrencyCode());
        payment.setExternalReference(order.getCheckoutRequestId());
        payment.setProcessedAt(java.time.LocalDateTime.now(java.time.ZoneOffset.UTC));
        paymentRepository.save(payment);
    }
}
