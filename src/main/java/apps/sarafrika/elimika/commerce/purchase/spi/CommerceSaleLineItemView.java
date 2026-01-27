package apps.sarafrika.elimika.commerce.purchase.spi;

import apps.sarafrika.elimika.commerce.purchase.enums.PurchaseScope;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CommerceSaleLineItemView(
        String orderId,
        String orderNumber,
        OffsetDateTime orderCreatedAt,
        String paymentStatus,
        String orderCurrencyCode,
        BigDecimal orderSubtotalAmount,
        BigDecimal orderTotalAmount,
        BigDecimal platformFeeAmount,
        String platformFeeCurrency,
        UUID platformFeeRuleUuid,
        UUID buyerUserUuid,
        String customerEmail,
        String lineItemId,
        String variantId,
        String title,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal,
        BigDecimal total,
        PurchaseScope scope,
        UUID courseUuid,
        UUID classDefinitionUuid,
        UUID studentUuid
) {
}
