package apps.sarafrika.elimika.shared.spi.revenue;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A settled purchase line read back with its split. {@code splitUuid} is the purchase line's uuid and
 * {@code lineReference} is {@code orderId:lineItemId}, the wallet SALE reference without its leg suffix.
 * creator.net + delivery.net + platformFeeTotal + retained = lineGross.
 */
public record SaleSplitView(
        UUID splitUuid,
        String orderId,
        String orderNumber,
        String lineItemId,
        String lineReference,
        String title,
        UUID courseUuid,
        UUID classDefinitionUuid,
        String currencyCode,
        BigDecimal lineGross,
        BigDecimal creatorPercentage,
        SaleSplitDetail.Leg creator,
        SaleDeliveryKind deliveryKind,
        SaleSplitDetail.Leg delivery,
        BigDecimal platformFeeTotal,
        BigDecimal retained,
        boolean reconciles,
        boolean backfilled,
        SaleSplitDetail.FeeRule feeRule,
        OffsetDateTime settledAt
) {
}
