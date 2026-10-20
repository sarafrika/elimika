package apps.sarafrika.elimika.wallet.service;

import java.util.Locale;

/** Who a wallet belongs to. */
public enum WalletOwnerType {
    USER,
    ORGANISATION;

    public static WalletOwnerType fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return WalletOwnerType.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
