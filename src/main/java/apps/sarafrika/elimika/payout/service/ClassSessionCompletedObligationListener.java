package apps.sarafrika.elimika.payout.service;

import apps.sarafrika.elimika.shared.event.timetabling.ClassSessionCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Turns a delivered class session into a persisted obligation on the organisation that owns the
 * class.
 *
 * <h2>Durability</h2>
 * Money owed to a real person must not depend on a thread surviving, so this follows the same shape
 * as {@link OrderCaptureWalletCreditListener}:
 * <ul>
 *     <li>{@code @TransactionalEventListener} makes this a {@code TransactionalApplicationListener},
 *     which is what causes Modulith's {@code PersistentApplicationEventMulticaster} to write a row
 *     into {@code event_publication} <em>before</em> the listener runs.</li>
 *     <li>{@code fallbackExecution = true} is kept even though it is not strictly required today.
 *     Every current publisher of {@link ClassSessionCompletedEvent} is inside a transaction
 *     ({@code TimetableServiceImpl} is {@code @Transactional} at class level, and
 *     {@code SchedulingEventListener#performStatusUpdateCheck} is annotated directly), so the
 *     listener would fire after commit regardless. The flag is insurance: the failure mode it
 *     prevents — a new, non-transactional publisher silently never accruing anything — is invisible,
 *     and it costs nothing when a transaction is present, because the fallback only applies when
 *     there is none.</li>
 *     <li>{@code @Async} keeps accrual off the thread that ended the session, so a payout failure can
 *     never fail the act of marking a class complete.</li>
 *     <li>Failures are rethrown. Modulith's completion advisor marks a publication complete only on a
 *     clean return, so a failed accrual stays incomplete and is retried by
 *     {@link IncompleteObligationAccrualRetryJob} and on restart
 *     ({@code spring.modulith.republish-outstanding-events-on-restart=true}).</li>
 * </ul>
 * Retrying is safe by construction: accrual is idempotent per
 * {@code (class_definition_uuid, session_uuid, instructor_uuid)}, backed by the unique constraint
 * {@code uq_instructor_obligations_session}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ClassSessionCompletedObligationListener {

    private final InstructorObligationService instructorObligationService;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(fallbackExecution = true)
    public void handleClassSessionCompleted(ClassSessionCompletedEvent event) {
        if (event == null || event.scheduledInstanceUuid() == null) {
            return;
        }

        try {
            instructorObligationService.accrueForCompletedSession(
                    event.classDefinitionUuid(),
                    event.scheduledInstanceUuid(),
                    event.instructorUuid(),
                    event.completedAt());
        } catch (Exception ex) {
            log.error("Failed to accrue the instructor obligation for session {} of class {}: {}",
                    event.scheduledInstanceUuid(), event.classDefinitionUuid(), ex.getMessage(), ex);
            throw new InstructorObligationAccrualFailedException(
                    "Failed to accrue the instructor obligation for session "
                            + event.scheduledInstanceUuid() + ". The event publication is left"
                            + " incomplete and will be retried.",
                    ex);
        }
    }
}
