package apps.sarafrika.elimika.shared.utils.commerce;

import apps.sarafrika.elimika.shared.spi.ClassScheduleService.ClassScheduleSummary;
import apps.sarafrika.elimika.shared.utils.enums.RateBasis;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("A class is charged its unit price times the units of its rate basis in the timetable")
class ClassLinePricingTest {

    // 4 sessions of 90 minutes over 2 calendar days.
    private static final ClassScheduleSummary TIMETABLE =
            new ClassScheduleSummary(360, 4, 0, BigDecimal.ZERO, 2);

    @Test
    @DisplayName("per hour: 1,000 x 6 hours")
    void perHour() {
        assertThat(ClassLinePricing.price(new BigDecimal("1000"), RateBasis.PER_HOUR, TIMETABLE))
                .isEqualByComparingTo("6000");
    }

    @Test
    @DisplayName("per day: 1,000 x 2 calendar days, not x hours")
    void perDay() {
        assertThat(ClassLinePricing.price(new BigDecimal("1000"), RateBasis.PER_DAY, TIMETABLE))
                .isEqualByComparingTo("2000");
    }

    @Test
    @DisplayName("per session: 1,000 x 4 sessions")
    void perSession() {
        assertThat(ClassLinePricing.price(new BigDecimal("1000"), RateBasis.PER_SESSION, TIMETABLE))
                .isEqualByComparingTo("4000");
    }

    @Test
    @DisplayName("an unknown basis is priced per hour, the column's default")
    void unknownBasisIsHourly() {
        assertThat(ClassLinePricing.price(new BigDecimal("1000"), null, TIMETABLE)).isEqualByComparingTo("6000");
    }

    @Test
    @DisplayName("a class with nothing scheduled yet is charged the unit price alone")
    void emptyTimetable() {
        assertThat(ClassLinePricing.price(new BigDecimal("1000"), RateBasis.PER_DAY, new ClassScheduleSummary(0, 0)))
                .isEqualByComparingTo("1000");
        assertThat(ClassLinePricing.price(new BigDecimal("1000"), RateBasis.PER_HOUR, null))
                .isEqualByComparingTo("1000");
    }

    @Test
    @DisplayName("a missing unit price is free")
    void missingPrice() {
        assertThat(ClassLinePricing.price(null, RateBasis.PER_HOUR, TIMETABLE)).isEqualByComparingTo("0");
    }
}
