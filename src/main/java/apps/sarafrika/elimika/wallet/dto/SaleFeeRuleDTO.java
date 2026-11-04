package apps.sarafrika.elimika.wallet.dto;

import apps.sarafrika.elimika.shared.spi.revenue.SaleFeeMode;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(name = "SaleFeeRule", description = "The platform fee rule in force when the order was placed")
public record SaleFeeRuleDTO(
        @Schema(description = "Human-readable rule", example = "10% of each share")
        @JsonProperty("description") String description,
        @Schema(description = "False for lines settled before splits were recorded, whose rule is unknown")
        @JsonProperty("recorded") boolean recorded,
        @JsonProperty("rule_uuid") UUID ruleUuid,
        @JsonProperty("rule_key") String ruleKey,
        @Schema(description = "PERCENTAGE of each share, or a FLAT amount per share")
        @JsonProperty("mode") SaleFeeMode mode,
        @Schema(description = "The percentage, or the flat amount", example = "10")
        @JsonProperty("rate") BigDecimal rate,
        @JsonProperty("rate_currency") String rateCurrency,
        @JsonProperty("waived") Boolean waived,
        @JsonProperty("discount_percentage") BigDecimal discountPercentage,
        @JsonProperty("discount_amount") BigDecimal discountAmount,
        @JsonProperty("evaluated_at") OffsetDateTime evaluatedAt
) {
}
