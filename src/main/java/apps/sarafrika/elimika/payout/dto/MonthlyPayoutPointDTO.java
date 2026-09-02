package apps.sarafrika.elimika.payout.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * One month of money an organisation actually paid out to its instructors — the sum of
 * settled obligations in that calendar month, in a single currency.
 *
 * @param month        calendar month in {@code YYYY-MM} form
 * @param amount       total settled payout for the month
 * @param currencyCode ISO-4217 currency the amount is denominated in, e.g. KES
 */
@Schema(name = "MonthlyPayoutPoint",
        description = "A month's settled instructor payouts for an organisation")
public record MonthlyPayoutPointDTO(
        @Schema(description = "Calendar month in YYYY-MM form", example = "2026-07")
        @JsonProperty("month") String month,

        @Schema(description = "Total settled payout for the month", example = "125000.00")
        @JsonProperty("amount") BigDecimal amount,

        @Schema(description = "ISO-4217 currency the amount is denominated in", example = "KES")
        @JsonProperty("currency_code") String currencyCode
) {
}
