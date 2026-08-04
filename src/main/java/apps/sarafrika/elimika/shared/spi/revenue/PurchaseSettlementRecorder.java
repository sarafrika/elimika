package apps.sarafrika.elimika.shared.spi.revenue;

/**
 * Writes back onto a recorded purchase line what was actually done with its money.
 * <p>
 * Implemented by {@code commerce}, which owns {@code commerce_purchase_item}, and called by
 * {@code payout}, which is the only module that knows who was credited and how much. Neither can
 * see the other - {@code payout} is not allowed to depend on {@code commerce} - so the seam is this
 * interface in {@code shared}, the same shape as the read-side {@link CommerceRevenueQueryService}.
 */
public interface PurchaseSettlementRecorder {

    /**
     * Records the fee, credit and retention for one purchased line. Idempotent: replaying the same
     * settlement overwrites it with the same figures.
     *
     * @throws IllegalStateException if the purchase line is not (yet) recorded, so the caller can
     *                               leave its event publication incomplete and try again rather than
     *                               losing the audit trail
     */
    void recordLineSettlement(PurchaseLineSettlement settlement);
}
