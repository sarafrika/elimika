package apps.sarafrika.elimika.shared.spi;

import java.math.BigDecimal;

/**
 * How many classes a course has open to the public, and the cheapest fee among them.
 *
 * @param openClassCount active, public, not-full classes whose registration and teaching have not ended
 * @param priceFrom      the lowest sale price among those classes, null when none has one
 */
public record CourseOpenClassSummary(long openClassCount, BigDecimal priceFrom) {
}
