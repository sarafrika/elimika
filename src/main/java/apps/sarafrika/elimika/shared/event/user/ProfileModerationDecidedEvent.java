package apps.sarafrika.elimika.shared.event.user;

import apps.sarafrika.elimika.shared.utils.enums.DomainApprovalStatus;

import java.util.UUID;

/**
 * A platform admin decided a profile review (course creator, instructor, organisation). The
 * tenancy module applies the same decision to the user's domain mapping, so the profile review
 * and dashboard access cannot drift apart.
 *
 * @param userUuid the profile owner
 * @param userDomain the domain the profile belongs to, e.g. {@code course_creator}
 * @param status the resulting domain state
 * @param reason optional reviewer note shown to the user
 * @param reviewedBy the deciding admin's user uuid, when known
 */
public record ProfileModerationDecidedEvent(
        UUID userUuid,
        String userDomain,
        DomainApprovalStatus status,
        String reason,
        UUID reviewedBy
) {
}
