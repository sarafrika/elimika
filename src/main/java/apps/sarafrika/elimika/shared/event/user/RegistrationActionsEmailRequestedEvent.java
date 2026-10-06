package apps.sarafrika.elimika.shared.event.user;

import java.util.List;

/** Ask Keycloak to email pending account actions. Carries no personal data, since events are persisted. */
public record RegistrationActionsEmailRequestedEvent(
        String keycloakUserId,
        String realm,
        List<String> actions,
        String clientId,
        String redirectUri,
        Integer lifespanSeconds
) {
}
