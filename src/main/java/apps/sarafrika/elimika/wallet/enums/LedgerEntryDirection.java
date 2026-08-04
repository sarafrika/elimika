package apps.sarafrika.elimika.wallet.enums;

import java.util.Locale;

/**
 * Which side of a transaction an entry sits on. Amounts are always unsigned; the direction carries
 * the sign.
 */
public enum LedgerEntryDirection {
    DEBIT,
    CREDIT;

    public LedgerEntryDirection opposite() {
        return this == DEBIT ? CREDIT : DEBIT;
    }

    public static LedgerEntryDirection fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LedgerEntryDirection.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
