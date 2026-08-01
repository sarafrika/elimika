package apps.sarafrika.elimika.payout.service;

import apps.sarafrika.elimika.course.spi.CourseInfoService;
import apps.sarafrika.elimika.course.spi.CourseInfoService.RevenueShare;
import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.shared.dto.commerce.CartItemResponse;
import apps.sarafrika.elimika.shared.dto.commerce.OrderResponse;
import apps.sarafrika.elimika.shared.event.commerce.OrderCompletedEvent;
import apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService;
import apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService.ClassDefinitionSnapshot;
import apps.sarafrika.elimika.shared.spi.revenue.PurchaseScope;
import apps.sarafrika.elimika.wallet.service.WalletService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * Credits earner wallets when a commerce order is captured.
 * <p>
 * For every purchased line item the earning party and revenue share are resolved by scope:
 * <ul>
 *     <li>{@link PurchaseScope#COURSE} &rarr; the course creator earns the creator share.</li>
 *     <li>{@link PurchaseScope#CLASS} &rarr; the class' default instructor earns the instructor share.</li>
 * </ul>
 * A line item with no configured revenue share, or a zero share on the earning side, credits
 * nobody.
 *
 * <h2>Durability</h2>
 * Money owed to a real person must not vanish into a log line, so this listener is a persistent
 * Spring Modulith event listener rather than a plain {@code @EventListener}:
 * <ul>
 *     <li>{@code @TransactionalEventListener} makes it a {@code TransactionalApplicationListener},
 *     which is what causes Modulith's {@code PersistentApplicationEventMulticaster} to write a row
 *     into {@code event_publication} <em>before</em> the listener runs.</li>
 *     <li>{@code fallbackExecution = true} is required: {@code OrderCompletedEvent} is published
 *     from {@code OrderServiceImpl}/{@code OrderPaymentServiceImpl}, neither of which is
 *     {@code @Transactional}, so without the fallback the listener would never be invoked at all.</li>
 *     <li>{@code @Async} keeps checkout available - crediting runs off the request thread, after the
 *     order has already been recorded, so a credit failure can never fail the order.</li>
 *     <li>Failures are rethrown. Modulith's completion advisor only marks the publication complete
 *     on a clean return, so a failed credit stays as an incomplete publication and is retried by
 *     {@link IncompleteWalletCreditRetryJob} and on restart
 *     ({@code spring.modulith.republish-outstanding-events-on-restart=true}).</li>
 * </ul>
 * Retrying is safe by construction: every credit goes through
 * {@code WalletService#creditSaleIdempotent} keyed on {@code orderId:lineItemId}, which is backed by
 * the partial unique index {@code uq_user_wallet_txn_sale_reference} on
 * {@code user_wallet_transactions (reference) WHERE transaction_type = 'SALE'}. Already-credited
 * items are skipped, so only the items that actually failed are credited on a retry.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCaptureWalletCreditListener {

    private static final String STATUS_CAPTURED = "CAPTURED";
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final WalletService walletService;
    private final CourseInfoService courseInfoService;
    private final InstructorLookupService instructorLookupService;
    private final ClassDefinitionLookupService classDefinitionLookupService;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(fallbackExecution = true)
    public void handleOrderCompleted(OrderCompletedEvent event) {
        if (event == null || event.order() == null) {
            return;
        }
        OrderResponse order = event.order();
        if (!STATUS_CAPTURED.equalsIgnoreCase(order.getPaymentStatus())) {
            return;
        }
        if (CollectionUtils.isEmpty(order.getItems())) {
            return;
        }

        // Every item is attempted even when an earlier one fails, so one broken line item cannot
        // starve the other earners on the same order. Failures are collected and rethrown at the
        // end so the event publication is left incomplete and the whole order is retried; the
        // items that already succeeded are skipped on the retry by the idempotent credit.
        List<String> failedItems = new ArrayList<>();
        Exception firstFailure = null;
        for (CartItemResponse item : order.getItems()) {
            try {
                creditItem(order, item);
            } catch (Exception ex) {
                log.error("Failed to credit wallet for order {} item {}: {}",
                        order.getId(), item.getId(), ex.getMessage(), ex);
                failedItems.add(String.valueOf(item.getId()));
                if (firstFailure == null) {
                    firstFailure = ex;
                }
            }
        }

        if (firstFailure != null) {
            throw new WalletCreditFailedException(
                    "Failed to credit " + failedItems.size() + " wallet(s) for order " + order.getId()
                            + " (line items " + String.join(", ", failedItems) + "). The event"
                            + " publication is left incomplete and will be retried.",
                    firstFailure);
        }
    }

    private void creditItem(OrderResponse order, CartItemResponse item) {
        BigDecimal total = item.getTotal();
        if (total == null || total.signum() <= 0) {
            return;
        }

        Map<String, Object> metadata = item.getMetadata();
        UUID courseUuid = parseUuid(metadata, "course_uuid");
        UUID classDefinitionUuid = parseUuid(metadata, "class_definition_uuid");
        PurchaseScope scope = determineScope(courseUuid, classDefinitionUuid);
        if (scope == null) {
            log.debug("Skipping wallet credit for order {} item {}: no course/class scope",
                    order.getId(), item.getId());
            return;
        }

        Earning earning = switch (scope) {
            case CLASS -> resolveClassEarning(classDefinitionUuid, total);
            case COURSE -> resolveCourseEarning(courseUuid, total);
        };
        if (earning == null) {
            return;
        }

        String reference = buildReference(order, item);
        boolean credited = walletService.creditSaleIdempotent(
                earning.userUuid(),
                earning.amount(),
                order.getCurrencyCode(),
                reference,
                earning.description());
        if (credited) {
            log.info("Credited {} {} to user {} for {} sale on order {} (item {})",
                    earning.amount(), order.getCurrencyCode(), earning.userUuid(),
                    scope, order.getId(), item.getId());
        } else {
            log.debug("Wallet credit already applied for reference {}", reference);
        }
    }

    private Earning resolveCourseEarning(UUID courseUuid, BigDecimal total) {
        Optional<UUID> creatorUserUuid = courseInfoService.getCourseCreatorUserUuid(courseUuid);
        if (creatorUserUuid.isEmpty()) {
            log.warn("No course creator user resolved for course {}", courseUuid);
            return null;
        }
        BigDecimal share = courseInfoService.getRevenueShare(courseUuid)
                .map(RevenueShare::creatorSharePercentage)
                .orElse(null);
        BigDecimal amount = applyShare(total, share);
        if (amount == null) {
            return null;
        }
        return new Earning(creatorUserUuid.get(), amount,
                "Course sale earnings (course " + courseUuid + ")");
    }

    private Earning resolveClassEarning(UUID classDefinitionUuid, BigDecimal total) {
        Optional<ClassDefinitionSnapshot> snapshot =
                classDefinitionLookupService.findByUuid(classDefinitionUuid);
        if (snapshot.isEmpty() || snapshot.get().courseUuid() == null) {
            log.warn("No course resolved for class definition {}", classDefinitionUuid);
            return null;
        }
        UUID courseUuid = snapshot.get().courseUuid();

        UUID instructorUuid = classDefinitionLookupService
                .findDefaultInstructorUuid(classDefinitionUuid)
                .orElse(null);
        if (instructorUuid == null) {
            log.warn("No default instructor for class definition {}", classDefinitionUuid);
            return null;
        }
        Optional<UUID> instructorUserUuid = instructorLookupService.getInstructorUserUuid(instructorUuid);
        if (instructorUserUuid.isEmpty()) {
            log.warn("No user resolved for instructor {}", instructorUuid);
            return null;
        }

        BigDecimal share = courseInfoService.getRevenueShare(courseUuid)
                .map(RevenueShare::instructorSharePercentage)
                .orElse(null);
        BigDecimal amount = applyShare(total, share);
        if (amount == null) {
            return null;
        }
        return new Earning(instructorUserUuid.get(), amount,
                "Class sale earnings (class " + classDefinitionUuid + ")");
    }

    private BigDecimal applyShare(BigDecimal total, BigDecimal sharePercentage) {
        if (sharePercentage == null || sharePercentage.signum() <= 0) {
            return null;
        }
        BigDecimal amount = total.multiply(sharePercentage)
                .divide(HUNDRED, 2, RoundingMode.HALF_UP);
        return amount.signum() > 0 ? amount : null;
    }

    private PurchaseScope determineScope(UUID courseUuid, UUID classDefinitionUuid) {
        if (classDefinitionUuid != null) {
            return PurchaseScope.CLASS;
        }
        if (courseUuid != null) {
            return PurchaseScope.COURSE;
        }
        return null;
    }

    private String buildReference(OrderResponse order, CartItemResponse item) {
        String lineItemId = StringUtils.hasText(item.getId()) ? item.getId() : item.getVariantId();
        return StringUtils.hasText(lineItemId)
                ? order.getId() + ":" + lineItemId
                : order.getId();
    }

    private UUID parseUuid(Map<String, Object> metadata, String key) {
        if (CollectionUtils.isEmpty(metadata)) {
            return null;
        }
        Object value = metadata.get(key);
        if (value instanceof UUID uuid) {
            return uuid;
        }
        if (value instanceof String str && StringUtils.hasText(str)) {
            try {
                return UUID.fromString(str);
            } catch (IllegalArgumentException ex) {
                log.warn("Unable to parse UUID from metadata {}={}", key, str);
            }
        }
        return null;
    }

    private record Earning(UUID userUuid, BigDecimal amount, String description) { }
}
