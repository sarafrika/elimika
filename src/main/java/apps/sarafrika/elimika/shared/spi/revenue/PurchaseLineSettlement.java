package apps.sarafrika.elimika.shared.spi.revenue;

import java.math.BigDecimal;

/**
 * What happened to the money collected on a single purchased line, once the earner has been paid.
 * <p>
 * The four amounts are a closed set: {@code gross = platformFee + credited + retained}. Anything
 * collected that was not taken as fee and not credited to an earner is, by definition, retained by
 * the platform - most often because only one side of a course's creator/instructor split is credited
 * per purchase scope, leaving the other side's percentage allocated to nobody. Recording it makes
 * that gap a number rather than something discoverable only by subtraction.
 * <p>
 * The invariant is enforced here rather than trusted, because a set of figures that does not
 * reconcile is worse than no figures at all.
 *
 * @param orderId           the commerce order the line belongs to
 * @param lineItemId        the line item, unique within the commerce stack
 * @param grossAmount       the line total the buyer actually paid
 * @param platformFeeAmount the order's platform fee apportioned to this line, taken off the top
 * @param creditedAmount    what was credited to an earner's wallet for this line; zero if nobody was
 * @param retainedAmount    collected, not fee, credited to no earner
 */
public record PurchaseLineSettlement(
        String orderId,
        String lineItemId,
        BigDecimal grossAmount,
        BigDecimal platformFeeAmount,
        BigDecimal creditedAmount,
        BigDecimal retainedAmount
) {

    public PurchaseLineSettlement {
        if (grossAmount == null || platformFeeAmount == null
                || creditedAmount == null || retainedAmount == null) {
            throw new IllegalArgumentException("All settlement amounts are required");
        }
        BigDecimal accountedFor = platformFeeAmount.add(creditedAmount).add(retainedAmount);
        if (accountedFor.compareTo(grossAmount) != 0) {
            throw new IllegalArgumentException(
                    "Settlement for order " + orderId + " line " + lineItemId + " does not reconcile:"
                            + " gross " + grossAmount + " != fee " + platformFeeAmount
                            + " + credited " + creditedAmount + " + retained " + retainedAmount);
        }
    }
}
