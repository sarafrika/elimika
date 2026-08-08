package apps.sarafrika.elimika.timetabling.internal;

import apps.sarafrika.elimika.shared.event.commerce.ClassPurchaseRecordedEvent;
import apps.sarafrika.elimika.timetabling.spi.EnrollmentRequestDTO;
import apps.sarafrika.elimika.timetabling.spi.TimetableService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Enrols a student into the class they have paid for.
 *
 * <h2>Durability</h2>
 * A learner who has been charged must end up with their seat, so this follows the same shape as
 * {@code ClassSessionCompletedObligationListener}: {@code @TransactionalEventListener} makes Modulith
 * persist the event before the listener runs, and {@code fallbackExecution = true} keeps a future
 * non-transactional publisher from silently enrolling nobody.
 *
 * <h2>Repeats</h2>
 * The event is re-delivered on restart if the listener never completed, and a purchase can be
 * re-recorded when a payment status is refreshed, so this must be safe to run twice.
 * {@code TimetableServiceImpl#enrollStudent} already skips instances the student is enrolled in, which
 * makes the happy path idempotent.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ClassPurchaseEnrolmentListener {

    private final TimetableService timetableService;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(fallbackExecution = true)
    public void handleClassPurchaseRecorded(ClassPurchaseRecordedEvent event) {
        if (event == null || event.studentUuid() == null || event.classDefinitionUuid() == null) {
            return;
        }

        try {
            timetableService.enrollStudent(new EnrollmentRequestDTO(
                    event.classDefinitionUuid(),
                    event.studentUuid()));
            log.info("Enrolled student {} into class {} from order {}",
                    event.studentUuid(), event.classDefinitionUuid(), event.orderId());
        } catch (Exception e) {
            // The seat is checked at checkout, but a concurrent buyer can still take the last one.
            // Losing the money is not an option and neither is poisoning the event, so this is
            // recorded loudly and left for a human: the purchase row is the evidence of what is owed.
            log.error("Paid seat could not be enrolled: student {}, class {}, order {}: {}",
                    event.studentUuid(), event.classDefinitionUuid(), event.orderId(), e.getMessage(), e);
        }
    }
}
