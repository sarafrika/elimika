package apps.sarafrika.elimika.commerce.purchase.service.impl;

import apps.sarafrika.elimika.commerce.purchase.entity.CommercePurchaseItem;
import apps.sarafrika.elimika.commerce.purchase.repository.CommercePurchaseItemRepository;
import apps.sarafrika.elimika.shared.spi.revenue.PurchaseLineSettlement;
import apps.sarafrika.elimika.shared.spi.revenue.PurchaseSettlementRecorder;
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
        purchaseItemRepository.save(item);

        log.debug("Recorded settlement for order {} line {}: gross {}, fee {}, credited {}, retained {}",
                settlement.orderId(), settlement.lineItemId(), settlement.grossAmount(),
                settlement.platformFeeAmount(), settlement.creditedAmount(), settlement.retainedAmount());
    }
}
