package apps.sarafrika.elimika.coursecreator.security;

import apps.sarafrika.elimika.coursecreator.spi.CourseCreatorLookupService;
import apps.sarafrika.elimika.profile.spi.ProfileCredentialAccess;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Credential reads on course creator routes, answered by the one profile-wide rule for the course
 * creator's owner (the same rule instructor routes use).
 */
@Service("courseCreatorCredentialSecurityService")
@RequiredArgsConstructor
@Slf4j
public class CourseCreatorCredentialSecurityService {

    private final DomainSecurityService domainSecurityService;
    private final CourseCreatorLookupService courseCreatorLookupService;
    private final ProfileCredentialAccess profileCredentialAccess;

    /**
     * @param courseCreatorUuid the course creator profile whose credentials are being read
     * @return true when the caller may read this course creator's credential records
     */
    public boolean canReadCredentials(UUID courseCreatorUuid) {
        if (courseCreatorUuid == null) {
            return false;
        }
        UUID userUuid = courseCreatorUserUuid(courseCreatorUuid);
        if (userUuid == null) {
            return domainSecurityService.isPlatformAdmin();
        }
        return profileCredentialAccess.canReadCredentials(userUuid);
    }

    private UUID courseCreatorUserUuid(UUID courseCreatorUuid) {
        try {
            return courseCreatorLookupService.getCourseCreatorUserUuid(courseCreatorUuid).orElse(null);
        } catch (Exception e) {
            log.error("Error resolving the owning user of course creator {}", courseCreatorUuid, e);
            return null;
        }
    }
}
