package apps.sarafrika.elimika.wallet.ledger;

import apps.sarafrika.elimika.wallet.enums.LedgerEntryDirection;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * One side of a posting. The amount is unsigned; {@code direction} carries the sign.
 */
public record LedgerPostingLeg(LedgerAccountRef account, LedgerEntryDirection direction, BigDecimal amount) {

    public LedgerPostingLeg {
        Objects.requireNonNull(account, "account is required");
        Objects.requireNonNull(direction, "direction is required");
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Ledger entry amount must be greater than zero");
        }
    }

    public static LedgerPostingLeg debit(LedgerAccountRef account, BigDecimal amount) {
        return new LedgerPostingLeg(account, LedgerEntryDirection.DEBIT, amount);
    }

    public static LedgerPostingLeg credit(LedgerAccountRef account, BigDecimal amount) {
        return new LedgerPostingLeg(account, LedgerEntryDirection.CREDIT, amount);
    }

    /** Debits are positive, credits negative. A transaction's legs must net to zero per currency. */
    public BigDecimal signedAmount() {
        return direction == LedgerEntryDirection.DEBIT ? amount : amount.negate();
    }
}
