package apps.sarafrika.elimika.wallet.enums;

import java.util.Locale;

public enum LedgerAccountStatus {
    ACTIVE,
    FROZEN,
    CLOSED;

    public static LedgerAccountStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LedgerAccountStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
