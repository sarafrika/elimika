package apps.sarafrika.elimika.profile.internal.security;

import apps.sarafrika.elimika.profile.spi.ProfileAccessGrant;
import apps.sarafrika.elimika.profile.spi.ProfileCredentialAccess;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.UUID;

/** One credential-read rule: owner, platform admin, staff of the user's organisation, or a {@link ProfileAccessGrant}. */
@Slf4j
@Service("profileCredentialSecurityService")
@RequiredArgsConstructor
public class ProfileCredentialSecurityService implements ProfileCredentialAccess {

    private final DomainSecurityService domainSecurityService;
    private final ObjectProvider<ProfileAccessGrant> accessGrants;

    @Override
    public boolean canReadCredentials(UUID userUuid) {
        if (userUuid == null) {
            return false;
        }
        UUID callerUuid = domainSecurityService.getCurrentUserUuid();
        if (userUuid.equals(callerUuid) || domainSecurityService.isPlatformAdmin()) {
            return true;
        }
        if (callerUuid == null) {
            return false;
        }
        if (domainSecurityService.staffsOrganisationOf(userUuid)) {
            return true;
        }
        return accessGrants.stream().anyMatch(grant -> grants(grant, userUuid, callerUuid));
    }

    /** The owner or a platform admin: who may change the profile. */
    public boolean canEdit(UUID userUuid) {
        if (userUuid == null) {
            return false;
        }
        return userUuid.equals(domainSecurityService.getCurrentUserUuid()) || domainSecurityService.isPlatformAdmin();
    }

    private static boolean grants(ProfileAccessGrant grant, UUID userUuid, UUID callerUuid) {
        try {
            return grant.grantsCredentialRead(userUuid, callerUuid);
        } catch (RuntimeException e) {
            log.error("Profile access grant {} failed; denying", grant.getClass().getSimpleName(), e);
            return false;
        }
    }
}
