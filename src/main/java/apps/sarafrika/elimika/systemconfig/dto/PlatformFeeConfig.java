package apps.sarafrika.elimika.systemconfig.dto;

import apps.sarafrika.elimika.systemconfig.enums.PlatformFeeMode;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Strongly typed representation of platform fee policies loaded from {@code system_rules}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PlatformFeeConfig(
        PlatformFeeMode mode,
        BigDecimal amount,
        String currency,
        @JsonProperty("waiver") TimeBoundModifier waiver,
        @JsonProperty("discount") TimeBoundModifier discount
) {
    public boolean waiverActive(OffsetDateTime evaluationInstant) {
        return waiver != null && waiver.isActive(evaluationInstant);
    }

    public boolean discountActive(OffsetDateTime evaluationInstant) {
        return discount != null && discount.isActive(evaluationInstant);
    }

    public record TimeBoundModifier(
            BigDecimal amount,
            BigDecimal percentage,
            OffsetDateTime start,
            OffsetDateTime end
    ) {
        public boolean isActive(OffsetDateTime instant) {
            if (instant == null) {
                return false;
            }
            boolean starts = start == null || !instant.isBefore(start);
            boolean ends = end == null || instant.isBefore(end) || instant.isEqual(end);
            return starts && ends && (isNonZero(amount) || isNonZero(percentage));
        }

        private boolean isNonZero(BigDecimal value) {
            return value != null && BigDecimal.ZERO.compareTo(value) != 0;
        }
    }
}
