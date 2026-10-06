package apps.sarafrika.elimika.instructor.security;

import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.profile.spi.ProfileAccessGrant;
import apps.sarafrika.elimika.shared.spi.instructor.InstructorCredentialReviewerLookup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Lets whoever is deciding an application the user lodged as an instructor (training, marketplace)
 * read that user's shared credentials. Fails closed.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class InstructorApplicationReviewerGrant implements ProfileAccessGrant {

    private final InstructorLookupService instructorLookupService;
    private final ObjectProvider<InstructorCredentialReviewerLookup> reviewerLookups;

    @Override
    public boolean grantsCredentialRead(UUID subjectUserUuid, UUID callerUserUuid) {
        try {
            UUID instructorUuid = instructorLookupService.findInstructorUuidByUserUuid(subjectUserUuid).orElse(null);
            if (instructorUuid == null) {
                return false;
            }
            return reviewerLookups.stream()
                    .anyMatch(lookup -> lookup.isReviewingApplicationFrom(instructorUuid, callerUserUuid));
        } catch (RuntimeException e) {
            log.error("Error checking application reviewers of user {}", subjectUserUuid, e);
            return false;
        }
    }
}
