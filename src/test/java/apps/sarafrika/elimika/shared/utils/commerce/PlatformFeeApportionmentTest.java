package apps.sarafrika.elimika.shared.utils.commerce;

import static org.assertj.core.api.Assertions.assertThat;

import apps.sarafrika.elimika.shared.dto.commerce.CartItemResponse;
import apps.sarafrika.elimika.shared.dto.commerce.OrderResponse;
import apps.sarafrika.elimika.shared.dto.commerce.PlatformFeeBreakdown;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * The platform fee is charged once on the whole order but credited against per-line revenue shares,
 * so it has to be split first. This is the point at which money is most easily invented or lost, so
 * the property being guarded is blunt: <strong>the split sums exactly to the fee, always</strong>.
 */
@DisplayName("Platform fee apportionment")
class PlatformFeeApportionmentTest {

    private static List<BigDecimal> totals(String... amounts) {
        List<BigDecimal> values = new ArrayList<>(amounts.length);
        for (String amount : amounts) {
            values.add(amount == null ? null : new BigDecimal(amount));
        }
        return values;
    }

    private static BigDecimal sum(List<BigDecimal> amounts) {
        return amounts.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Nested
    @DisplayName("exactness")
    class Exactness {

        @Test
        @DisplayName("a fee of 10.00 over 33.33/33.33/33.34 sums to exactly 10.00")
        void theAwkwardRoundingCaseSumsExactly() {
            List<BigDecimal> lines = totals("33.33", "33.33", "33.34");

            List<BigDecimal> apportioned = PlatformFeeApportionment.apportion(new BigDecimal("10.00"), lines);

            // Exact shares are 3.333 / 3.333 / 3.334. Rounding each independently (HALF_UP, 2dp)
            // gives 3.33 / 3.33 / 3.33 and quietly loses a cent, so the largest discarded fraction
            // takes the leftover instead.
            assertThat(apportioned).containsExactly(
                    new BigDecimal("3.33"), new BigDecimal("3.33"), new BigDecimal("3.34"));
            assertThat(sum(apportioned)).isEqualByComparingTo(new BigDecimal("10.00"));
        }

        @Test
        @DisplayName("rounding each line independently would have leaked a cent")
        void independentRoundingWouldNotHaveSummed() {
            List<BigDecimal> lines = totals("33.33", "33.33", "33.34");
            BigDecimal fee = new BigDecimal("10.00");
            BigDecimal orderTotal = sum(lines);

            BigDecimal naiveSum = lines.stream()
                    .map(line -> fee.multiply(line).divide(orderTotal, 2, RoundingMode.HALF_UP))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // This is the bug the largest-remainder method exists to prevent, pinned so nobody
            // "simplifies" the implementation back into it.
            assertThat(naiveSum).isEqualByComparingTo(new BigDecimal("9.99"));
            assertThat(sum(PlatformFeeApportionment.apportion(fee, lines)))
                    .isEqualByComparingTo(fee);
        }

        @Test
        @DisplayName("every awkward shape still sums to exactly the fee")
        void arbitraryShapesSumExactly() {
            List<List<BigDecimal>> shapes = List.of(
                    totals("0.01", "0.01", "0.01"),
                    totals("1.00", "2.00", "3.00", "4.00", "5.00", "6.00", "7.00"),
                    totals("999.99", "0.01"),
                    totals("1.11", "2.22", "3.33", "4.44", "5.55", "6.66", "7.77", "8.88", "9.99"),
                    totals("1000.0000", "0.3333", "0.3333", "0.3334"));
            List<BigDecimal> fees = List.of(
                    new BigDecimal("0.01"), new BigDecimal("0.03"), new BigDecimal("7.77"),
                    new BigDecimal("10.00"), new BigDecimal("123.45"));

            for (List<BigDecimal> shape : shapes) {
                for (BigDecimal fee : fees) {
                    List<BigDecimal> apportioned = PlatformFeeApportionment.apportion(fee, shape);
                    BigDecimal pool = sum(shape);
                    BigDecimal expected = fee.min(pool.setScale(2, RoundingMode.DOWN));

                    assertThat(sum(apportioned))
                            .as("fee %s over %s", fee, shape)
                            .isEqualByComparingTo(expected);
                    assertThat(apportioned).allSatisfy(amount -> assertThat(amount.signum()).isNotNegative());
                    for (int i = 0; i < shape.size(); i++) {
                        assertThat(apportioned.get(i))
                                .as("line %s of %s may not be charged more fee than it collected", i, shape)
                                .isLessThanOrEqualTo(shape.get(i));
                    }
                }
            }
        }
    }

    @Nested
    @DisplayName("degenerate inputs")
    class DegenerateInputs {

        @Test
        @DisplayName("a single line carries the whole fee")
        void singleLineTakesTheWholeFee() {
            assertThat(PlatformFeeApportionment.apportion(new BigDecimal("15.50"), totals("1000.00")))
                    .containsExactly(new BigDecimal("15.50"));
        }

        @Test
        @DisplayName("an order whose lines total zero is apportioned nothing, not a division by zero")
        void zeroOrderTotalApportionsNothing() {
            List<BigDecimal> apportioned =
                    PlatformFeeApportionment.apportion(new BigDecimal("10.00"), totals("0.00", "0.00"));

            assertThat(apportioned).allSatisfy(amount -> assertThat(amount).isEqualByComparingTo(BigDecimal.ZERO));
        }

        @Test
        @DisplayName("a zero line is charged nothing and the other lines carry the whole fee")
        void zeroLineIsChargedNothing() {
            List<BigDecimal> apportioned =
                    PlatformFeeApportionment.apportion(new BigDecimal("10.00"), totals("0.00", "100.00", null));

            assertThat(apportioned.get(0)).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(apportioned.get(1)).isEqualByComparingTo(new BigDecimal("10.00"));
            assertThat(apportioned.get(2)).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("no fee configured charges nothing")
        void nullOrZeroFeeChargesNothing() {
            assertThat(PlatformFeeApportionment.apportion(null, totals("100.00", "50.00")))
                    .allSatisfy(amount -> assertThat(amount).isEqualByComparingTo(BigDecimal.ZERO));
            assertThat(PlatformFeeApportionment.apportion(BigDecimal.ZERO, totals("100.00", "50.00")))
                    .allSatisfy(amount -> assertThat(amount).isEqualByComparingTo(BigDecimal.ZERO));
        }

        @Test
        @DisplayName("a flat fee larger than the order is capped at what was collected")
        void feeLargerThanTheOrderIsCapped() {
            // A FLAT platform fee of 50 on a 20.00 order. Taking more off the top than came in would
            // net negative and produce a nonsensical negative credit.
            List<BigDecimal> apportioned =
                    PlatformFeeApportionment.apportion(new BigDecimal("50.00"), totals("20.00"));

            assertThat(apportioned).containsExactly(new BigDecimal("20.00"));
        }

        @Test
        @DisplayName("no line items apportions nothing")
        void emptyOrderApportionsNothing() {
            assertThat(PlatformFeeApportionment.apportion(new BigDecimal("10.00"), List.of())).isEmpty();
            assertThat(PlatformFeeApportionment.apportion(new BigDecimal("10.00"), null)).isEmpty();
        }
    }

    @Nested
    @DisplayName("reading an order")
    class ReadingAnOrder {

        private OrderResponse order(String fee, String... lineTotals) {
            List<CartItemResponse> items = Arrays.stream(lineTotals)
                    .map(total -> CartItemResponse.builder().total(new BigDecimal(total)).build())
                    .toList();
            return OrderResponse.builder()
                    .items(items)
                    .total(sum(totals(lineTotals)))
                    .platformFee(fee == null ? null
                            : new PlatformFeeBreakdown(new BigDecimal(fee), "KES", null, null, null, null, null))
                    .build();
        }

        @Test
        @DisplayName("the apportionment is parallel to the order's line items")
        void apportionsAcrossItemsInOrder() {
            List<BigDecimal> apportioned =
                    PlatformFeeApportionment.apportionAcrossItems(order("10.00", "33.33", "33.33", "33.34"));

            assertThat(apportioned).containsExactly(
                    new BigDecimal("3.33"), new BigDecimal("3.33"), new BigDecimal("3.34"));
        }

        @Test
        @DisplayName("an order with no platform fee charges nothing")
        void orderWithoutAFeeChargesNothing() {
            assertThat(PlatformFeeApportionment.apportionAcrossItems(order(null, "100.00", "50.00")))
                    .allSatisfy(amount -> assertThat(amount).isEqualByComparingTo(BigDecimal.ZERO));
        }

        @Test
        @DisplayName("a null order apportions nothing")
        void nullOrderApportionsNothing() {
            assertThat(PlatformFeeApportionment.apportionAcrossItems(null)).isEmpty();
        }
    }
}
