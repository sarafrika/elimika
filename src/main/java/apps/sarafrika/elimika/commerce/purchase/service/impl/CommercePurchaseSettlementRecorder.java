package apps.sarafrika.elimika.commerce.purchase.service.impl;

import apps.sarafrika.elimika.commerce.purchase.entity.CommercePurchase;
import apps.sarafrika.elimika.commerce.purchase.entity.CommercePurchaseItem;
import apps.sarafrika.elimika.commerce.purchase.repository.CommercePurchaseItemRepository;
import apps.sarafrika.elimika.shared.spi.revenue.PurchaseLineSettlement;
import apps.sarafrika.elimika.shared.spi.revenue.PurchaseSettlementRecorder;
import apps.sarafrika.elimika.shared.spi.revenue.SaleSplitDetail;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Stamps what {@code payout} did with a line's money onto the purchase record that commerce owns.
 * <p>
 * The write is a plain update of three amounts on an existing {@code commerce_purchase_item}; the
 * row itself is created by {@link CommercePurchaseServiceImpl#recordOrder}, which runs first on the
 * same event (see {@link OrderCompletionListener}). A missing row is therefore a real anomaly and is
 * raised rather than swallowed, so the caller's event publication stays incomplete and the write is
 * retried instead of the audit trail quietly having a hole in it.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CommercePurchaseSettlementRecorder implements PurchaseSettlementRecorder {

    private final CommercePurchaseItemRepository purchaseItemRepository;

    @Override
    @Transactional
    public void recordLineSettlement(PurchaseLineSettlement settlement) {
        if (settlement == null) {
            return;
        }
        if (!StringUtils.hasText(settlement.orderId()) || !StringUtils.hasText(settlement.lineItemId())) {
            // Nothing identifies the row to stamp. The credit itself is unaffected.
            log.warn("Cannot record settlement without an order id and line item id (order {}, line {})",
                    settlement.orderId(), settlement.lineItemId());
            return;
        }

        CommercePurchaseItem item = purchaseItemRepository
                .findByOrderIdAndLineItemId(settlement.orderId(), settlement.lineItemId())
                .orElseThrow(() -> new IllegalStateException(
                        "No recorded purchase line " + settlement.lineItemId()
                                + " on order " + settlement.orderId() + " to record settlement against"));

        item.setPlatformFeeAmount(settlement.platformFeeAmount());
        item.setCreditedAmount(settlement.creditedAmount());
        item.setRetainedAmount(settlement.retainedAmount());
        item.setCreatorCreditedAmount(settlement.creatorCreditedAmount());
        stampSplit(item, settlement);
        purchaseItemRepository.saveAndFlush(item);
        rollUpPlatformFee(item.getPurchase());

        log.debug("Recorded settlement for order {} line {}: gross {}, fee {}, credited {}, retained {}",
                settlement.orderId(), settlement.lineItemId(), settlement.grossAmount(),
                settlement.platformFeeAmount(), settlement.creditedAmount(), settlement.retainedAmount());
    }

    /** Fees are charged per earner at settlement, so the purchase's fee is the sum of its settled lines. */
    private void rollUpPlatformFee(CommercePurchase purchase) {
        purchase.setPlatformFeeAmount(purchaseItemRepository.sumPlatformFeeByPurchaseId(purchase.getId()));
        purchase.setPlatformFeeCurrency(purchase.getOrderCurrencyCode());
    }

    /** The per-leg split beside the line totals; a split that does not explain them is kept and flagged. */
    private void stampSplit(CommercePurchaseItem item, PurchaseLineSettlement settlement) {
        SaleSplitDetail split = settlement.split();
        if (split == null) {
            return;
        }
        boolean reconciles = split.reconcilesWith(settlement);
        if (!reconciles) {
            log.error("Split of order {} line {} does not reconcile: creator {} / {} / {}, delivery {} / {} / {}, "
                            + "line fee {}, credited {}, creator credited {}", settlement.orderId(),
                    settlement.lineItemId(), split.creator().gross(), split.creator().fee(), split.creator().net(),
                    split.delivery().gross(), split.delivery().fee(), split.delivery().net(),
                    settlement.platformFeeAmount(), settlement.creditedAmount(), settlement.creatorCreditedAmount());
        }
        item.setCreatorPercentage(split.creatorPercentage());
        SaleSplitDetail.Leg creator = split.creator();
        item.setCreatorOwnerType(creator.ownerType());
        item.setCreatorOwnerUuid(creator.ownerUuid());
        item.setCreatorStatus(creator.status());
        item.setCreatorGross(creator.gross());
        item.setCreatorFee(creator.fee());
        SaleSplitDetail.Leg delivery = split.delivery();
        item.setDeliveryKind(split.deliveryKind());
        item.setDeliveryOwnerType(delivery.ownerType());
        item.setDeliveryOwnerUuid(delivery.ownerUuid());
        item.setDeliveryStatus(delivery.status());
        item.setDeliveryGross(delivery.gross());
        item.setDeliveryFee(delivery.fee());
        item.setDeliveryNet(delivery.net());
        SaleSplitDetail.FeeRule rule = split.feeRule();
        item.setFeeRuleUuid(rule == null ? null : rule.ruleUuid());
        item.setFeeRuleKey(rule == null ? null : rule.ruleKey());
        item.setFeeMode(rule == null ? null : rule.mode());
        item.setFeeRate(rule == null ? null : rule.rate());
        item.setFeeRateCurrency(rule == null ? null : rule.rateCurrency());
        item.setFeeWaived(rule == null ? null : rule.waived());
        item.setFeeDiscountPercentage(rule == null ? null : rule.discountPercentage());
        item.setFeeDiscountAmount(rule == null ? null : rule.discountAmount());
        item.setFeeEvaluatedAt(rule == null ? null : rule.evaluatedAt());
        // A replay keeps the first settlement time, so the split stays dated to the purchase.
        if (item.getSettledAt() == null || item.isSplitBackfilled()) {
            item.setSettledAt(split.settledAt());
        }
        item.setSplitReconciles(reconciles);
        item.setSplitBackfilled(false);
    }
}
