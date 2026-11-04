package apps.sarafrika.elimika.shared.spi.revenue;

import java.util.Locale;

/** Who delivers a sold class: its instructor, or the organisation that hired them through a job. */
public enum SaleDeliveryKind {
    INSTRUCTOR, ORGANISATION;

    public static SaleDeliveryKind fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return SaleDeliveryKind.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
}
