package apps.sarafrika.elimika.authentication.internal;

import apps.sarafrika.elimika.authentication.spi.KeycloakUserService;
import apps.sarafrika.elimika.shared.event.user.RegistrationActionsEmailRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** Sends the set-password email after commit; a failed send stays in the event log for retry. */
@Component
@RequiredArgsConstructor
@Slf4j
class RegistrationEmailListener {

    private final KeycloakUserService keycloakUserService;

    @ApplicationModuleListener
    void onActionsEmailRequested(RegistrationActionsEmailRequestedEvent event) {
        keycloakUserService.sendRequiredActionEmail(event.keycloakUserId(), event.actions(), event.realm(),
                event.clientId(), event.redirectUri(), event.lifespanSeconds());
        log.info("Sent account action email to Keycloak user {}", event.keycloakUserId());
    }
}
