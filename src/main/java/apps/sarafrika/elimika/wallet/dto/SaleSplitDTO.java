package apps.sarafrika.elimika.wallet.dto;

import apps.sarafrika.elimika.wallet.service.SaleSplitRole;
import apps.sarafrika.elimika.wallet.service.WalletPurse;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Schema(name = "SaleSplit", description = "How a captured purchase line was split between the course creator, the "
        + "delivery party (instructor or hiring organisation) and the platform, recorded at the point of purchase. "
        + "creator.net + delivery.net + platform_fee_total + retained = line_gross.")
public record SaleSplitDTO(
        @Schema(description = "The split's id, which is the purchase line's uuid")
        @JsonProperty("uuid") UUID uuid,
        @JsonProperty("order_id") String orderId,
        @Schema(description = "The order's display number, when it has one")
        @JsonProperty("order_number") String orderNumber,
        @JsonProperty("line_item_id") String lineItemId,
        @JsonProperty("title") String title,
        @JsonProperty("course_uuid") UUID courseUuid,
        @JsonProperty("class_definition_uuid") UUID classDefinitionUuid,
        @Schema(description = "When the line was settled into wallets (UTC); the order time on older lines")
        @JsonProperty("captured_at") OffsetDateTime capturedAt,
        @JsonProperty("currency_code") String currencyCode,
        @Schema(description = "What the buyer paid for the line", example = "50000.00")
        @JsonProperty("line_gross") BigDecimal lineGross,
        @Schema(description = "The course creator's percentage of the line; null on backfilled splits", example = "30")
        @JsonProperty("creator_percentage") BigDecimal creatorPercentage,
        @Schema(description = "The viewer's part in the sale; null for admins. Asked for one purse, the role of "
                + "that purse's leg, and your_* covers that leg only. Otherwise a creator who also delivered is "
                + "COURSE_CREATOR, with both legs summed into your_gross, your_fee and your_net")
        @JsonProperty("your_role") SaleSplitRole yourRole,
        @Schema(description = "The purse your_net was paid into: COURSE_CREATOR, INSTRUCTOR or EARNINGS. Null for "
                + "admins, and when a person was paid on both legs and no purse was asked for (the legs went to two "
                + "purses; see transactions[].purse)")
        @JsonProperty("your_purse") WalletPurse yourPurse,
        @JsonProperty("your_gross") BigDecimal yourGross,
        @JsonProperty("your_fee") BigDecimal yourFee,
        @JsonProperty("your_net") BigDecimal yourNet,
        @JsonProperty("creator") SaleSplitPartyDTO creator,
        @JsonProperty("delivery") SaleSplitPartyDTO delivery,
        @JsonProperty("platform_fee_total") BigDecimal platformFeeTotal,
        @Schema(description = "Collected, not fee, and paid to nobody, e.g. a leg with no earner")
        @JsonProperty("retained") BigDecimal retained,
        @Schema(description = "False when the figures did not add up at settlement; flagged for review")
        @JsonProperty("reconciles") boolean reconciles,
        @Schema(description = "Rebuilt from wallet rows for a line settled before splits were recorded")
        @JsonProperty("backfilled") boolean backfilled,
        @JsonProperty("fee_rule") SaleFeeRuleDTO feeRule,
        @Schema(description = "The viewer's SALE and PLATFORM_FEE rows for this line; every party's rows for admins")
        @JsonProperty("transactions") List<SaleSplitTransactionDTO> transactions
) {
}
