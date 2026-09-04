package apps.sarafrika.elimika.instructor.security;

import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.shared.security.DomainSecurityService;
import apps.sarafrika.elimika.shared.spi.instructor.InstructorCredentialReviewerLookup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Authorization for an instructor's credential documents — certificates, licences and the identity
 * papers filed to back them.
 * <p>
 * Everything here is checked against the instructor being read, never against a role the caller
 * happens to hold. That distinction is the whole point: {@code course_creator}, {@code admin} and
 * {@code organisation_user} are all domains a user can hold platform-wide or in an unrelated
 * organisation, so testing for them would let anyone who can obtain one read every instructor's
 * papers. The four ways in are all relationships to <em>this</em> instructor:
 * <ul>
 *   <li>the instructor themselves;</li>
 *   <li>a platform admin, who runs the verification queue;</li>
 *   <li>staff of an organisation the instructor belongs to;</li>
 *   <li>a reviewer of an application this instructor made — see
 *       {@link InstructorCredentialReviewerLookup}.</li>
 * </ul>
 * Everything else about an instructor that a directory or a hiring page needs — name, skills,
 * education, experience, ratings and the platform's own verification flag — is readable without
 * this; only the documents behind the verification are gated.
 */
@Service("instructorCredentialSecurityService")
@RequiredArgsConstructor
@Slf4j
public class InstructorCredentialSecurityService {

    private final DomainSecurityService domainSecurityService;
    private final InstructorLookupService instructorLookupService;
    private final ObjectProvider<InstructorCredentialReviewerLookup> reviewerLookups;

    /**
     * @param instructorUuid the instructor profile whose documents are being read
     * @return true when the caller may read this instructor's credential documents
     */
    public boolean canReadCredentials(UUID instructorUuid) {
        if (instructorUuid == null) {
            return false;
        }
        if (domainSecurityService.isInstructorWithUuid(instructorUuid) || domainSecurityService.isPlatformAdmin()) {
            return true;
        }

        UUID callerUuid = domainSecurityService.getCurrentUserUuid();
        if (callerUuid == null) {
            return false;
        }

        UUID instructorUserUuid = instructorUserUuid(instructorUuid);
        if (instructorUserUuid != null && domainSecurityService.staffsOrganisationOf(instructorUserUuid)) {
            return true;
        }

        return reviewerLookups.stream()
                .anyMatch(lookup -> lookup.isReviewingApplicationFrom(instructorUuid, callerUuid));
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
