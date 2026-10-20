package apps.sarafrika.elimika.systemconfig.dto;

import apps.sarafrika.elimika.systemconfig.dto.PlatformFeeConfig.TimeBoundModifier;
import apps.sarafrika.elimika.systemconfig.enums.PlatformFeeMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("The platform fee rule prices the fee on one earner's share")
class PlatformFeeConfigTest {

    private static final OffsetDateTime NOW = OffsetDateTime.of(2026, 10, 20, 12, 0, 0, 0, ZoneOffset.UTC);
    private static final TimeBoundModifier OPEN_ENDED_HALF_OFF =
            new TimeBoundModifier(null, new BigDecimal("50"), NOW.minusDays(1), null);

    @Test
    @DisplayName("a percentage fee is that percentage of the share, to the cent")
    void percentage() {
        PlatformFeeConfig config = new PlatformFeeConfig(PlatformFeeMode.PERCENTAGE, new BigDecimal("10"), "KES", null, null);

        assertThat(config.feeOn(new BigDecimal("15000"), NOW)).isEqualByComparingTo("1500.00");
        assertThat(config.feeOn(new BigDecimal("33.33"), NOW)).isEqualByComparingTo("3.33");
    }

    @Test
    @DisplayName("a flat fee is the same amount whatever the share")
    void flat() {
        PlatformFeeConfig config = new PlatformFeeConfig(PlatformFeeMode.FLAT, new BigDecimal("200"), "KES", null, null);

        assertThat(config.feeOn(new BigDecimal("15000"), NOW)).isEqualByComparingTo("200.00");
    }

    @Test
    @DisplayName("an active waiver charges nothing")
    void waiver() {
        TimeBoundModifier waiver = new TimeBoundModifier(null, new BigDecimal("100"), NOW.minusDays(1), NOW.plusDays(1));
        PlatformFeeConfig config = new PlatformFeeConfig(PlatformFeeMode.PERCENTAGE, new BigDecimal("10"), "KES", waiver, null);

        assertThat(config.feeOn(new BigDecimal("15000"), NOW)).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("an active percentage discount reduces the fee, and a fixed discount never takes it below zero")
    void discounts() {
        PlatformFeeConfig halfOff = new PlatformFeeConfig(PlatformFeeMode.PERCENTAGE, new BigDecimal("10"), "KES", null, OPEN_ENDED_HALF_OFF);
        TimeBoundModifier minus500 = new TimeBoundModifier(new BigDecimal("500"), null, NOW.minusDays(1), null);
        PlatformFeeConfig flatOff = new PlatformFeeConfig(PlatformFeeMode.FLAT, new BigDecimal("200"), "KES", null, minus500);

        assertThat(halfOff.feeOn(new BigDecimal("15000"), NOW)).isEqualByComparingTo("750.00");
        assertThat(flatOff.feeOn(new BigDecimal("15000"), NOW)).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("an expired discount no longer applies")
    void expiredDiscount() {
        TimeBoundModifier expired = new TimeBoundModifier(null, new BigDecimal("50"), NOW.minusDays(10), NOW.minusDays(1));
        PlatformFeeConfig config = new PlatformFeeConfig(PlatformFeeMode.PERCENTAGE, new BigDecimal("10"), "KES", null, expired);

        assertThat(config.feeOn(new BigDecimal("15000"), NOW)).isEqualByComparingTo("1500.00");
    }

    @Test
    @DisplayName("a rule with no amount charges nothing")
    void missingAmount() {
        PlatformFeeConfig config = new PlatformFeeConfig(PlatformFeeMode.PERCENTAGE, null, "KES", null, null);

        assertThat(config.feeOn(new BigDecimal("15000"), NOW)).isEqualByComparingTo("0");
    }
}
