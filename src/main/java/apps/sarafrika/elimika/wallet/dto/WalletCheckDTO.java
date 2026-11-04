package apps.sarafrika.elimika.wallet.dto;

import apps.sarafrika.elimika.wallet.service.WalletCheck;
import apps.sarafrika.elimika.wallet.service.WalletPurse;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Schema(name = "WalletCheck", description = "The earner's wallet read when the admin view was loaded, so a reviewer "
        + "sees what the withdrawal is paid from. Only on admin views.")
public record WalletCheckDTO(
        @Schema(description = "The purse checked: the one the withdrawal is paid from. Every figure below is that "
                + "purse's own", example = "INSTRUCTOR")
        @JsonProperty("purse") WalletPurse purse,
        @Schema(description = "Account balance now: the available balance plus everything held", example = "189465.50")
        @JsonProperty("account_balance") BigDecimal accountBalance,
        @Schema(description = "Gross amount (incl. any earner M-Pesa charge) of all this owner's held withdrawals from this purse "
                + "(AWAITING_SECOND_APPROVAL, NEEDS_REVIEW, SENDING, PROCESSING), this one included when held",
                example = "100.00")
        @JsonProperty("held_amount") BigDecimal heldAmount,
        @Schema(description = "How many withdrawals make up held_amount", example = "1")
        @JsonProperty("held_count") long heldCount,
        @Schema(description = "Withdrawal and charge debits the wallet actually recorded for those held withdrawals, "
                + "less reversals; below held_amount means something is held that never left the wallet",
                example = "100.00")
        @JsonProperty("held_debited_amount") BigDecimal heldDebitedAmount,
        @Schema(description = "What the earner could still withdraw now; the wallet balance, which held withdrawals "
                + "have already left. Same figure as the earner's own Available.", example = "189365.50")
        @JsonProperty("available_balance") BigDecimal availableBalance,
        @Schema(description = "account_balance less this withdrawal's gross amount: what remains once it pays. Null "
                + "when the withdrawal is finished (paid, failed, rejected, cancelled, expired)", example = "189365.50")
        @JsonProperty("balance_after") BigDecimal balanceAfter,
        @Schema(description = "Posted balance of the purse's ledger account, which mirrors "
                + "available_balance; null when the wallet has no ledger account", example = "189365.50")
        @JsonProperty("ledger_balance") BigDecimal ledgerBalance,
        @Schema(description = "ledger_balance equals available_balance; null when there is no ledger account. "
                + "False raises LEDGER_MISMATCH")
        @JsonProperty("ledger_matches") Boolean ledgerMatches,
        @Schema(description = "The wallet backs everything held: held_debited_amount is at least held_amount. "
                + "False raises WALLET_SHORT")
        @JsonProperty("covers") boolean covers,
        @JsonProperty("checked_at") OffsetDateTime checkedAt
) {

    public static WalletCheckDTO from(WalletCheck c) {
        return c == null ? null : new WalletCheckDTO(c.purse(), c.accountBalance(), c.heldAmount(), c.heldCount(),
                c.heldDebitedAmount(), c.availableBalance(), c.balanceAfter(), c.ledgerBalance(), c.ledgerMatches(),
                c.covers(), c.checkedAt());
    }
}
