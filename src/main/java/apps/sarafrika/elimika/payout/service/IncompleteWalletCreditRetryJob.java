package apps.sarafrika.elimika.payout.service;

import apps.sarafrika.elimika.shared.event.commerce.OrderCompletedEvent;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.modulith.events.EventPublication;
import org.springframework.modulith.events.IncompleteEventPublications;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Retries wallet credits that failed when their order was captured.
 * <p>
 * {@link OrderCaptureWalletCreditListener} leaves a failed credit as an incomplete row in
 * {@code event_publication}. Modulith already republishes those on restart, but an earner should not
 * have to wait for a deployment to be paid, so this job resubmits them on a schedule too.
 * <p>
 * The predicate is deliberately narrow - only {@link OrderCompletedEvent} publications, and only
 * those older than {@code grace}. Other modules' incomplete publications are left alone, and the
 * grace period avoids racing a credit that is still in flight. Resubmission is safe regardless:
 * credits are idempotent per {@code orderId:lineItemId}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class IncompleteWalletCreditRetryJob {

    private final IncompleteEventPublications incompleteEventPublications;

    /** How long a credit is given to finish before it is considered stuck and resubmitted. */
    @Value("${payout.wallet-credit.retry-grace:PT5M}")
    private Duration grace;

    @Scheduled(
            initialDelayString = "${payout.wallet-credit.retry-initial-delay:PT2M}",
            fixedDelayString = "${payout.wallet-credit.retry-interval:PT10M}")
    public void retryIncompleteWalletCredits() {
        try {
            incompleteEventPublications.resubmitIncompletePublications(this::isStuckWalletCredit);
        } catch (Exception ex) {
            // Never let a retry sweep kill the scheduler; the publications stay incomplete and the
            // next sweep (or a restart) picks them up again.
            log.error("Failed to resubmit incomplete wallet credit publications: {}", ex.getMessage(), ex);
        }
    }

    private boolean isStuckWalletCredit(EventPublication publication) {
        if (!(publication.getEvent() instanceof OrderCompletedEvent)) {
            return false;
        }
        boolean stuck = publication.getPublicationDate().isBefore(Instant.now().minus(grace));
        if (stuck) {
            log.warn("Resubmitting incomplete wallet credit publication {} published at {}",
                    publication.getIdentifier(), publication.getPublicationDate());
        }
        return stuck;
    }
}
