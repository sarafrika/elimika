package apps.sarafrika.elimika.wallet.enums;

import java.util.Locale;

/** Who bears the M-Pesa transfer charge on a withdrawal. */
public enum PayoutFeePayer {
    PLATFORM,
    EARNER;

    public static PayoutFeePayer fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return PayoutFeePayer.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
