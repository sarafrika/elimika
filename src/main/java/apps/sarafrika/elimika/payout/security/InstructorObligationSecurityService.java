package apps.sarafrika.elimika.payout.security;

import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Authorization for an instructor's own statement of what they are owed.
 * <p>
 * Organisation-scoped routes are guarded by {@code organisationSecurityService}, which already
 * answers "may this caller read/change this organisation's data". This covers the other side: a
 * statement is keyed by user, and the people who may legitimately read someone's earnings are the
 * instructor themselves, a platform admin doing support, and an administrator of an organisation the
 * instructor actually belongs to — the last checked against the target, not against a role the
 * caller happens to hold in some unrelated organisation.
 */
@Service("instructorObligationSecurityService")
@RequiredArgsConstructor
public class InstructorObligationSecurityService {

    private final DomainSecurityService domainSecurityService;

    public boolean canReadStatement(UUID instructorUserUuid) {
        if (instructorUserUuid == null) {
            return false;
        }
        UUID callerUuid = domainSecurityService.getCurrentUserUuid();
        if (callerUuid != null && callerUuid.equals(instructorUserUuid)) {
            return true;
        }
        return domainSecurityService.isPlatformAdmin()
                || domainSecurityService.administersOrganisationOf(instructorUserUuid);
    }
}
