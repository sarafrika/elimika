package apps.sarafrika.elimika.tenancy.internal;

import apps.sarafrika.elimika.tenancy.services.OrganisationInvitationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Retires invitations whose window has passed.
 * <p>
 * Without this an unanswered offer would stay live indefinitely, and its emailed link
 * would keep working long after the organisation expected it to.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2026-07-28
 */
@Component
@RequiredArgsConstructor
@Slf4j
class OrganisationInvitationExpiryScheduler {

    private final OrganisationInvitationService invitationService;

    @Scheduled(cron = "0 45 0 * * *")
    void expireLapsedInvitations() {
        try {
            invitationService.expireLapsed();
        } catch (Exception e) {
            log.error("Failed to expire lapsed organisation invitations", e);
        }
    }
}
