package apps.sarafrika.elimika.authentication.internal;

import apps.sarafrika.elimika.authentication.services.KeycloakOrganisationService;
import apps.sarafrika.elimika.authentication.services.KeycloakUserService;
import apps.sarafrika.elimika.common.event.user.AddUserToOrganisationEvent;
import apps.sarafrika.elimika.common.event.user.SuccessfulUserUpdateEvent;
import apps.sarafrika.elimika.common.event.user.UserCreationEvent;
import apps.sarafrika.elimika.common.event.user.UserUpdateEvent;
import apps.sarafrika.elimika.common.exceptions.RecordNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component @RequiredArgsConstructor @Slf4j
class UserEventsListener {
    private final KeycloakUserService keycloakUserService;
    private final KeycloakOrganisationService keycloakOrganisationService;

    private final ApplicationEventPublisher eventPublisher;

    @ApplicationModuleListener
    void onUserCreation(UserCreationEvent event) {
        keycloakUserService.getUserByUsername(event.email(), event.realm()).orElse(
                keycloakUserService.createUser(event)
        );
    }

    @EventListener
    void onUserUpdate(UserUpdateEvent event) {
        log.debug("User update event received {}", event);
        UserRepresentation representation = keycloakUserService.getUserById(event.keyCloakId(), event.realm())
                .orElseThrow(() -> new RecordNotFoundException("User not found on keycloak"));
        representation.setFirstName(event.firstName());
        representation.setLastName(event.lastName());
        representation.setEmail(event.email());
        representation.setUsername(event.username());
        representation.setEnabled(event.active());
        keycloakUserService.updateUser(event.keyCloakId(), representation, event.realm());
        eventPublisher.publishEvent(new SuccessfulUserUpdateEvent(event.keyCloakId(), event.blastWaveId()));
    }

    @ApplicationModuleListener
    void onAddUserToOrganisation(AddUserToOrganisationEvent event) {
        keycloakOrganisationService.addUserToOrganization(event.realm(), event.organisationId(), event.userId());
    }

}
