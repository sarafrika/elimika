package apps.sarafrika.elimika.wallet.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

/** One purse of a wallet as the wallet screens show it. */
@Schema(name = "WalletPurse", description = "One purse of a wallet, with its own balance. A person has COURSE_CREATOR, "
        + "INSTRUCTOR and STUDENT purses; an organisation has EARNINGS.")
public record WalletPurseSummary(
        @Schema(example = "COURSE_CREATOR")
        @JsonProperty("purse") WalletPurse purse,
        @Schema(description = "The purse's wallet row; null until money first lands in it")
        @JsonProperty("wallet_uuid") UUID walletUuid,
        @JsonProperty("currency_code") String currencyCode,
        @Schema(description = "The purse's balance, which confirmed withdrawals have already left; same as "
                + "available_amount", example = "1500.00")
        @JsonProperty("balance_amount") BigDecimal balanceAmount,
        @Schema(description = "Held for withdrawals from this purse still being paid out", example = "0.00")
        @JsonProperty("held_amount") BigDecimal heldAmount,
        @Schema(description = "What can be withdrawn or moved from this purse now", example = "1500.00")
        @JsonProperty("available_amount") BigDecimal availableAmount,
        @Schema(description = "available_amount plus held_amount", example = "1500.00")
        @JsonProperty("account_balance") BigDecimal accountBalance,
        @Schema(description = "Lifetime sale shares credited to this purse, net of reversals")
        @JsonProperty("shares_total") BigDecimal sharesTotal,
        @Schema(description = "Lifetime platform fees charged on those shares")
        @JsonProperty("platform_fees_total") BigDecimal platformFeesTotal,
        @Schema(description = "Sales credited to this purse, net of reversals")
        @JsonProperty("sales_count") Long salesCount,
        @Schema(description = "Lifetime transfers out of this purse, moves to the owner's other purses included")
        @JsonProperty("transferred_out_total") BigDecimal transferredOutTotal
) {

    public static WalletPurseSummary from(WalletPurseBalance balance) {
        WalletTotals totals = balance.totals();
        return new WalletPurseSummary(balance.purse(), balance.walletUuid(), balance.currencyCode(),
                balance.balanceAmount(), balance.heldAmount(), balance.availableAmount(), balance.accountBalance(),
                totals.sharesTotal(), totals.platformFeesTotal(), totals.salesCount(), totals.transferredOutTotal());
    }
}
