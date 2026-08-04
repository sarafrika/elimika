package apps.sarafrika.elimika.wallet.enums;

import java.util.Locale;

/**
 * Who a ledger account belongs to. {@link #PLATFORM} accounts are the platform's own internal
 * accounts and carry no owner UUID.
 */
public enum LedgerOwnerType {
    USER,
    ORGANISATION,
    PLATFORM;

    public static LedgerOwnerType fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LedgerOwnerType.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
