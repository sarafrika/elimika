package apps.sarafrika.elimika.tenancy.internal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Builds the links that carry an invitation's raw token.
 * <p>
 * The raw token exists only here and in the resulting email - it is never persisted, so
 * these links cannot be reconstructed from the database.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2026-07-28
 */
@Component
public class InvitationLinkFactory {

    private final String frontendUrl;

    public InvitationLinkFactory(@Value("${app.email.frontend.url:https://elimika.sarafrika.com}") String frontendUrl) {
        this.frontendUrl = frontendUrl.endsWith("/")
                ? frontendUrl.substring(0, frontendUrl.length() - 1)
                : frontendUrl;
    }

    public String invitationLink(String rawToken) {
        return frontendUrl + "/invitations/" + rawToken;
    }

    public String guardianConsentLink(String rawToken) {
        return frontendUrl + "/guardian-invitations/" + rawToken;
    }

    public String organisationInvitationsLink() {
        return frontendUrl + "/dashboard/organisation/invite-students";
    }
}
