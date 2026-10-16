package apps.sarafrika.elimika.course.internal;

import apps.sarafrika.elimika.course.model.CourseGradeBand;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;

/** Picks the band a final grade earns: the highest whose minimum it reaches, so decimals never fall between bands. */
public final class GradeBands {

    private GradeBands() {
    }

    public static String labelFor(Collection<CourseGradeBand> bands, BigDecimal finalGrade) {
        if (finalGrade == null || bands == null) {
            return null;
        }
        return bands.stream()
                .filter(band -> band.getMinPercentage() != null && finalGrade.compareTo(band.getMinPercentage()) >= 0)
                .max(Comparator.comparing(CourseGradeBand::getMinPercentage))
                .map(CourseGradeBand::getName)
                .filter(Objects::nonNull)
                .orElse(null);
    }
}
