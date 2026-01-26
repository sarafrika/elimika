package apps.sarafrika.elimika.commerce.purchase.spi;

import apps.sarafrika.elimika.commerce.purchase.enums.PurchaseScope;
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
