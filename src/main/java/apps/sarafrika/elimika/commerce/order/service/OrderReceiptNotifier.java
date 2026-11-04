package apps.sarafrika.elimika.commerce.order.service;

import apps.sarafrika.elimika.commerce.internal.entity.CommercePayment;
import apps.sarafrika.elimika.commerce.internal.enums.PaymentStatus;
import apps.sarafrika.elimika.commerce.internal.repository.CommercePaymentRepository;
import apps.sarafrika.elimika.shared.dto.commerce.CartItemResponse;
import apps.sarafrika.elimika.shared.dto.commerce.OrderResponse;
import apps.sarafrika.elimika.shared.event.notification.DashboardLinks;
import apps.sarafrika.elimika.shared.event.notification.NotificationRequestedEvent;
import apps.sarafrika.elimika.shared.sms.Msisdn;
import apps.sarafrika.elimika.shared.sms.SmsSender;
import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Sends the receipt for a paid order: email and in-app through notifications, SMS to the number that
 * paid. Only a captured order gets one, so nobody is told "payment received" before the money is in.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderReceiptNotifier {

    static final int SMS_CLASS_TITLE_MAX = 32;

    private final ApplicationEventPublisher eventPublisher;
    private final UserLookupService userLookupService;
    private final CommercePaymentRepository paymentRepository;
    private final SmsSender smsSender;

    public void sendReceipt(OrderResponse order) {
        if (order == null || order.getUserUuid() == null || !"CAPTURED".equalsIgnoreCase(order.getPaymentStatus())) {
            return;
        }
        Optional<CommercePayment> payment = parseUuid(order.getId())
                .flatMap(uuid -> paymentRepository.findFirstByOrderUuidAndStatus(uuid, PaymentStatus.CAPTURED));
        String receipt = payment.map(CommercePayment::getProviderReceipt).orElse(null);
        String amount = amountLabel(order.getCurrencyCode(), order.getTotal());
        String classTitle = classTitle(order);

        sendEmail(order, amount, classTitle, receipt);
        payment.map(CommercePayment::getPayerMsisdn)
                .flatMap(Msisdn::normalise)
                .ifPresent(msisdn -> smsSender.send(msisdn, smsText(amount, classTitle, order.getDisplayId(), receipt)));
    }

    private void sendEmail(OrderResponse order, String amount, String classTitle, String receipt) {
        UUID buyer = order.getUserUuid();
        Optional<String> email = userLookupService.getUserEmail(buyer).filter(StringUtils::hasText);
        if (email.isEmpty()) {
            log.warn("No email on file for the buyer of order {}; email receipt skipped", order.getDisplayId());
            return;
        }
        Map<String, Object> variables = receiptVariables(order, amount, classTitle, receipt);
        eventPublisher.publishEvent(new NotificationRequestedEvent(
                UUID.randomUUID(),
                buyer,
                email.get(),
                userLookupService.getUserFullName(buyer).orElse(email.get()),
                "ORDER_PAYMENT_RECEIPT",
                "NORMAL",
                "POPUP",
                "Payment received",
                "We received " + amount + " for " + classTitle + ". Your seat is confirmed.",
                receiptActionUrl(buyer),
                variables,
                Set.of("email", "in_app"),
                "order-payment-receipt:" + order.getId(),
                LocalDateTime.now(ZoneOffset.UTC),
                findOrganization(buyer),
                null));
    }

    /** One GSM-7 segment: plain hyphen, no emoji, class title cut to {@value #SMS_CLASS_TITLE_MAX}. */
    static String smsText(String amount, String classTitle, String orderNumber, String receipt) {
        String title = classTitle.length() > SMS_CLASS_TITLE_MAX
                ? classTitle.substring(0, SMS_CLASS_TITLE_MAX - 3).trim() + "..."
                : classTitle;
        StringBuilder text = new StringBuilder("Elimika: Paid ").append(amount).append(" for ").append(title)
                .append(". Order ").append(orderNumber);
        if (StringUtils.hasText(receipt)) {
            text.append(", M-Pesa ").append(receipt);
        }
        return text.append(". Thank you!").toString().replace('—', '-').replace('–', '-');
    }

    /** Whole shillings with thousands separators, e.g. {@code KES 10,500}. */
    static String amountLabel(String currency, BigDecimal total) {
        BigDecimal value = total == null ? BigDecimal.ZERO : total.setScale(0, RoundingMode.HALF_UP);
        String code = StringUtils.hasText(currency) ? currency.toUpperCase(Locale.ROOT) : "KES";
        return code + " " + NumberFormat.getIntegerInstance(Locale.ENGLISH).format(value);
    }

    private static String classTitle(OrderResponse order) {
        List<CartItemResponse> items = order.getItems();
        if (items == null || items.isEmpty() || !StringUtils.hasText(items.getFirst().getTitle())) {
            return "your class";
        }
        return items.size() == 1 ? items.getFirst().getTitle() : items.getFirst().getTitle() + " and " + (items.size() - 1) + " more";
    }

    /** Payments surface in the student wallet or the parent billing page; others use the dashboard entry. */
    private String receiptActionUrl(UUID userId) {
        if (userLookupService.userHasDomain(userId, UserDomain.student)) {
            return DashboardLinks.STUDENT_WALLET;
        }
        if (userLookupService.userHasDomain(userId, UserDomain.parent)) {
            return DashboardLinks.PARENT_BILLING;
        }
        return DashboardLinks.DASHBOARD;
    }

    private UUID findOrganization(UUID userId) {
        List<UUID> organizations = userLookupService.getUserOrganizations(userId);
        return organizations == null || organizations.isEmpty() ? null : organizations.getFirst();
    }

    private Map<String, Object> receiptVariables(OrderResponse order, String amount, String classTitle, String receipt) {
        Map<String, Object> variables = new HashMap<>();
        putIfNotNull(variables, "orderId", order.getId());
        putIfNotNull(variables, "orderDisplayId", order.getDisplayId());
        putIfNotNull(variables, "paymentStatus", order.getPaymentStatus());
        putIfNotNull(variables, "currencyCode", order.getCurrencyCode());
        putIfNotNull(variables, "subtotal", order.getSubtotal());
        putIfNotNull(variables, "total", order.getTotal());
        putIfNotNull(variables, "createdAt", order.getCreatedAt());
        putIfNotNull(variables, "mpesaReceipt", receipt);
        variables.put("amountLabel", amount);
        variables.put("classTitle", classTitle);
        variables.put("items", order.getItems() == null ? List.of() : order.getItems());
        return variables;
    }

    private static void putIfNotNull(Map<String, Object> variables, String key, Object value) {
        if (value != null) {
            variables.put(key, value);
        }
    }

    private static Optional<UUID> parseUuid(String id) {
        try {
            return Optional.ofNullable(id).map(UUID::fromString);
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
