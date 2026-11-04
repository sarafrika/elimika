package apps.sarafrika.elimika.shared.spi.revenue;

import java.util.Locale;

/**
 * What happened to one leg of a sale line: PAID, or why not. NO_EARNER: nobody was resolved to pay;
 * NO_SHARE: the split gave the leg nothing; NOT_SPLIT: the line had no course, programme or revenue split.
 */
public enum SaleLegStatus {
    PAID, NO_EARNER, NO_SHARE, NOT_SPLIT;

    public static SaleLegStatus fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return SaleLegStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
