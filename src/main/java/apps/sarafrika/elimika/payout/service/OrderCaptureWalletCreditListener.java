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
import apps.sarafrika.elimika.shared.spi.revenue.SaleDeliveryKind;
import apps.sarafrika.elimika.shared.spi.revenue.SaleFeeMode;
import apps.sarafrika.elimika.shared.spi.revenue.SaleLegStatus;
import apps.sarafrika.elimika.shared.spi.revenue.SalePartyType;
import apps.sarafrika.elimika.shared.spi.revenue.SaleSplitDetail;
import apps.sarafrika.elimika.shared.utils.commerce.PlatformFeeApportionment;
import apps.sarafrika.elimika.systemconfig.dto.PlatformFeeConfig;
import apps.sarafrika.elimika.systemconfig.dto.RuleContext;
import apps.sarafrika.elimika.systemconfig.service.RuleEvaluationService;
import apps.sarafrika.elimika.wallet.service.WalletOwner;
import apps.sarafrika.elimika.wallet.service.WalletOwnerType;
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

        FeeRule feeRule = feeRuleAt(order);
        List<String> failedItems = new ArrayList<>();
        Exception firstFailure = null;
        for (CartItemResponse item : order.getItems()) {
            try {
                settleLine(order, item, feeRule);
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

    private void settleLine(OrderResponse order, CartItemResponse item, FeeRule feeRule) {
        BigDecimal gross = item.getTotal();
        if (gross == null || gross.signum() <= 0) {
            return;
        }
        OffsetDateTime settledAt = OffsetDateTime.now(ZoneOffset.UTC);
        Parties parties = resolveParties(item);
        if (parties == null) {
            SaleSplitDetail.Leg notSplit = SaleSplitDetail.Leg.unpaid(null, null, SaleLegStatus.NOT_SPLIT);
            record(order, item, gross, NO_MONEY, NO_MONEY, NO_MONEY,
                    new SaleSplitDetail(null, notSplit, null, notSplit, feeRule.snapshot(), settledAt));
            return;
        }

        RevenueSplit split = RevenueSplit.of(gross, parties.creatorPercentage(), feeRule.feeOnShare());
        String reference = reference(order, item);
        SaleSplitDetail.Leg creator = pay(parties.creator(), split.creator(), order, reference + ":creator",
                parties.description());
        SaleSplitDetail.Leg delivery = pay(parties.delivery(), split.delivery(), order, reference + ":delivery",
                parties.description());
        BigDecimal fee = creator.fee().add(delivery.fee());
        BigDecimal credited = creator.net().add(delivery.net());
        record(order, item, gross, fee, credited, creator.net(), new SaleSplitDetail(parties.creatorPercentage(),
                creator, parties.deliveryKind(), delivery, feeRule.snapshot(), settledAt));
    }

    /** Settles one leg; an unpaid leg (nobody, or nothing, to pay) is retained and says why. */
    private SaleSplitDetail.Leg pay(WalletOwner owner, RevenueSplit.Leg leg, OrderResponse order, String reference,
                                    String description) {
        if (owner == null) {
            return SaleSplitDetail.Leg.unpaid(null, null, SaleLegStatus.NO_EARNER);
        }
        SalePartyType ownerType = SalePartyType.valueOf(owner.type().name());
        if (!leg.earnsAnything()) {
            return SaleSplitDetail.Leg.unpaid(ownerType, owner.uuid(), SaleLegStatus.NO_SHARE);
        }
        boolean settledNow = walletService.settleSaleShare(
                owner, leg.gross(), leg.platformFee(), order.getCurrencyCode(), reference, description);
        log.info("{} {} {} share {} for {} (fee {}) on order {}", settledNow ? "Settled" : "Already settled",
                order.getCurrencyCode(), leg.gross(), reference, owner, leg.platformFee(), order.getId());
        return new SaleSplitDetail.Leg(ownerType, owner.uuid(), SaleLegStatus.PAID, leg.gross(), leg.platformFee(),
                leg.net());
    }

    private Parties resolveParties(CartItemResponse item) {
        UUID classUuid = parseUuid(item.getMetadata(), "class_definition_uuid");
        UUID courseUuid = parseUuid(item.getMetadata(), "course_uuid");
        UUID programUuid = null;
        String title = item.getTitle();
        WalletOwner delivery = null;
        if (classUuid != null) {
            Optional<ClassDefinitionSnapshot> snapshot = classDefinitionLookupService.findByUuid(classUuid);
            courseUuid = snapshot.map(ClassDefinitionSnapshot::courseUuid).orElse(null);
            programUuid = snapshot.map(ClassDefinitionSnapshot::programUuid).orElse(null);
            title = snapshot.map(ClassDefinitionSnapshot::title).orElse(title);
        }
        // A class taught under a program, not a course, splits by the program's courses on average.
        boolean program = courseUuid == null && programUuid != null;
        if (courseUuid == null && !program) {
            log.warn("Line {} has no course or program to split by; retaining it", item.getId());
            return null;
        }
        BigDecimal creatorPercentage = (program
                ? courseInfoService.getProgramRevenueShare(programUuid)
                : courseInfoService.getRevenueShare(courseUuid))
                .map(RevenueShare::creatorSharePercentage)
                .orElse(null);
        if (creatorPercentage == null) {
            log.warn("No revenue split for {} {}; retaining line {}", program ? "program" : "course",
                    program ? programUuid : courseUuid, item.getId());
            return null;
        }
        SaleDeliveryKind deliveryKind = null;
        if (classUuid != null) {
            delivery = resolveDelivery(classUuid);
            deliveryKind = delivery != null && delivery.type() == WalletOwnerType.ORGANISATION
                    ? SaleDeliveryKind.ORGANISATION : SaleDeliveryKind.INSTRUCTOR;
        }
        WalletOwner creator = (program
                ? courseInfoService.getTrainingProgramCreatorUserUuid(programUuid)
                : courseInfoService.getCourseCreatorUserUuid(courseUuid))
                .map(WalletOwner::user)
                .orElse(null);
        return new Parties(creatorPercentage, creator, delivery, deliveryKind,
                "Class sale share - " + (title == null ? "class" : title));
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
    private FeeRule feeRuleAt(OrderResponse order) {
        OffsetDateTime at = order.getCreatedAt() == null ? OffsetDateTime.now(ZoneOffset.UTC) : order.getCreatedAt();
        Optional<RuleEvaluationService.RuleMatch<PlatformFeeConfig>> match = ruleEvaluationService
                .resolvePlatformFeeMatch(RuleContext.builder().evaluationInstant(at).build());
        Optional<PlatformFeeConfig> rule = match.map(RuleEvaluationService.RuleMatch::payload);
        UnaryOperator<BigDecimal> feeOnShare = share -> rule.map(config -> config.feeOn(share, at)).orElse(NO_MONEY);
        return new FeeRule(feeOnShare, snapshot(match.orElse(null), at));
    }

    private static SaleSplitDetail.FeeRule snapshot(RuleEvaluationService.RuleMatch<PlatformFeeConfig> match,
                                                    OffsetDateTime at) {
        PlatformFeeConfig config = match == null ? null : match.payload();
        if (config == null) {
            return new SaleSplitDetail.FeeRule(null, null, null, null, null, null, null, null, at);
        }
        boolean discounted = config.discountActive(at);
        return new SaleSplitDetail.FeeRule(
                match.rule() == null ? null : match.rule().getUuid(),
                match.rule() == null ? null : match.rule().getKey(),
                config.mode() == null ? null : SaleFeeMode.valueOf(config.mode().name()),
                config.amount(),
                config.currency(),
                config.waiverActive(at),
                discounted ? config.discount().percentage() : null,
                discounted && config.discount().percentage() == null ? config.discount().amount() : null,
                at);
    }

    private void record(OrderResponse order, CartItemResponse item, BigDecimal gross, BigDecimal fee, BigDecimal credited,
                        BigDecimal creatorNet, SaleSplitDetail split) {
        purchaseSettlementRecorder.recordLineSettlement(new PurchaseLineSettlement(order.getId(), item.getId(), gross,
                fee, credited, gross.subtract(fee).subtract(credited), creatorNet, split));
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

    private record Parties(BigDecimal creatorPercentage, WalletOwner creator, WalletOwner delivery,
                           SaleDeliveryKind deliveryKind, String description) { }

    /** Prices one earner's fee, and says which rule did so for the split's record. */
    private record FeeRule(UnaryOperator<BigDecimal> feeOnShare, SaleSplitDetail.FeeRule snapshot) { }
}
