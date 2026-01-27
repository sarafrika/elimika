package apps.sarafrika.elimika.revenue.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record RevenueSaleLineItemDTO(
        @JsonProperty("order_id")
        String orderId,
        @JsonProperty("order_number")
        String orderNumber,
        @JsonProperty("order_created_at")
        OffsetDateTime orderCreatedAt,
        @JsonProperty("payment_status")
        String paymentStatus,
        @JsonProperty("order_currency_code")
        String orderCurrencyCode,
        @JsonProperty("order_subtotal_amount")
        BigDecimal orderSubtotalAmount,
        @JsonProperty("order_total_amount")
        BigDecimal orderTotalAmount,
        @JsonProperty("platform_fee_amount")
        BigDecimal platformFeeAmount,
        @JsonProperty("platform_fee_currency")
        String platformFeeCurrency,
        @JsonProperty("platform_fee_rule_uuid")
        UUID platformFeeRuleUuid,
        @JsonProperty("buyer_user_uuid")
        UUID buyerUserUuid,
        @JsonProperty("customer_email")
        String customerEmail,
        @JsonProperty("line_item_id")
        String lineItemId,
        @JsonProperty("variant_id")
        String variantId,
        @JsonProperty("title")
        String title,
        @JsonProperty("quantity")
        int quantity,
        @JsonProperty("unit_price")
        BigDecimal unitPrice,
        @JsonProperty("subtotal")
        BigDecimal subtotal,
        @JsonProperty("total")
        BigDecimal total,
        @JsonProperty("scope")
        String scope,
        @JsonProperty("course_uuid")
        UUID courseUuid,
        @JsonProperty("class_definition_uuid")
        UUID classDefinitionUuid,
        @JsonProperty("student_uuid")
        UUID studentUuid
) {
}
