package apps.sarafrika.elimika.shared.event.user;

import apps.sarafrika.elimika.shared.utils.enums.DomainApprovalStatus;

import java.util.UUID;

/** A platform admin decided a domain through the generic moderation route; domain modules mirror the decision. */
public record DomainModeratedEvent(
        UUID userUuid,
        String userDomain,
        DomainApprovalStatus status,
        String reason,
        UUID reviewedBy
) {
}
