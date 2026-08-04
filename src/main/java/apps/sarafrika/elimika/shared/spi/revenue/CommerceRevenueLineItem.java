package apps.sarafrika.elimika.shared.spi.revenue;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A captured purchase line, as the revenue dashboards read it.
 *
 * @param itemPlatformFeeAmount the order's platform fee apportioned to this line. Null on lines
 *                              settled before the fee was charged off the top; those wallets really
 *                              were credited on gross, so reporting must treat null as zero rather
 *                              than guess.
 * @param creditedAmount        what the earner's wallet was actually credited for this line, or null
 *                              if the credit has not been recorded (historical, or still pending).
 */
public record CommerceRevenueLineItem(
        String orderId,
        OffsetDateTime orderCreatedAt,
        String currencyCode,
        BigDecimal itemTotal,
        int quantity,
        PurchaseScope scope,
        UUID courseUuid,
        UUID classDefinitionUuid,
        BigDecimal itemPlatformFeeAmount,
        BigDecimal creditedAmount
) {
}
