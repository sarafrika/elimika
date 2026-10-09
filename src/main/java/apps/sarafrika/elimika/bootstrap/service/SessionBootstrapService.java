package apps.sarafrika.elimika.bootstrap.service;

import apps.sarafrika.elimika.bootstrap.dto.ActiveOrganisationDTO;
import apps.sarafrika.elimika.bootstrap.dto.RoleProfilesDTO;
import apps.sarafrika.elimika.bootstrap.dto.SessionBootstrapDTO;
import apps.sarafrika.elimika.coursecreator.spi.CourseCreatorLookupService;
import apps.sarafrika.elimika.instructor.spi.InstructorLookupService;
import apps.sarafrika.elimika.notifications.spi.NotificationCountsLookupService;
import apps.sarafrika.elimika.notifications.spi.UnreadNotificationSummary;
import apps.sarafrika.elimika.student.spi.StudentLookupService;
import apps.sarafrika.elimika.tenancy.dto.UserDTO;
import apps.sarafrika.elimika.tenancy.dto.UserOrganisationAffiliationDTO;
import apps.sarafrika.elimika.tenancy.spi.UserProfileLookupService;
import apps.sarafrika.elimika.wallet.service.WalletBalanceSummary;
import apps.sarafrika.elimika.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Composes the session bootstrap from existing module SPIs, a fixed handful of indexed reads
 * regardless of how many domains or affiliations the caller has. Only the user record is required.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionBootstrapService {

    private static final Set<String> ORGANISATION_DOMAINS = Set.of("organisation_user", "organisation");

    private final UserProfileLookupService userProfileLookupService;
    private final StudentLookupService studentLookupService;
    private final InstructorLookupService instructorLookupService;
    private final CourseCreatorLookupService courseCreatorLookupService;
    private final WalletService walletService;
    private final NotificationCountsLookupService notificationCountsLookupService;

    public SessionBootstrapDTO bootstrap(UUID userUuid) {
        UserDTO user = userProfileLookupService.getUserProfile(userUuid);
        List<String> domains = user.userDomain() == null ? List.of() : user.userDomain();

        RoleProfilesDTO profiles = new RoleProfilesDTO(
                optional("student profile", () -> studentLookupService.findStudentUuidByUserUuid(userUuid).orElse(null)),
                optional("instructor profile", () -> instructorLookupService.findInstructorUuidByUserUuid(userUuid).orElse(null)),
                optional("course creator profile",
                        () -> courseCreatorLookupService.findCourseCreatorUuidByUserUuid(userUuid).orElse(null)));
        WalletBalanceSummary wallet = optional("wallet", () -> walletService.getBalanceSummary(userUuid, null));
        UnreadNotificationSummary notifications = optional("notification counts",
                () -> notificationCountsLookupService.summarizeUnread(userUuid, domains));

        return new SessionBootstrapDTO(user, profiles, activeOrganisation(user, domains), wallet, notifications);
    }

    // Mirrors the dashboard's organisation context: the first active affiliation, else the first one.
    static ActiveOrganisationDTO activeOrganisation(UserDTO user, List<String> domains) {
        boolean organisationUser = domains.stream().anyMatch(ORGANISATION_DOMAINS::contains);
        List<UserOrganisationAffiliationDTO> affiliations = user.organisationAffiliations();
        if (!organisationUser || affiliations == null || affiliations.isEmpty()) {
            return null;
        }
        UserOrganisationAffiliationDTO chosen = affiliations.stream()
                .filter(UserOrganisationAffiliationDTO::active)
                .findFirst()
                .orElse(affiliations.getFirst());
        return new ActiveOrganisationDTO(chosen.organisationUuid(), chosen.organisationName(),
                chosen.domainInOrganisation(), chosen.branchUuid(), chosen.branchName(),
                chosen.active());
    }

    // A failing optional section degrades to null so the shell still renders; the caller can refetch it.
    private <T> T optional(String section, Supplier<T> read) {
        try {
            return read.get();
        } catch (RuntimeException ex) {
            log.warn("Session bootstrap could not read the {}: {}", section, ex.getMessage());
            return null;
        }
    }
}
