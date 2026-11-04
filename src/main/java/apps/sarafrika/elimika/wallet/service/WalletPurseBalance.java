package apps.sarafrika.elimika.wallet.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * One purse of an owner, read now. {@code walletUuid} is null while the owner has no row for it yet.
 * {@code balanceAmount} is the purse's wallet row, which confirmed withdrawals have already left, so it is also
 * what is available; the account balance adds back what is held for withdrawals still being paid.
 */
public record WalletPurseBalance(
        WalletPurse purse,
        UUID walletUuid,
        String currencyCode,
        BigDecimal balanceAmount,
        BigDecimal heldAmount,
        WalletTotals totals,
        LocalDateTime createdDate,
        LocalDateTime updatedDate
) {

    public BigDecimal availableAmount() {
        return balanceAmount;
    }

    public BigDecimal accountBalance() {
        return balanceAmount.add(heldAmount);
    }
}
