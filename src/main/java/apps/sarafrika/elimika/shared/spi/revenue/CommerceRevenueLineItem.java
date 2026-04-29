package apps.sarafrika.elimika.shared.spi.revenue;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CommerceRevenueLineItem(
        String orderId,
        OffsetDateTime orderCreatedAt,
        String currencyCode,
        BigDecimal itemTotal,
        int quantity,
        PurchaseScope scope,
        UUID courseUuid,
        UUID classDefinitionUuid
) {
}
