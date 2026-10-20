package apps.sarafrika.elimika.wallet.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

/** A wallet's balance in one currency; {@code wallet_uuid} is null while the user has no wallet yet. */
@Schema(name = "WalletBalanceSummary", description = "Read-only wallet balance for one currency")
public record WalletBalanceSummary(
        @JsonProperty("wallet_uuid")
        UUID walletUuid,
        @JsonProperty("currency_code")
        String currencyCode,
        @JsonProperty("balance_amount")
        BigDecimal balanceAmount,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("shares_total")
        BigDecimal sharesTotal,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("platform_fees_total")
        BigDecimal platformFeesTotal,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("sales_count")
        Long salesCount,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("transferred_out_total")
        BigDecimal transferredOutTotal
) {

    public WalletBalanceSummary(UUID walletUuid, String currencyCode, BigDecimal balanceAmount) {
        this(walletUuid, currencyCode, balanceAmount, null, null, null, null);
    }

    /** The same balance with the wallet's lifetime totals, for the screens that show them. */
    public WalletBalanceSummary withTotals(WalletTotals totals) {
        return new WalletBalanceSummary(walletUuid, currencyCode, balanceAmount, totals.sharesTotal(),
                totals.platformFeesTotal(), totals.salesCount(), totals.transferredOutTotal());
    }
}
