package apps.sarafrika.elimika.wallet.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * The purse a withdrawal is paid from, as an admin reviewing it sees it, read now. A confirmed withdrawal has
 * already left {@code availableBalance}, so the account is available plus held; both are the purse's own.
 */
public record WalletCheck(
        WalletPurse purse,
        BigDecimal accountBalance,
        BigDecimal heldAmount,
        long heldCount,
        BigDecimal heldDebitedAmount,
        BigDecimal availableBalance,
        BigDecimal balanceAfter,
        BigDecimal ledgerBalance,
        Boolean ledgerMatches,
        boolean covers,
        OffsetDateTime checkedAt
) {

    public static final String LEDGER_MISMATCH = "LEDGER_MISMATCH";
    public static final String WALLET_SHORT = "WALLET_SHORT";

    /** Derived review flags; never stored. */
    public List<String> flags() {
        List<String> flags = new ArrayList<>(2);
        if (Boolean.FALSE.equals(ledgerMatches)) {
            flags.add(LEDGER_MISMATCH);
        }
        if (!covers) {
            flags.add(WALLET_SHORT);
        }
        return List.copyOf(flags);
    }

    public boolean hasIssues() {
        return Boolean.FALSE.equals(ledgerMatches) || !covers;
    }
}
