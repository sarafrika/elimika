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
import apps.sarafrika.elimika.shared.spi.revenue.PurchaseSettlementRecorder;
import apps.sarafrika.elimika.shared.utils.commerce.PlatformFeeApportionment;
import apps.sarafrika.elimika.systemconfig.dto.PlatformFeeConfig;
import apps.sarafrika.elimika.systemconfig.dto.RuleContext;
import apps.sarafrika.elimika.systemconfig.service.RuleEvaluationService;
import apps.sarafrika.elimika.wallet.service.WalletOwner;
import apps.sarafrika.elimika.wallet.service.WalletService;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.UnaryOperator;
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
 * Pays each captured class sale line to the course creator and the delivery party (the hiring organisation
 * for a job class, otherwise the instructor), each net of their own platform fee; see {@link RevenueSplit}.
 * Persistent and retried until clean: failures are rethrown and every share is idempotent on its reference.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCaptureWalletCreditListener {

    private static final String STATUS_CAPTURED = "CAPTURED";
    private static final BigDecimal NO_MONEY = BigDecimal.ZERO.setScale(PlatformFeeApportionment.MONEY_SCALE);

    private final WalletService walletService;
    private final CourseInfoService courseInfoService;
    private final InstructorLookupService instructorLookupService;
    private final ClassDefinitionLookupService classDefinitionLookupService;
    private final PurchaseSettlementRecorder purchaseSettlementRecorder;
    private final RuleEvaluationService ruleEvaluationService;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(fallbackExecution = true)
    public void handleOrderCompleted(OrderCompletedEvent event) {
        if (event == null || event.order() == null) {
            return;
        }
        OrderResponse order = event.order();
        if (!STATUS_CAPTURED.equalsIgnoreCase(order.getPaymentStatus()) || CollectionUtils.isEmpty(order.getItems())) {
            return;
        }

        UnaryOperator<BigDecimal> feeOnShare = feeRuleAt(order);
        List<String> failedItems = new ArrayList<>();
        Exception firstFailure = null;
        for (CartItemResponse item : order.getItems()) {
            try {
                settleLine(order, item, feeOnShare);
            } catch (Exception ex) {
                log.error("Failed to settle order {} line {}: {}", order.getId(), item.getId(), ex.getMessage(), ex);
                failedItems.add(String.valueOf(item.getId()));
                if (firstFailure == null) {
                    firstFailure = ex;
                }
            }
        }

        if (firstFailure != null) {
            throw new WalletCreditFailedException(
                    "Failed to settle " + failedItems.size() + " line(s) of order " + order.getId()
                            + " (" + String.join(", ", failedItems) + "); the publication stays incomplete for a retry.",
                    firstFailure);
        }
    }

    private void settleLine(OrderResponse order, CartItemResponse item, UnaryOperator<BigDecimal> feeOnShare) {
        BigDecimal gross = item.getTotal();
        if (gross == null || gross.signum() <= 0) {
            return;
        }
        Parties parties = resolveParties(item);
        if (parties == null) {
            record(order, item, gross, NO_MONEY, NO_MONEY);
            return;
        }

        RevenueSplit split = RevenueSplit.of(gross, parties.creatorPercentage(), feeOnShare);
        String reference = reference(order, item);
        BigDecimal fee = NO_MONEY;
        BigDecimal credited = NO_MONEY;
        if (pay(parties.creator(), split.creator(), order, reference + ":creator", parties.description())) {
            fee = fee.add(split.creator().platformFee());
            credited = credited.add(split.creator().net());
        }
        if (pay(parties.delivery(), split.delivery(), order, reference + ":delivery", parties.description())) {
            fee = fee.add(split.delivery().platformFee());
            credited = credited.add(split.delivery().net());
        }
        record(order, item, gross, fee, credited);
    }

    /** Settles one leg; false when there is nobody to pay or nothing to pay, so the leg is retained. */
    private boolean pay(WalletOwner owner, RevenueSplit.Leg leg, OrderResponse order, String reference, String description) {
        if (owner == null || !leg.earnsAnything()) {
            return false;
        }
        boolean settledNow = walletService.settleSaleShare(
                owner, leg.gross(), leg.platformFee(), order.getCurrencyCode(), reference, description);
        log.info("{} {} {} share {} for {} (fee {}) on order {}", settledNow ? "Settled" : "Already settled",
                order.getCurrencyCode(), leg.gross(), reference, owner, leg.platformFee(), order.getId());
        return true;
    }

    private Parties resolveParties(CartItemResponse item) {
        UUID classUuid = parseUuid(item.getMetadata(), "class_definition_uuid");
        UUID courseUuid = parseUuid(item.getMetadata(), "course_uuid");
        String title = item.getTitle();
        WalletOwner delivery = null;
        if (classUuid != null) {
            Optional<ClassDefinitionSnapshot> snapshot = classDefinitionLookupService.findByUuid(classUuid);
            courseUuid = snapshot.map(ClassDefinitionSnapshot::courseUuid).orElse(null);
            title = snapshot.map(ClassDefinitionSnapshot::title).orElse(title);
        }
        if (courseUuid == null) {
            log.warn("Line {} has no course to split by; retaining it", item.getId());
            return null;
        }
        BigDecimal creatorPercentage = courseInfoService.getRevenueShare(courseUuid)
                .map(RevenueShare::creatorSharePercentage)
                .orElse(null);
        if (creatorPercentage == null) {
            log.warn("Course {} has no revenue split; retaining line {}", courseUuid, item.getId());
            return null;
        }
        if (classUuid != null) {
            delivery = resolveDelivery(classUuid);
        }
        WalletOwner creator = courseInfoService.getCourseCreatorUserUuid(courseUuid).map(WalletOwner::user).orElse(null);
        return new Parties(creatorPercentage, creator, delivery, "Class sale share - " + (title == null ? "class" : title));
    }

    private WalletOwner resolveDelivery(UUID classUuid) {
        Optional<UUID> hiringOrganisation = classDefinitionLookupService.findHiringOrganisationUuid(classUuid);
        if (hiringOrganisation.isPresent()) {
            return WalletOwner.organisation(hiringOrganisation.get());
        }
        return classDefinitionLookupService.findDefaultInstructorUuid(classUuid)
                .flatMap(instructorLookupService::getInstructorUserUuid)
                .map(WalletOwner::user)
                .orElse(null);
    }

    /** The fee rule as it stood when the order was placed, so a retry charges what the first attempt would have. */
    private UnaryOperator<BigDecimal> feeRuleAt(OrderResponse order) {
        OffsetDateTime at = order.getCreatedAt() == null ? OffsetDateTime.now(ZoneOffset.UTC) : order.getCreatedAt();
        Optional<PlatformFeeConfig> rule = ruleEvaluationService.resolvePlatformFee(
                RuleContext.builder().evaluationInstant(at).build());
        return share -> rule.map(config -> config.feeOn(share, at)).orElse(NO_MONEY);
    }

    private void record(OrderResponse order, CartItemResponse item, BigDecimal gross, BigDecimal fee, BigDecimal credited) {
        purchaseSettlementRecorder.recordLineSettlement(new PurchaseLineSettlement(
                order.getId(), item.getId(), gross, fee, credited, gross.subtract(fee).subtract(credited)));
    }

    private static String reference(OrderResponse order, CartItemResponse item) {
        String lineItemId = StringUtils.hasText(item.getId()) ? item.getId() : item.getVariantId();
        return StringUtils.hasText(lineItemId) ? order.getId() + ":" + lineItemId : order.getId();
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

    private record Parties(BigDecimal creatorPercentage, WalletOwner creator, WalletOwner delivery, String description) { }
}
