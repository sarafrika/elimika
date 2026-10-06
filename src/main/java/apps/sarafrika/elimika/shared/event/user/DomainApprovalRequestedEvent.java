package apps.sarafrika.elimika.shared.event.user;

import java.util.UUID;

/**
 * A user now holds a pending domain that a platform admin has to review.
 *
 * @param userUuid the user awaiting approval
 * @param userDomain the domain name, e.g. {@code course_creator}
 */
public record DomainApprovalRequestedEvent(
        UUID userUuid,
        String userDomain
) {
}
