package apps.sarafrika.elimika.shared.search;

import apps.sarafrika.elimika.shared.utils.CoordinatePrecision;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * A near-me request ({@code near=lat,lng&radius_km=}) after the server has coarsened it.
 * <p>
 * The caller's point is rounded to {@link CoordinatePrecision#PUBLIC_SCALE} decimal places (about
 * 1 km) the moment it is parsed, before it reaches a filter, a sort or anything that might log it; it
 * is never stored. The radius is clamped to {@value #MIN_RADIUS_KM}-{@value #MAX_RADIUS_KM} km and
 * defaults to {@value #DEFAULT_RADIUS_KM}. Error messages never echo the submitted value.
 * <p>
 * Results never carry metres or coordinates back: only a {@link #distanceBand(Integer) distance band}.
 *
 * @param lat      rounded latitude
 * @param lng      rounded longitude
 * @param radiusKm clamped radius in kilometres
 */
public record NearMe(double lat, double lng, int radiusKm) {

    public static final String NEAR_PARAM = "near";
    public static final String RADIUS_PARAM = "radius_km";
    public static final int MIN_RADIUS_KM = 2;
    public static final int MAX_RADIUS_KM = 100;
    public static final int DEFAULT_RADIUS_KM = 10;

    public NearMe {
        SearchGeoPoint.requireValid(lat, lng);
        radiusKm = Math.clamp(radiusKm, MIN_RADIUS_KM, MAX_RADIUS_KM);
    }

    /**
     * Parses {@code near} ({@code "lat,lng"}) and {@code radius_km}; empty when {@code near} is absent.
     *
     * @throws IllegalArgumentException for a malformed or out-of-range point or radius (a 400)
     */
    public static Optional<NearMe> parse(String near, String radiusKm) {
        if (near == null || near.isBlank()) {
            if (radiusKm != null && !radiusKm.isBlank()) {
                throw new IllegalArgumentException("radius_km needs near=lat,lng");
            }
            return Optional.empty();
        }
        String[] parts = near.split(",");
        if (parts.length != 2) {
            throw new IllegalArgumentException("near must be lat,lng in decimal degrees");
        }
        double lat;
        double lng;
        try {
            lat = round(parts[0]);
            lng = round(parts[1]);
            SearchGeoPoint.requireValid(lat, lng);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("near must be lat,lng in decimal degrees, latitude -90..90 and longitude -180..180");
        }
        return Optional.of(new NearMe(lat, lng, parseRadius(radiusKm)));
    }

    /** Reads {@code near} and {@code radius_km} out of a request's parameter map. */
    public static Optional<NearMe> from(Map<String, String> params) {
        if (params == null) {
            return Optional.empty();
        }
        return parse(params.get(NEAR_PARAM), params.get(RADIUS_PARAM));
    }

    /** Documents whose {@code _geo} lies within the radius. */
    public SearchFilter filter() {
        return SearchFilter.geoRadius(lat, lng, radiusKm * 1000);
    }

    /** Nearest first. */
    public SearchSort sort() {
        return SearchSort.geoPoint(lat, lng);
    }

    /**
     * The ordering for a near-me read: without text, distance first and then the caller's own sorts;
     * with text, relevance and the caller's sorts first and distance as the last tie-breaker (which
     * also makes the engine report each hit's distance for its band).
     */
    public List<SearchSort> sorts(boolean hasText, List<SearchSort> requested) {
        List<SearchSort> sorts = new ArrayList<>();
        if (!hasText) {
            sorts.add(sort());
        }
        if (requested != null) {
            requested.stream().filter(s -> !s.isGeo()).forEach(sorts::add);
        }
        if (hasText) {
            sorts.add(sort());
        }
        return sorts;
    }

    /**
     * The coarse band a distance falls in: {@code "<2 km"}, {@code "2-5 km"}, {@code "5-10 km"},
     * {@code "10-25 km"} or {@code ">25 km"}; {@code null} without a distance.
     */
    public static String distanceBand(Integer meters) {
        if (meters == null || meters < 0) {
            return null;
        }
        if (meters < 2_000) {
            return "<2 km";
        }
        if (meters < 5_000) {
            return "2-5 km";
        }
        if (meters < 10_000) {
            return "5-10 km";
        }
        if (meters < 25_000) {
            return "10-25 km";
        }
        return ">25 km";
    }

    /**
     * The band of a document's indexed point (already rounded, see {@link SearchGeoPoint#rounded}) from
     * this (rounded) origin, or {@code null} without a point.
     * <p>
     * Computed here rather than read from the engine: the engine reports a hit's distance only when
     * {@code _geo} is a displayed attribute, and it never is, so coordinates cannot leave the index.
     * Both ends are the same rounded points the engine filtered on, so the answer is the same.
     */
    public String distanceBand(SearchGeoPoint point) {
        return point == null ? null : distanceBand((int) Math.round(meters(lat, lng, point.lat(), point.lng())));
    }

    /** Great-circle distance in metres on a spherical Earth, the model the engine uses. */
    static double meters(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 6_371_000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * earthRadius * Math.asin(Math.min(1, Math.sqrt(a)));
    }

    /**
     * Each hit's distance band by document UUID: from the engine's reported distance when it gives
     * one, else from {@code points} (the documents' rounded points, looked up by the owning module).
     */
    public Map<UUID, String> distanceBands(SearchPage page, Map<UUID, SearchGeoPoint> points) {
        Map<UUID, String> bands = new java.util.HashMap<>();
        if (page == null) {
            return bands;
        }
        for (SearchHit hit : page.hits()) {
            if (hit.uuid() == null) {
                continue;
            }
            String band = hit.geoDistanceMeters() != null
                    ? distanceBand(hit.geoDistanceMeters())
                    : distanceBand(points == null ? null : points.get(hit.uuid()));
            if (band != null) {
                bands.put(hit.uuid(), band);
            }
        }
        return bands;
    }

    private static double round(String value) {
        BigDecimal parsed;
        try {
            parsed = new BigDecimal(value.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("not a number", ex);
        }
        return CoordinatePrecision.toPublic(parsed).doubleValue();
    }

    private static int parseRadius(String radiusKm) {
        if (radiusKm == null || radiusKm.isBlank()) {
            return DEFAULT_RADIUS_KM;
        }
        double value;
        try {
            value = Double.parseDouble(radiusKm.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("radius_km must be a number of kilometres");
        }
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("radius_km must be a number of kilometres");
        }
        return (int) Math.clamp(Math.round(value), MIN_RADIUS_KM, MAX_RADIUS_KM);
    }
}
