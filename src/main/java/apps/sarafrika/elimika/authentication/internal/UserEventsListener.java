package apps.sarafrika.elimika.authentication.internal;

import apps.sarafrika.elimika.authentication.services.KeycloakOrganisationService;
import apps.sarafrika.elimika.authentication.services.KeycloakUserService;
import apps.sarafrika.elimika.common.event.user.AddUserToOrganisationEvent;
import apps.sarafrika.elimika.common.event.user.SuccessfulUserUpdateEvent;
import apps.sarafrika.elimika.common.event.user.UserCreationEvent;
import apps.sarafrika.elimika.common.event.user.UserUpdateEvent;
import apps.sarafrika.elimika.common.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
                .orElseThrow(() -> new ResourceNotFoundException("User not found on keycloak"));
        
        // Update core fields
        representation.setFirstName(event.firstName());
        representation.setLastName(event.lastName());
        representation.setEmail(event.email());
        representation.setUsername(event.username());
        representation.setEnabled(event.active());
        
        // Update custom attributes (matching the names used in UserServiceImpl.createUser())
        Map<String, List<String>> attributes = representation.getAttributes();
        if (attributes == null) {
            attributes = new HashMap<>();
            representation.setAttributes(attributes);
        }
        
        // Sync custom attributes from the event using hardcoded attribute names
        updateAttributeIfNotNull(attributes, "middleName", event.middleName());
        updateAttributeIfNotNull(attributes, "primaryPhoneNumber", event.phoneNumber());
        updateAttributeIfNotNull(attributes, "dob", event.dob() != null ? event.dob().toString() : null);
        updateAttributeIfNotNull(attributes, "gender", event.gender() != null ? event.gender().name() : null);
        updateAttributeIfNotNull(attributes, "profileImageUrl", event.profileImageUrl());
        
        log.debug("Updating Keycloak user {} with attributes: {}", event.keyCloakId(), attributes);
        
        keycloakUserService.updateUser(event.keyCloakId(), representation, event.realm());
        eventPublisher.publishEvent(new SuccessfulUserUpdateEvent(event.keyCloakId(), event.sarafrikaCorrelationId()));
    }

    @ApplicationModuleListener
    void onAddUserToOrganisation(AddUserToOrganisationEvent event) {
        keycloakOrganisationService.addUserToOrganization(event.realm(), event.organisationId(), event.userId());
    }

    /**
     * Updates a Keycloak attribute if the value is not null.
     * If value is null, the attribute is left unchanged.
     */
    private void updateAttributeIfNotNull(Map<String, List<String>> attributes, String key, String value) {
        if (value != null) {
            attributes.put(key, List.of(value));
        }
    }
}
