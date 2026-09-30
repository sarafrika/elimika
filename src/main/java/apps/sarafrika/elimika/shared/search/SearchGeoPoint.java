package apps.sarafrika.elimika.shared.search;

import apps.sarafrika.elimika.shared.utils.CoordinatePrecision;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

/**
 * A WGS84 point, shaped the way the engine expects a document's {@code _geo} attribute:
 * {@code {"lat": ..., "lng": ...}}. A document exposes it as
 * {@code @JsonProperty("_geo") SearchGeoPoint geo} and leaves it {@code null} when the row has no
 * location, which keeps the document out of every geo filter.
 * <p>
 * Round personal locations (an instructor's home area) before they reach a document; the engine
 * stores what it is given.
 */
public record SearchGeoPoint(
        @JsonProperty("lat") double lat,
        @JsonProperty("lng") double lng
) {

    public SearchGeoPoint {
        requireValid(lat, lng);
    }

    /**
     * The point at public precision ({@link CoordinatePrecision#toPublic}, about 1 km), or
     * {@code null} when either coordinate is missing or out of range. Every {@code _geo} a document
     * carries goes through here, so no index ever holds a stored coordinate at full precision.
     */
    public static SearchGeoPoint rounded(BigDecimal lat, BigDecimal lng) {
        if (lat == null || lng == null) {
            return null;
        }
        double roundedLat = CoordinatePrecision.toPublic(lat).doubleValue();
        double roundedLng = CoordinatePrecision.toPublic(lng).doubleValue();
        if (!Double.isFinite(roundedLat) || roundedLat < -90 || roundedLat > 90
                || !Double.isFinite(roundedLng) || roundedLng < -180 || roundedLng > 180) {
            return null;
        }
        return new SearchGeoPoint(roundedLat, roundedLng);
    }

    static void requireValid(double lat, double lng) {
        if (!Double.isFinite(lat) || lat < -90 || lat > 90) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90");
        }
        if (!Double.isFinite(lng) || lng < -180 || lng > 180) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180");
        }
    }
}
