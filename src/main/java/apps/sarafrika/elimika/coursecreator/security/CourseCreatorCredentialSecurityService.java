package apps.sarafrika.elimika.coursecreator.security;

import apps.sarafrika.elimika.coursecreator.spi.CourseCreatorLookupService;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Authorization for a course creator's credential records - certifications, professional
 * memberships and education, which carry certificate and membership numbers.
 * <p>
 * Mirrors {@code InstructorCredentialSecurityService}: access is checked against the course
 * creator being read, never against a role the caller holds platform-wide. The ways in are all
 * relationships to <em>this</em> course creator:
 * <ul>
 *   <li>the course creator themselves;</li>
 *   <li>a platform admin, who runs the verification queue;</li>
 *   <li>staff of an organisation the course creator belongs to.</li>
 * </ul>
 * Skills and experience stay readable without this, as they are for instructors.
 */
@Service("courseCreatorCredentialSecurityService")
@RequiredArgsConstructor
@Slf4j
public class CourseCreatorCredentialSecurityService {

    private final DomainSecurityService domainSecurityService;
    private final CourseCreatorLookupService courseCreatorLookupService;

    /**
     * @param courseCreatorUuid the course creator profile whose credentials are being read
     * @return true when the caller may read this course creator's credential records
     */
    public boolean canReadCredentials(UUID courseCreatorUuid) {
        if (courseCreatorUuid == null) {
            return false;
        }
        if (domainSecurityService.isCourseCreatorWithUuid(courseCreatorUuid) || domainSecurityService.isPlatformAdmin()) {
            return true;
        }
        if (domainSecurityService.getCurrentUserUuid() == null) {
            return false;
        }

        UUID courseCreatorUserUuid = courseCreatorUserUuid(courseCreatorUuid);
        return courseCreatorUserUuid != null && domainSecurityService.staffsOrganisationOf(courseCreatorUserUuid);
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
