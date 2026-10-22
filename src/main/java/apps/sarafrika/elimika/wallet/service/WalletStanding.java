package apps.sarafrika.elimika.wallet.service;

import java.math.BigDecimal;

/** What a wallet holds for payouts in flight, what is free to use, and whether the caller may withdraw. */
public record WalletStanding(BigDecimal heldAmount, BigDecimal availableAmount, boolean canWithdraw) {
}
