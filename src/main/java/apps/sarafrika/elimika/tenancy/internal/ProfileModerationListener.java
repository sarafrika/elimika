package apps.sarafrika.elimika.tenancy.internal;

import apps.sarafrika.elimika.shared.event.user.ProfileModerationDecidedEvent;
import apps.sarafrika.elimika.shared.utils.enums.DomainApprovalStatus;
import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import apps.sarafrika.elimika.tenancy.services.DomainApprovalService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Locale;

/** Applies a profile review decision to the domain mapping in the same transaction, so they never drift. */
@Component
@RequiredArgsConstructor
class ProfileModerationListener {

    private final DomainApprovalService domainApprovalService;

    @EventListener
    void onProfileModerationDecided(ProfileModerationDecidedEvent event) {
        UserDomain domain = UserDomain.valueOf(event.userDomain().toLowerCase(Locale.ROOT));
        if (event.status() == DomainApprovalStatus.APPROVED) {
            domainApprovalService.approveReviewedProfile(event.userUuid(), domain, event.reviewedBy());
        } else {
            domainApprovalService.decide(event.userUuid(), domain, event.status(), event.reason(), event.reviewedBy());
        }
    }
}
