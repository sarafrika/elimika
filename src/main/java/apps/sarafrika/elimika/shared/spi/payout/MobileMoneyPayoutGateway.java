package apps.sarafrika.elimika.shared.spi.payout;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Sends money to a mobile-money number and reports what became of it. Implemented by commerce over
 * the mpesa-service gateway; used by the wallet, which may only depend on shared.
 * <p>
 * {@code clientReference} is the caller's id for the payout: submitting it again returns the payout
 * already started, so a retry never pays twice.
 */
public interface MobileMoneyPayoutGateway {

    /** Whether payouts can be sent at all (gateway and payout shortcode configured). */
    boolean isConfigured();

    /** Starts the payout, or finds the one already started for this reference. */
    PayoutOutcome submit(String clientReference, String msisdn, BigDecimal amount, String remarks);

    /** The payout's latest state; empty when the gateway has no payout for this reference. */
    Optional<PayoutOutcome> status(String clientReference);

    /** The payout shortcode's float as last reported by M-Pesa. */
    Optional<PayoutFloat> currentFloat();

    enum PayoutState { PENDING, SUCCESS, FAILED }

    /**
     * @param timedOut true when M-Pesa reported a queue timeout and the outcome is still unknown
     */
    record PayoutOutcome(String gatewayPayoutUuid, PayoutState state, String receipt, String recipientName,
                         BigDecimal chargesPaid, String failureReason, boolean timedOut,
                         LocalDateTime completedAt) {
    }

    record PayoutFloat(BigDecimal utilityAccountFunds, BigDecimal workingAccountFunds, LocalDateTime reportedAt) {
    }
}
