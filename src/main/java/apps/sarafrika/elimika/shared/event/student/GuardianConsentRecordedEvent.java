package apps.sarafrika.elimika.shared.event.student;

import java.util.UUID;

/**
 * Published when a guardian has consented to a minor joining an organisation.
 * <p>
 * The guardian link itself lives in the student module, which owns
 * {@code student_guardian_links} and the learner profile it points at. Carrying the
 * consent across as an event keeps the module boundary intact - tenancy may not depend
 * on student - and lets the link be created once the learner profile exists, which for a
 * brand-new invitee is only after their {@code student} domain has been assigned.
 *
 * @param studentUserUuid  the minor's user account
 * @param guardianUserUuid the consenting guardian's user account
 * @param relationshipType PARENT, GUARDIAN or SPONSOR
 * @param shareScope       how much of the child's learning the guardian may see
 * @param organisationUuid the organisation the minor was invited to join
 */
public record GuardianConsentRecordedEvent(
        UUID studentUserUuid,
        UUID guardianUserUuid,
        String relationshipType,
        String shareScope,
        UUID organisationUuid
) {
}
