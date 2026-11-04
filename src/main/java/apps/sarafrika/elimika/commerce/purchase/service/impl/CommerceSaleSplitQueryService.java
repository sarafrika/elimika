package apps.sarafrika.elimika.commerce.purchase.service.impl;

import apps.sarafrika.elimika.commerce.purchase.entity.CommercePurchase;
import apps.sarafrika.elimika.commerce.purchase.entity.CommercePurchaseItem;
import apps.sarafrika.elimika.commerce.purchase.repository.CommercePurchaseItemRepository;
import apps.sarafrika.elimika.shared.spi.revenue.SaleLegSide;
import apps.sarafrika.elimika.shared.spi.revenue.SaleLegStatus;
import apps.sarafrika.elimika.shared.spi.revenue.SalePartyType;
import apps.sarafrika.elimika.shared.spi.revenue.SaleSplitDetail;
import apps.sarafrika.elimika.shared.spi.revenue.SaleSplitQueryService;
import apps.sarafrika.elimika.shared.spi.revenue.SaleSplitView;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads the split stamped on each settled purchase line by {@link CommercePurchaseSettlementRecorder}. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommerceSaleSplitQueryService implements SaleSplitQueryService {

    private final CommercePurchaseItemRepository purchaseItemRepository;

    @Override
    public Page<SaleSplitView> findPaidTo(SalePartyType ownerType, UUID ownerUuid, Pageable pageable) {
        return findPaidTo(ownerType, ownerUuid, null, pageable);
    }

    @Override
    public Page<SaleSplitView> findPaidTo(SalePartyType ownerType, UUID ownerUuid, SaleLegSide side,
                                          Pageable pageable) {
        Pageable unsorted = pageable.isPaged() ? PageRequest.of(pageable.getPageNumber(), pageable.getPageSize())
                : pageable;
        Page<CommercePurchaseItem> page = side == null
                ? purchaseItemRepository.findSplitsPaidTo(ownerType, ownerUuid, SaleLegStatus.PAID, unsorted)
                : side == SaleLegSide.CREATOR
                ? purchaseItemRepository.findSplitsPaidToCreator(ownerType, ownerUuid, SaleLegStatus.PAID, unsorted)
                : purchaseItemRepository.findSplitsPaidToDelivery(ownerType, ownerUuid, SaleLegStatus.PAID, unsorted);
        return page.map(CommerceSaleSplitQueryService::toView);
    }

    @Override
    public Optional<SaleSplitView> findByUuid(UUID splitUuid) {
        return purchaseItemRepository.findSplitByUuid(splitUuid).map(CommerceSaleSplitQueryService::toView);
    }

    @Override
    public List<SaleSplitView> findByOrderId(String orderId) {
        return purchaseItemRepository.findSplitsByOrderId(orderId).stream()
                .map(CommerceSaleSplitQueryService::toView).toList();
    }

    @Override
    public Map<String, UUID> findSplitUuidsByLineReference(Collection<String> lineReferences) {
        if (lineReferences == null || lineReferences.isEmpty()) {
            return Map.of();
        }
        Map<String, UUID> result = new HashMap<>();
        for (CommercePurchaseItem item : purchaseItemRepository.findSplitsByLineReference(lineReferences)) {
            result.put(item.getPurchase().getOrderId() + ":" + item.getLineItemId(), item.getUuid());
        }
        return result;
    }

    private static SaleSplitView toView(CommercePurchaseItem item) {
        CommercePurchase purchase = item.getPurchase();
        BigDecimal gross = zeroIfNull(item.getTotal());
        BigDecimal creatorNet = zeroIfNull(item.getCreatorCreditedAmount());
        SaleSplitDetail.Leg creator = new SaleSplitDetail.Leg(item.getCreatorOwnerType(), item.getCreatorOwnerUuid(),
                item.getCreatorStatus(), item.getCreatorGross(), item.getCreatorFee(), creatorNet);
        SaleSplitDetail.Leg delivery = new SaleSplitDetail.Leg(item.getDeliveryOwnerType(), item.getDeliveryOwnerUuid(),
                item.getDeliveryStatus(), item.getDeliveryGross(), item.getDeliveryFee(), item.getDeliveryNet());
        SaleSplitDetail.FeeRule feeRule = item.getFeeRuleUuid() == null && item.getFeeMode() == null
                && item.getFeeEvaluatedAt() == null ? null
                : new SaleSplitDetail.FeeRule(item.getFeeRuleUuid(), item.getFeeRuleKey(), item.getFeeMode(),
                item.getFeeRate(), item.getFeeRateCurrency(), item.getFeeWaived(), item.getFeeDiscountPercentage(),
                item.getFeeDiscountAmount(), item.getFeeEvaluatedAt());
        BigDecimal fee = zeroIfNull(item.getPlatformFeeAmount());
        BigDecimal retained = item.getRetainedAmount() != null ? item.getRetainedAmount()
                : gross.subtract(fee).subtract(creator.net()).subtract(delivery.net());
        OffsetDateTime settledAt = item.getSettledAt() != null ? item.getSettledAt()
                : purchase.getOrderCreatedAt() != null ? purchase.getOrderCreatedAt()
                : item.getCreatedDate() == null ? null : item.getCreatedDate().atOffset(ZoneOffset.UTC);
        return new SaleSplitView(
                item.getUuid(),
                purchase.getOrderId(),
                purchase.getOrderNumber(),
                item.getLineItemId(),
                purchase.getOrderId() + ":" + item.getLineItemId(),
                item.getTitle(),
                item.getCourseUuid(),
                item.getClassDefinitionUuid(),
                purchase.getOrderCurrencyCode(),
                gross,
                item.getCreatorPercentage(),
                creator,
                item.getDeliveryKind(),
                delivery,
                fee,
                retained,
                !Boolean.FALSE.equals(item.getSplitReconciles()),
                item.isSplitBackfilled(),
                feeRule,
                settledAt);
    }

    private static BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
