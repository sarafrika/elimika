package apps.sarafrika.elimika.shared.event.user;

import java.util.UUID;

/** A user now holds a pending domain (e.g. {@code course_creator}) awaiting platform admin review. */
public record DomainApprovalRequestedEvent(
        UUID userUuid,
        String userDomain
) {
}
