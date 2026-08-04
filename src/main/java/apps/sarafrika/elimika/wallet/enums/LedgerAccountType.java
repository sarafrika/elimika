package apps.sarafrika.elimika.wallet.enums;

import java.util.Locale;

/**
 * The accounting classification of an account, which fixes which direction increases it.
 * <p>
 * Assets and expenses are debit-normal; liabilities and revenue are credit-normal. A user's wallet
 * balance is a {@link #LIABILITY}: the platform holds the money and owes it to them.
 */
public enum LedgerAccountType {
    ASSET,
    LIABILITY,
    REVENUE,
    EXPENSE;

    /**
     * @return {@code true} when a DEBIT increases this account.
     */
    public boolean isDebitNormal() {
        return this == ASSET || this == EXPENSE;
    }

    public static LedgerAccountType fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LedgerAccountType.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
