package apps.sarafrika.elimika.course.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * How easy a class is to get into, without publishing seat numbers.
 * <p>
 * Seats filled beside seats offered, next to a published fee, turn a course page into a revenue
 * calculator, so the page gets a band instead of the counts behind it.
 */
@Schema(name = "OpenClassAvailability", description = "OPEN: seats available (or capacity unknown). "
        + "FEW_LEFT: at most max(5, 20% of capacity) seats left. FULL: no seats left.")
public enum OpenClassAvailability {
    OPEN,
    FEW_LEFT,
    FULL;

    /** The smallest seat count that is always "few", whatever the class size. */
    private static final int FEW_SEATS_FLOOR = 5;

    /**
     * The band for a class.
     *
     * @param capacity  seats offered, null when unknown
     * @param seatsLeft seats still free, null when unknown
     * @return FULL with none left, FEW_LEFT at or under max(5, 20% of capacity), OPEN otherwise or when unknown
     */
    public static OpenClassAvailability of(Integer capacity, Integer seatsLeft) {
        if (capacity == null || seatsLeft == null) {
            return OPEN;
        }
        if (seatsLeft <= 0) {
            return FULL;
        }
        double fewThreshold = Math.max(FEW_SEATS_FLOOR, capacity * 0.2);
        return seatsLeft <= fewThreshold ? FEW_LEFT : OPEN;
    }
}
