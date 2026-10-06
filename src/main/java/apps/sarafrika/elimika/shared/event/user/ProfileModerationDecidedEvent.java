package apps.sarafrika.elimika.shared.event.user;

import apps.sarafrika.elimika.shared.utils.enums.DomainApprovalStatus;

import java.util.UUID;

/** A profile review was decided; tenancy applies the same decision to the user's domain mapping. */
public record ProfileModerationDecidedEvent(
        UUID userUuid,
        String userDomain,
        DomainApprovalStatus status,
        String reason,
        UUID reviewedBy
) {
}
