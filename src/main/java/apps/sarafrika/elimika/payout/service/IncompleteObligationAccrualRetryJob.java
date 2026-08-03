package apps.sarafrika.elimika.payout.service;

import apps.sarafrika.elimika.shared.event.timetabling.ClassSessionCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.modulith.events.EventPublication;
import org.springframework.modulith.events.IncompleteEventPublications;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Retries obligation accruals that failed when their session completed.
 * <p>
 * {@link ClassSessionCompletedObligationListener} leaves a failed accrual as an incomplete row in
 * {@code event_publication}. Modulith republishes those on restart, but an instructor should not have
 * to wait for a deployment before the work they delivered is recorded as owed, so this resubmits them
 * on a schedule too.
 * <p>
 * The predicate is narrow on purpose — only {@link ClassSessionCompletedEvent} publications, and only
 * those older than {@code grace}, so other modules' incomplete publications are left alone and an
 * accrual still in flight is not raced. Resubmission is safe regardless: accrual is idempotent per
 * session and instructor.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class IncompleteObligationAccrualRetryJob {

    private final IncompleteEventPublications incompleteEventPublications;

    /** How long an accrual is given to finish before it is considered stuck and resubmitted. */
    @Value("${payout.instructor-obligation.retry-grace:PT5M}")
    private Duration grace;

    @Scheduled(
            initialDelayString = "${payout.instructor-obligation.retry-initial-delay:PT3M}",
            fixedDelayString = "${payout.instructor-obligation.retry-interval:PT10M}")
    public void retryIncompleteAccruals() {
        try {
            incompleteEventPublications.resubmitIncompletePublications(this::isStuckAccrual);
        } catch (Exception ex) {
            // Never let a retry sweep kill the scheduler; the publications stay incomplete and the
            // next sweep (or a restart) picks them up again.
            log.error("Failed to resubmit incomplete instructor obligation publications: {}", ex.getMessage(), ex);
        }
    }

    private boolean isStuckAccrual(EventPublication publication) {
        if (!(publication.getEvent() instanceof ClassSessionCompletedEvent)) {
            return false;
        }
        boolean stuck = publication.getPublicationDate().isBefore(Instant.now().minus(grace));
        if (stuck) {
            log.warn("Resubmitting incomplete instructor obligation publication {} published at {}",
                    publication.getIdentifier(), publication.getPublicationDate());
        }
        return stuck;
    }
}
