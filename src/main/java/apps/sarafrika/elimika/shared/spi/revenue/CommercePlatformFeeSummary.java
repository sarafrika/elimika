package apps.sarafrika.elimika.shared.spi.revenue;

import java.math.BigDecimal;

public record CommercePlatformFeeSummary(
        String currencyCode,
        BigDecimal totalAmount
) {
}
