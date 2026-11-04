package apps.sarafrika.elimika.shared.spi.revenue;

import java.util.Locale;

/** How the platform fee on a sale share was priced: a percentage of the share or a flat amount per share. */
public enum SaleFeeMode {
    PERCENTAGE, FLAT;

    public static SaleFeeMode fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return SaleFeeMode.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
