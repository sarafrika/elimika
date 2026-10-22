package apps.sarafrika.elimika.wallet.enums;

import java.util.Locale;

/** The rail a payout goes over; M-Pesa only for now. */
public enum PayoutMethodType {
    MPESA;

    public static PayoutMethodType fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return PayoutMethodType.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
