package apps.sarafrika.elimika.wallet.enums;

import java.util.Locale;

/** A payout number's life: added and waiting for its code, verified, or removed. */
public enum PayoutMethodStatus {
    PENDING_VERIFICATION,
    VERIFIED,
    REMOVED;

    public static PayoutMethodStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return PayoutMethodStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
