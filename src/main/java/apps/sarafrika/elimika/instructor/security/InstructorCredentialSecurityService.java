package apps.sarafrika.elimika.instructor.security;

import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.profile.spi.ProfileCredentialAccess;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Credential reads on instructor routes, answered by the one profile-wide rule for the instructor's
 * owner: the owner, a platform admin, organisation staff, or a reviewer of an application they made.
 */
@Service("instructorCredentialSecurityService")
@RequiredArgsConstructor
@Slf4j
public class InstructorCredentialSecurityService {

    private final DomainSecurityService domainSecurityService;
    private final InstructorLookupService instructorLookupService;
    private final ProfileCredentialAccess profileCredentialAccess;

    /**
     * @param instructorUuid the instructor profile whose credentials are being read
     * @return true when the caller may read this instructor's credential records
     */
    public boolean canReadCredentials(UUID instructorUuid) {
        if (instructorUuid == null) {
            return false;
        }
        UUID userUuid = instructorUserUuid(instructorUuid);
        if (userUuid == null) {
            return domainSecurityService.isPlatformAdmin();
        }
        return profileCredentialAccess.canReadCredentials(userUuid);
    }

    private UUID instructorUserUuid(UUID instructorUuid) {
        try {
            return instructorLookupService.getInstructorUserUuid(instructorUuid).orElse(null);
        } catch (Exception e) {
            log.error("Error resolving the owning user of instructor {}", instructorUuid, e);
            return null;
        }
    }
}
