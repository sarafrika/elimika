package apps.sarafrika.elimika.shared.dto.commerce;

import apps.sarafrika.elimika.systemconfig.enums.PlatformFeeMode;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "PlatformFeeBreakdown", description = "Computed platform fee details applied to an order")
public record PlatformFeeBreakdown(
        @Schema(description = "Fee amount in major currency units after adjustments", example = "15.50")
        BigDecimal amount,

        @Schema(description = "Currency code the fee is denominated in", example = "USD")
        String currency,

        @Schema(description = "How the platform fee was configured", example = "PERCENTAGE")
        PlatformFeeMode mode,

        @Schema(description = "Percentage rate applied when mode is PERCENTAGE", example = "2.50")
        BigDecimal rate,

        @Schema(description = "Order total used to compute the platform fee", example = "620.00")
        BigDecimal baseAmount,

        @Schema(description = "Identifier of the rule that produced the fee")
        UUID ruleUuid,

        @Schema(description = "Timestamp when the fee was evaluated", format = "date-time")
        OffsetDateTime evaluatedAt
) {
}
