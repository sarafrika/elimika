package apps.sarafrika.elimika.commerce.purchase.spi;

import java.math.BigDecimal;

public record CommercePlatformFeeSummary(
        String currencyCode,
        BigDecimal totalAmount
) {
}
