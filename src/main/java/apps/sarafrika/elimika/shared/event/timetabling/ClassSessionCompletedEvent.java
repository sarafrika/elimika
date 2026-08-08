package apps.sarafrika.elimika.shared.event.timetabling;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A scheduled class session has been delivered and is now {@code COMPLETED}.
 * <p>
 * Timetabling already had an internal {@code ScheduledInstanceCompletedEvent}, but it is declared
 * inside {@code timetabling.internal} and so is invisible outside the module. This is the same fact
 * restated in {@code shared}, where any module may consume it.
 * <p>
 * It carries only what timetabling actually knows — which session, of which class, delivered by
 * which instructor, when, and how long it ran. Deliberately no money: the rate an organisation owes
 * is resolved by the module that records the obligation, not by the module that runs the calendar.
 * The duration is here because instructor rates are per hour, and the calendar is the only thing
 * that knows how long the session actually was.
 *
 * @param scheduledInstanceUuid the session that completed
 * @param classDefinitionUuid   the class the session belongs to
 * @param instructorUuid        the instructor profile that delivered it, never the user UUID
 * @param completedAt           UTC instant the session was marked complete
 * @param durationMinutes       scheduled length of the session; null when it could not be determined
 */
public record ClassSessionCompletedEvent(
        UUID scheduledInstanceUuid,
        UUID classDefinitionUuid,
        UUID instructorUuid,
        LocalDateTime completedAt,
        Integer durationMinutes
) {
}
