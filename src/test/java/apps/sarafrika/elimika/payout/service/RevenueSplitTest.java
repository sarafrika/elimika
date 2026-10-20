package apps.sarafrika.elimika.payout.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.function.UnaryOperator;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("A class sale splits between the course creator and the delivery party, each paying their own platform fee")
class RevenueSplitTest {

    private static final UnaryOperator<BigDecimal> NO_FEE = share -> BigDecimal.ZERO;

    private static UnaryOperator<BigDecimal> percentFee(String percent) {
        return share -> share.multiply(new BigDecimal(percent)).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    @Test
    @DisplayName("worked example: KES 50,000 on a 30/70 course with a 10% fee pays 13,500 and 31,500 and keeps 5,000")
    void workedExample() {
        RevenueSplit split = RevenueSplit.of(money("50000"), new BigDecimal("30.00"), percentFee("10"));

        assertLeg(split.creator(), "15000.00", "1500.00", "13500.00");
        assertLeg(split.delivery(), "35000.00", "3500.00", "31500.00");
        assertThat(split.totalPlatformFee()).isEqualByComparingTo("5000.00");
    }

    @Test
    @DisplayName("creator net, delivery net and the fees always add back to what the learner paid")
    void legsAddBackToGross() {
        RevenueSplit split = RevenueSplit.of(money("135332"), new BigDecimal("30.00"), percentFee("7.5"));

        BigDecimal total = split.creator().net().add(split.delivery().net()).add(split.totalPlatformFee());
        assertThat(total).isEqualByComparingTo("135332");
    }

    @Test
    @DisplayName("a share that does not divide evenly gives the remainder cent to the delivery party")
    void remainderCentGoesToDelivery() {
        RevenueSplit split = RevenueSplit.of(money("100.01"), new BigDecimal("50.00"), NO_FEE);

        assertLeg(split.creator(), "50.00", "0.00", "50.00");
        assertLeg(split.delivery(), "50.01", "0.00", "50.01");
    }

    @Test
    @DisplayName("with no fee configured each earner receives their whole gross share")
    void noFee() {
        RevenueSplit split = RevenueSplit.of(money("10000"), new BigDecimal("30.00"), NO_FEE);

        assertLeg(split.creator(), "3000.00", "0.00", "3000.00");
        assertLeg(split.delivery(), "7000.00", "0.00", "7000.00");
        assertThat(split.totalPlatformFee()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("a flat fee is charged to each earner but never exceeds their share")
    void flatFeeIsCappedAtTheShare() {
        UnaryOperator<BigDecimal> flat200 = share -> new BigDecimal("200.00");

        RevenueSplit split = RevenueSplit.of(money("1000"), new BigDecimal("10.00"), flat200);

        assertLeg(split.creator(), "100.00", "100.00", "0.00");
        assertLeg(split.delivery(), "900.00", "200.00", "700.00");
    }

    @Test
    @DisplayName("a 0% creator share sends the whole sale to the delivery party")
    void zeroCreatorShare() {
        RevenueSplit split = RevenueSplit.of(money("5000"), BigDecimal.ZERO, percentFee("10"));

        assertLeg(split.creator(), "0.00", "0.00", "0.00");
        assertLeg(split.delivery(), "5000.00", "500.00", "4500.00");
    }

    @Test
    @DisplayName("a 100% creator share sends the whole sale to the creator")
    void fullCreatorShare() {
        RevenueSplit split = RevenueSplit.of(money("5000"), new BigDecimal("100.00"), percentFee("10"));

        assertLeg(split.creator(), "5000.00", "500.00", "4500.00");
        assertLeg(split.delivery(), "0.00", "0.00", "0.00");
    }

    @Test
    @DisplayName("a free line pays and charges nobody")
    void freeLine() {
        RevenueSplit split = RevenueSplit.of(BigDecimal.ZERO, new BigDecimal("30.00"), percentFee("10"));

        assertLeg(split.creator(), "0.00", "0.00", "0.00");
        assertLeg(split.delivery(), "0.00", "0.00", "0.00");
    }

    private static BigDecimal money(String value) {
        return new BigDecimal(value);
    }

    private static void assertLeg(RevenueSplit.Leg leg, String gross, String fee, String net) {
        assertThat(leg.gross()).as("gross").isEqualByComparingTo(gross);
        assertThat(leg.platformFee()).as("platform fee").isEqualByComparingTo(fee);
        assertThat(leg.net()).as("net").isEqualByComparingTo(net);
    }
}
