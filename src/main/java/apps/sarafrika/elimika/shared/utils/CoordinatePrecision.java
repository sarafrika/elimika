package apps.sarafrika.elimika.shared.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Coarsens coordinates before they leave the platform in a public or directory response.
 * <p>
 * A profile's stored latitude and longitude usually pin a person's home or workplace to within a
 * few metres. Directory listings only need to say roughly where somebody is, so they publish the
 * coordinate at two decimal places — about 1.1 km at the equator, town level. Only the profile
 * owner and platform admins reading a single profile see the stored precision; stored data is
 * never changed.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CoordinatePrecision {

    /** Decimal places published in directory responses (about 1 km). */
    public static final int PUBLIC_SCALE = 2;

    /**
     * Rounds a coordinate to {@link #PUBLIC_SCALE} decimal places, half-up; null stays null.
     */
    public static BigDecimal toPublic(BigDecimal coordinate) {
        return coordinate == null ? null : coordinate.setScale(PUBLIC_SCALE, RoundingMode.HALF_UP);
    }
}
