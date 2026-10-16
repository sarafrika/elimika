package apps.sarafrika.elimika.course.internal;

import apps.sarafrika.elimika.course.model.CourseGradeBand;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** The workbook's colour bands (28–30, 24–27, 18–23, 12–17, 0–11 out of 30) entered as lower-bound percentages. */
class GradeBandsTest {

    private static final List<CourseGradeBand> WORKBOOK = List.of(
            band("Distinction", "93.33"), band("Merit", "80.00"), band("Pass", "60.00"),
            band("Fail", "40.00"), band("No Effort", "0.00"));

    @Test
    void aDecimalBetweenTheWorkbookBandsStillGetsALabel() {
        // 27.5/30 fell between the workbook's whole-number bands; as a lower bound it is Merit.
        assertThat(GradeBands.labelFor(WORKBOOK, new BigDecimal("91.67"))).isEqualTo("Merit");
    }

    @Test
    void reachingTheMinimumEarnsTheBand() {
        assertThat(GradeBands.labelFor(WORKBOOK, new BigDecimal("93.33"))).isEqualTo("Distinction");
        assertThat(GradeBands.labelFor(WORKBOOK, new BigDecimal("80.00"))).isEqualTo("Merit");
    }

    @Test
    void theLowestBandCatchesEverythingBelowTheNext() {
        assertThat(GradeBands.labelFor(WORKBOOK, new BigDecimal("39.67"))).isEqualTo("No Effort");
    }

    @Test
    void noBandsOrNoGradeMeansNoLabel() {
        assertThat(GradeBands.labelFor(List.of(), new BigDecimal("91.67"))).isNull();
        assertThat(GradeBands.labelFor(WORKBOOK, null)).isNull();
    }

    private static CourseGradeBand band(String name, String min) {
        CourseGradeBand band = new CourseGradeBand();
        band.setName(name);
        band.setMinPercentage(new BigDecimal(min));
        return band;
    }
}
