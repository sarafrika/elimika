package apps.sarafrika.elimika.classes.internal.security;

import apps.sarafrika.elimika.classes.repository.ClassMarketplaceJobApplicationRepository;
import apps.sarafrika.elimika.shared.spi.instructor.InstructorCredentialReviewerLookup;
import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * The marketplace's answer to "may this caller read that instructor's credentials?": yes, while the
 * instructor has applied to an advert posted by an organisation the caller staffs.
 * <p>
 * Both halves are scoped to the subject. The caller's organisations are those they actually hold a
 * role in — not the {@code organisation_user} domain in the abstract — and the advert has to be one
 * of those organisations' own, so applying to organisation A never exposes the applicant to
 * organisation B.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MarketplaceApplicationReviewerLookup implements InstructorCredentialReviewerLookup {

    private static final List<UserDomain> HIRING_DOMAINS = List.of(UserDomain.admin, UserDomain.organisation_user);

    private final ClassMarketplaceJobApplicationRepository applicationRepository;
    private final UserLookupService userLookupService;

    @Override
    public boolean isReviewingApplicationFrom(UUID instructorUuid, UUID reviewerUserUuid) {
        if (instructorUuid == null || reviewerUserUuid == null) {
            return false;
        }
        try {
            List<UUID> hiringOrganisations = userLookupService.getUserOrganizations(reviewerUserUuid).stream()
                    .filter(organisationUuid -> HIRING_DOMAINS.stream()
                            .anyMatch(domain -> userLookupService.userBelongsToOrganizationWithDomain(
                                    reviewerUserUuid, organisationUuid, domain)))
                    .toList();
            if (hiringOrganisations.isEmpty()) {
                return false;
            }

            return applicationRepository.existsByInstructorUuidAndJobOrganisationUuidIn(
                    instructorUuid, hiringOrganisations);
        } catch (Exception e) {
            log.error("Error checking marketplace review rights over instructor {}", instructorUuid, e);
            return false;
        }
    }
}
