package apps.sarafrika.elimika.tenancy.internal;

import apps.sarafrika.elimika.shared.event.user.ProfileModerationDecidedEvent;
import apps.sarafrika.elimika.shared.event.user.ProfileReviewRequestedEvent;
import apps.sarafrika.elimika.shared.utils.enums.DomainApprovalStatus;
import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import apps.sarafrika.elimika.tenancy.services.DomainApprovalService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Locale;

/** Keeps domain approval in step with profile reviews, and tells admins when a profile is submitted. */
@Component
@RequiredArgsConstructor
class ProfileModerationListener {

    private final DomainApprovalService domainApprovalService;
    private final DomainApprovalNotifier notifier;

    @EventListener
    void onProfileReviewRequested(ProfileReviewRequestedEvent event) {
        notifier.requested(event.userUuid(), event.userDomain());
    }

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
