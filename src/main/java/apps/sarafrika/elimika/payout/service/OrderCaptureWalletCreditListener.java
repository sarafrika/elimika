package apps.sarafrika.elimika.payout.service;

import apps.sarafrika.elimika.course.spi.CourseInfoService;
import apps.sarafrika.elimika.course.spi.CourseInfoService.RevenueShare;
import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.shared.dto.commerce.CartItemResponse;
import apps.sarafrika.elimika.shared.dto.commerce.OrderResponse;
import apps.sarafrika.elimika.shared.event.commerce.OrderCompletedEvent;
import apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService;
import apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService.ClassDefinitionSnapshot;
import apps.sarafrika.elimika.shared.spi.revenue.PurchaseLineSettlement;
import apps.sarafrika.elimika.shared.spi.revenue.PurchaseScope;
import apps.sarafrika.elimika.shared.spi.revenue.PurchaseSettlementRecorder;
import apps.sarafrika.elimika.shared.utils.commerce.PlatformFeeApportionment;
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
 * <h2>The platform fee comes off the top</h2>
 * The platform fee is charged before any revenue share is applied, so an earner is credited a share
 * of the <em>net</em>. The fee is computed once on the whole order but crediting is per line, so it
 * is first apportioned across the lines by {@link PlatformFeeApportionment} - to the cent, summing
 * exactly to the fee, with no line able to leak or invent money.
 *
 * <h2>The unallocated remainder is booked, not lost</h2>
 * A course splits revenue between its creator and its instructor, but only one of them is credited
 * per purchase scope; the other side's percentage is credited to nobody. That gap used to be
 * discoverable only by subtraction. Every settled line now reports gross, fee, credited and retained
 * back to commerce through {@link PurchaseSettlementRecorder}, so the money the platform kept is a
 * figure someone can read. This deliberately does <em>not</em> change who is credited - it only makes
 * the gap measurable, pending the double-entry ledger where it will eventually be booked to
 * {@code PLATFORM_UNALLOCATED_REVENUE}.
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
    private static final BigDecimal NO_MONEY =
            BigDecimal.ZERO.setScale(PlatformFeeApportionment.MONEY_SCALE);

    private final WalletService walletService;
    private final CourseInfoService courseInfoService;
    private final InstructorLookupService instructorLookupService;
    private final ClassDefinitionLookupService classDefinitionLookupService;
    private final PurchaseSettlementRecorder purchaseSettlementRecorder;

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

        // The order-level platform fee is split across the lines once, up front, so that every line
        // is netted against its own share of the same fee and the shares sum back to it exactly.
        List<CartItemResponse> items = order.getItems();
        List<BigDecimal> apportionedFees = PlatformFeeApportionment.apportionAcrossItems(order);

        // Every item is attempted even when an earlier one fails, so one broken line item cannot
        // starve the other earners on the same order. Failures are collected and rethrown at the
        // end so the event publication is left incomplete and the whole order is retried; the
        // items that already succeeded are skipped on the retry by the idempotent credit.
        List<String> failedItems = new ArrayList<>();
        Exception firstFailure = null;
        for (int index = 0; index < items.size(); index++) {
            CartItemResponse item = items.get(index);
            try {
                creditItem(order, item, apportionedFees.get(index));
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

    private void creditItem(OrderResponse order, CartItemResponse item, BigDecimal apportionedFee) {
        BigDecimal gross = item.getTotal();
        if (gross == null || gross.signum() <= 0) {
            // No money was collected on this line, so there is nothing to charge, credit or retain.
            return;
        }

        // The fee is taken off the top; the revenue share only ever applies to what is left. The
        // apportionment already caps at the line total, but a fee can never exceed its own line.
        BigDecimal platformFee = apportionedFee == null ? NO_MONEY : apportionedFee;
        if (platformFee.compareTo(gross) > 0) {
            platformFee = gross;
        }
        BigDecimal net = gross.subtract(platformFee);

        Map<String, Object> metadata = item.getMetadata();
        UUID courseUuid = parseUuid(metadata, "course_uuid");
        UUID classDefinitionUuid = parseUuid(metadata, "class_definition_uuid");
        PurchaseScope scope = determineScope(courseUuid, classDefinitionUuid);
        if (scope == null) {
            log.debug("Skipping wallet credit for order {} item {}: no course/class scope",
                    order.getId(), item.getId());
            // Still settled: nobody is credited, so everything after the fee is retained.
            recordSettlement(order, item, gross, platformFee, NO_MONEY);
            return;
        }

        Earning earning = switch (scope) {
            case CLASS -> resolveClassEarning(classDefinitionUuid, net);
            case COURSE -> resolveCourseEarning(courseUuid, net);
        };
        if (earning == null) {
            recordSettlement(order, item, gross, platformFee, NO_MONEY);
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
            log.info("Credited {} {} to user {} for {} sale on order {} (item {}); gross {}, platform fee {}",
                    earning.amount(), order.getCurrencyCode(), earning.userUuid(),
                    scope, order.getId(), item.getId(), gross, platformFee);
        } else {
            log.debug("Wallet credit already applied for reference {}", reference);
        }

        recordSettlement(order, item, gross, platformFee, earning.amount());
    }

    /**
     * Books what became of this line's money. The retained amount is whatever is left once the fee
     * has been taken and the earner paid - on a 70/20 course sold as a course, that is the
     * instructor's 10% that nobody was credited. It is derived by subtraction here so it can never
     * disagree with the other three figures.
     */
    private void recordSettlement(
            OrderResponse order,
            CartItemResponse item,
            BigDecimal gross,
            BigDecimal platformFee,
            BigDecimal creditedAmount
    ) {
        BigDecimal retained = gross.subtract(platformFee).subtract(creditedAmount);
        purchaseSettlementRecorder.recordLineSettlement(new PurchaseLineSettlement(
                order.getId(),
                item.getId(),
                gross,
                platformFee,
                creditedAmount,
                retained));
    }

    private Earning resolveCourseEarning(UUID courseUuid, BigDecimal netOfPlatformFee) {
        Optional<UUID> creatorUserUuid = courseInfoService.getCourseCreatorUserUuid(courseUuid);
        if (creatorUserUuid.isEmpty()) {
            log.warn("No course creator user resolved for course {}", courseUuid);
            return null;
        }
        BigDecimal share = courseInfoService.getRevenueShare(courseUuid)
                .map(RevenueShare::creatorSharePercentage)
                .orElse(null);
        BigDecimal amount = applyShare(netOfPlatformFee, share);
        if (amount == null) {
            return null;
        }
        return new Earning(creatorUserUuid.get(), amount,
                "Course sale earnings (course " + courseUuid + ")");
    }

    private Earning resolveClassEarning(UUID classDefinitionUuid, BigDecimal netOfPlatformFee) {
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
        BigDecimal amount = applyShare(netOfPlatformFee, share);
        if (amount == null) {
            return null;
        }
        return new Earning(instructorUserUuid.get(), amount,
                "Class sale earnings (class " + classDefinitionUuid + ")");
    }

    /**
     * Applies a revenue share to the line total <em>net of the platform fee</em>. Returns null when
     * nothing is earned, which the caller books as retained rather than credited.
     */
    private BigDecimal applyShare(BigDecimal netOfPlatformFee, BigDecimal sharePercentage) {
        if (sharePercentage == null || sharePercentage.signum() <= 0) {
            return null;
        }
        if (netOfPlatformFee.signum() <= 0) {
            return null;
        }
        BigDecimal amount = netOfPlatformFee.multiply(sharePercentage)
                .divide(HUNDRED, PlatformFeeApportionment.MONEY_SCALE, RoundingMode.HALF_UP);
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
