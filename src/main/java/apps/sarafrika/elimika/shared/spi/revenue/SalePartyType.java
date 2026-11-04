package apps.sarafrika.elimika.shared.spi.revenue;

import java.util.Locale;

/** Whose wallet a sale leg was paid into: a user's or an organisation's. */
public enum SalePartyType {
    USER, ORGANISATION;

    public static SalePartyType fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return SalePartyType.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
