package apps.sarafrika.elimika.authentication.internal;

import apps.sarafrika.elimika.authentication.services.KeycloakRoleService;
import apps.sarafrika.elimika.authentication.services.KeycloakUserService;
import apps.sarafrika.elimika.shared.event.role.AssignRoleToUserEvent;
import apps.sarafrika.elimika.shared.event.role.CreateRoleOnKeyCloakEvent;
import apps.sarafrika.elimika.shared.event.role.SuccessfulRoleCreationOnKeycloakEvent;
import lombok.RequiredArgsConstructor;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RolesEventListener {
    private final KeycloakRoleService keycloakRoleService;
    private final KeycloakUserService keycloakUserService;

    private final ApplicationEventPublisher applicationEventPublisher;

    @EventListener
    void onRoleCreation(CreateRoleOnKeyCloakEvent event) {
        RoleRepresentation role = keycloakRoleService.getRoleByName(event.roleName(), event.realm()).orElse(
                keycloakRoleService.createRole(event.roleName(), event.description(), event.realm())
        );
        applicationEventPublisher.publishEvent(new SuccessfulRoleCreationOnKeycloakEvent(event.sarafrikaCorrelationId(), UUID.fromString(role.getId())));
    }

    @ApplicationModuleListener
    void assignRoleToUser(AssignRoleToUserEvent event) {
        keycloakRoleService.assignRoleToUser(event.userKeyCloakId().toString(), event.roleName(), event.realm());
        keycloakUserService.logoutUser(event.userKeyCloakId().toString(), event.realm());
    }
}
