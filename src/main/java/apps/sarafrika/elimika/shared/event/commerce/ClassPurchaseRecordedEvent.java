package apps.sarafrika.elimika.shared.event.commerce;

import java.util.UUID;

/**
 * A paid-for class purchase has been written down, so the buyer may now be enrolled.
 * <p>
 * Published from inside the purchase recorder rather than from an {@code OrderCompletedEvent}
 * listener. Enrolment goes through the paywall, and the paywall reads the very row the recorder
 * writes — a sibling listener has no ordering guarantee against it and would race the record it
 * depends on.
 *
 * @param studentUuid         the student profile that bought the seat, never the user UUID
 * @param classDefinitionUuid the class the seat belongs to
 * @param orderId             the order the purchase was recorded against, for traceability
 */
public record ClassPurchaseRecordedEvent(
        UUID studentUuid,
        UUID classDefinitionUuid,
        String orderId
) {
}
