package apps.sarafrika.elimika.shared.utils.commerce;

import apps.sarafrika.elimika.shared.dto.commerce.CartItemResponse;
import apps.sarafrika.elimika.shared.dto.commerce.OrderResponse;
import apps.sarafrika.elimika.shared.dto.commerce.PlatformFeeBreakdown;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Splits an order-level platform fee across the order's line items.
 * <p>
 * The platform fee is computed <em>once, on the whole order total</em>, but revenue is credited
 * <em>per line item</em> to a party resolved from that line's course or class. The fee therefore has
 * to be apportioned before it can be netted off, and the apportionment has to be identical wherever
 * it is done - the crediting side and the recording side must agree to the cent or the per-line
 * reconciliation {@code gross = fee + credited + retained} silently stops holding.
 * <p>
 * That is why this lives in {@code shared} rather than in {@code commerce} (where the fee is
 * computed) or in {@code payout} (where the credit is applied): {@code payout} is not permitted to
 * depend on {@code commerce}, so a helper in either module would have to be duplicated in the other.
 * It is a pure function over the {@code shared.dto.commerce} DTOs that both modules already read.
 *
 * <h2>Rounding</h2>
 * Money is allocated in whole cents and the allocation <strong>sums exactly to the fee</strong>.
 * Rounding each line independently does not: a fee of {@code 10.00} over lines of
 * {@code 33.33 / 33.33 / 33.34} gives exact shares of {@code 3.333 / 3.333 / 3.334}, which round
 * (HALF_UP, 2dp) to {@code 3.33 / 3.33 / 3.33} and lose a cent.
 * <p>
 * So the classic largest-remainder method is used instead: floor every exact share to the cent, then
 * hand the leftover cents out one at a time to the lines with the largest discarded fractions (ties
 * broken by line order). The example above becomes {@code 3.33 / 3.33 / 3.34}, summing to exactly
 * {@code 10.00}. Flooring rather than HALF_UP per line is deliberate - it is what guarantees no line
 * is ever charged more fee than its own share, and that the residual handed out is non-negative so
 * no line can be pushed below zero.
 *
 * <h2>Degenerate inputs</h2>
 * <ul>
 *     <li>No fee, a zero fee, or an order whose lines total zero &rarr; every line gets zero.</li>
 *     <li>A zero (or null, or negative) line total gets nothing, rather than a division by zero or a
 *     negative fee.</li>
 *     <li>A flat fee larger than the money actually collected is capped at the collected total.
 *     Taking more off the top than was taken in would produce a negative net and, from it, a
 *     nonsensical negative credit.</li>
 * </ul>
 */
public final class PlatformFeeApportionment {

    /** Money is held to the cent, as {@code applyShare} and the fee calculator already do. */
    public static final int MONEY_SCALE = 2;

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(MONEY_SCALE);
    private static final BigDecimal ONE_CENT = BigDecimal.ONE.movePointLeft(MONEY_SCALE);
    /** Ample precision for the intermediate ratio; the result is floored to the cent anyway. */
    private static final MathContext RATIO = new MathContext(20, RoundingMode.HALF_UP);

    private PlatformFeeApportionment() {
    }

    /**
     * Apportions an order's platform fee across its line items, in the order the items appear.
     *
     * @return one amount per line item, never null, always the same size as {@code order.getItems()}
     */
    public static List<BigDecimal> apportionAcrossItems(OrderResponse order) {
        if (order == null || order.getItems() == null || order.getItems().isEmpty()) {
            return List.of();
        }
        PlatformFeeBreakdown breakdown = order.getPlatformFee();
        List<BigDecimal> lineTotals = new ArrayList<>(order.getItems().size());
        for (CartItemResponse item : order.getItems()) {
            lineTotals.add(item == null ? null : item.getTotal());
        }
        return apportion(breakdown == null ? null : breakdown.amount(), lineTotals);
    }

    /**
     * Apportions {@code orderFee} across {@code lineTotals} in proportion to each line's share of
     * their sum, to the cent, with the allocation summing exactly to the fee.
     *
     * @param orderFee   the fee charged on the whole order; null or non-positive allocates nothing
     * @param lineTotals the gross total of each line; null and negative entries are treated as zero
     * @return one non-negative amount per line, parallel to {@code lineTotals}
     */
    public static List<BigDecimal> apportion(BigDecimal orderFee, List<BigDecimal> lineTotals) {
        if (lineTotals == null || lineTotals.isEmpty()) {
            return List.of();
        }
        int lineCount = lineTotals.size();
        List<BigDecimal> allocation = new ArrayList<>(lineCount);
        for (int i = 0; i < lineCount; i++) {
            allocation.add(ZERO);
        }

        BigDecimal pool = BigDecimal.ZERO;
        for (BigDecimal lineTotal : lineTotals) {
            pool = pool.add(collectable(lineTotal));
        }
        if (orderFee == null || orderFee.signum() <= 0 || pool.signum() <= 0) {
            return allocation;
        }

        // Charge the fee in whole cents, and never more than was actually collected.
        BigDecimal fee = orderFee.setScale(MONEY_SCALE, RoundingMode.HALF_UP)
                .min(pool.setScale(MONEY_SCALE, RoundingMode.DOWN));
        if (fee.signum() <= 0) {
            return allocation;
        }

        List<BigDecimal> discardedFractions = new ArrayList<>(lineCount);
        BigDecimal allocated = BigDecimal.ZERO;
        for (int i = 0; i < lineCount; i++) {
            BigDecimal line = collectable(lineTotals.get(i));
            if (line.signum() == 0) {
                discardedFractions.add(BigDecimal.ZERO);
                continue;
            }
            BigDecimal exactShare = fee.multiply(line).divide(pool, RATIO);
            BigDecimal wholeCents = exactShare.setScale(MONEY_SCALE, RoundingMode.DOWN);
            allocation.set(i, wholeCents);
            allocated = allocated.add(wholeCents);
            discardedFractions.add(exactShare.subtract(wholeCents));
        }

        distributeResidual(fee.subtract(allocated), allocation, discardedFractions, lineTotals);
        return allocation;
    }

    /**
     * Hands the cents lost to flooring back out, largest discarded fraction first. Each line can
     * take at most one cent here (the residual is strictly smaller than one cent per line), and a
     * line is skipped if the extra cent would take its fee past its own total.
     */
    private static void distributeResidual(
            BigDecimal residual,
            List<BigDecimal> allocation,
            List<BigDecimal> discardedFractions,
            List<BigDecimal> lineTotals
    ) {
        if (residual.signum() <= 0) {
            return;
        }
        int remainingCents = residual.divide(ONE_CENT, 0, RoundingMode.HALF_UP).intValueExact();
        List<Integer> byLargestFraction = IntStream.range(0, allocation.size())
                .boxed()
                .sorted(Comparator.<Integer, BigDecimal>comparing(discardedFractions::get)
                        .reversed()
                        .thenComparing(Comparator.naturalOrder()))
                .toList();

        for (Integer index : byLargestFraction) {
            if (remainingCents <= 0) {
                return;
            }
            BigDecimal line = collectable(lineTotals.get(index));
            BigDecimal bumped = allocation.get(index).add(ONE_CENT);
            if (line.signum() == 0 || bumped.compareTo(line) > 0) {
                continue;
            }
            allocation.set(index, bumped);
            remainingCents--;
        }
    }

    /** A null or negative line total collects nothing, so it can neither absorb nor create fee. */
    private static BigDecimal collectable(BigDecimal lineTotal) {
        return lineTotal == null || lineTotal.signum() <= 0 ? BigDecimal.ZERO : lineTotal;
    }
}
