package apps.sarafrika.elimika.wallet.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
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
        BigDecimal transferredOutTotal,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("held_amount")
        BigDecimal heldAmount,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("available_amount")
        BigDecimal availableAmount,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("can_withdraw")
        Boolean canWithdraw,
        @Schema(description = "The purse this balance is of; null when it sums every purse")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("purse")
        WalletPurse purse,
        @Schema(description = "Each purse with its own balance; on the wallet screens only")
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @JsonProperty("purses")
        List<WalletPurseSummary> purses
) {

    public WalletBalanceSummary(UUID walletUuid, String currencyCode, BigDecimal balanceAmount) {
        this(walletUuid, currencyCode, balanceAmount, null, null, null, null, null, null, null, null, null);
    }

    public WalletBalanceSummary(UUID walletUuid, String currencyCode, BigDecimal balanceAmount, WalletPurse purse) {
        this(walletUuid, currencyCode, balanceAmount, null, null, null, null, null, null, null, purse, null);
    }

    /** The same balance with each purse listed. */
    public WalletBalanceSummary withPurses(List<WalletPurseBalance> balances) {
        return new WalletBalanceSummary(walletUuid, currencyCode, balanceAmount, sharesTotal, platformFeesTotal,
                salesCount, transferredOutTotal, heldAmount, availableAmount, canWithdraw, purse,
                balances.stream().map(WalletPurseSummary::from).toList());
    }

    /** The same balance with the wallet's lifetime totals, for the screens that show them. */
    public WalletBalanceSummary withTotals(WalletTotals totals) {
        return new WalletBalanceSummary(walletUuid, currencyCode, balanceAmount, totals.sharesTotal(),
                totals.platformFeesTotal(), totals.salesCount(), totals.transferredOutTotal(),
                heldAmount, availableAmount, canWithdraw, purse, purses);
    }

    /** The same balance with what is held, what is available and whether the caller may withdraw. */
    public WalletBalanceSummary withStanding(WalletStanding standing) {
        return new WalletBalanceSummary(walletUuid, currencyCode, balanceAmount, sharesTotal, platformFeesTotal,
                salesCount, transferredOutTotal, standing.heldAmount(), standing.availableAmount(),
                standing.canWithdraw(), purse, purses);
    }
}
