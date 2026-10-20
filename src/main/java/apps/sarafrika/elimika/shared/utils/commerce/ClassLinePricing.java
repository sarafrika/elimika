package apps.sarafrika.elimika.shared.utils.commerce;

import apps.sarafrika.elimika.shared.spi.ClassScheduleService.ClassScheduleSummary;
import apps.sarafrika.elimika.shared.utils.enums.RateBasis;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** What a learner pays for a whole class: the unit price times the units of its rate basis in the timetable. */
public final class ClassLinePricing {

    private static final int SCALE = 4;

    private ClassLinePricing() {
    }

    public static BigDecimal price(BigDecimal unitAmount, RateBasis basis, ClassScheduleSummary timetable) {
        BigDecimal unit = unitAmount == null ? BigDecimal.ZERO : unitAmount.setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal units = timetable == null ? BigDecimal.ZERO : billableUnits(basis, timetable);
        return units.signum() <= 0 ? unit : unit.multiply(units).setScale(SCALE, RoundingMode.HALF_UP);
    }

    /** How many priced units the timetable holds; an unknown basis counts hours, the column's default. */
    public static BigDecimal billableUnits(RateBasis basis, ClassScheduleSummary timetable) {
        return switch (basis == null ? RateBasis.PER_HOUR : basis) {
            case PER_SESSION -> BigDecimal.valueOf(timetable.scheduledInstances());
            case PER_DAY -> BigDecimal.valueOf(timetable.scheduledDays());
            case PER_HOUR -> BigDecimal.valueOf(timetable.scheduledMinutes())
                    .divide(BigDecimal.valueOf(60), SCALE, RoundingMode.HALF_UP);
        };
    }
}
