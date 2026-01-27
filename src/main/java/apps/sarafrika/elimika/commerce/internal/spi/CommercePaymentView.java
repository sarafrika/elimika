package apps.sarafrika.elimika.commerce.internal.spi;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CommercePaymentView(
        UUID paymentUuid,
        UUID orderUuid,
        BigDecimal orderTotalAmount,
        String orderCurrencyCode,
        String provider,
        String status,
        BigDecimal amount,
        String currencyCode,
        String externalReference,
        LocalDateTime processedAt
) {
}
