package apps.sarafrika.elimika.classes.util;

import apps.sarafrika.elimika.shared.enums.LocationType;
import apps.sarafrika.elimika.shared.enums.SessionFormat;
import apps.sarafrika.elimika.shared.utils.enums.RateBasis;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * Plain-language names for the parts of a rate card cell, so a refusal names the exact cell at stake.
 */
public final class RateWording {

    private RateWording() {
    }

    public static String basis(RateBasis basis) {
        return switch (basis) {
            case PER_HOUR -> "per hour";
            case PER_SESSION -> "per session";
            case PER_DAY -> "per day";
        };
    }

    public static String format(SessionFormat format) {
        return format == SessionFormat.INDIVIDUAL ? "private" : "group";
    }

    /** Hybrid delivery is priced from the in-person cell, the same way the rate card resolves it. */
    public static String location(LocationType locationType) {
        return locationType == LocationType.IN_PERSON || locationType == LocationType.HYBRID
                ? "in-person"
                : "online";
    }

    public static String money(BigDecimal amount) {
        return String.format(Locale.ROOT, "KES %,.2f", amount == null ? BigDecimal.ZERO : amount);
    }
}
