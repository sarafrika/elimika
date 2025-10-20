package apps.sarafrika.elimika.authentication.services.impl;

import apps.sarafrika.elimika.authentication.spi.KeycloakUserService;
import apps.sarafrika.elimika.shared.event.user.SuccessfulUserCreation;
import apps.sarafrika.elimika.shared.event.user.UserCreationEvent;
import apps.sarafrika.elimika.shared.exceptions.KeycloakException;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class KeycloakUserServiceImpl implements KeycloakUserService {
    private final Keycloak keycloak;

    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.keycloak.user.attributes[0]}")
    private String userDomain;

    private static final List<String> DEFAULT_REQUIRED_ACTIONS = Arrays.asList("VERIFY_EMAIL", "UPDATE_PASSWORD");

    @Override
    @Transactional
    public UserRepresentation createUser(UserCreationEvent newUserRecord) {
        Optional<UserRepresentation> existingUser = getUserByUsername(newUserRecord.username(), newUserRecord.realm());
        if (existingUser.isPresent()) {
            log.info("User already exists with username: {}", newUserRecord.username());
            return existingUser.get();
        }

        try {
            Map<String, List<String>> attributes = new HashMap<>();
            if (newUserRecord.userDomain() != null) {
                attributes.put(userDomain, List.of(newUserRecord.userDomain().name()));
            }
            UserRepresentation userRepresentation = createUserRepresentation(newUserRecord, attributes);
            UsersResource usersResource = getUsersResource(newUserRecord.realm());

            try (Response response = usersResource.create(userRepresentation)) {
                handleResponse(response, newUserRecord.username());
                String userId = extractCreatedUserId(response);
                sendRequiredActionEmail(userId, DEFAULT_REQUIRED_ACTIONS, newUserRecord.realm());
                eventPublisher.publishEvent(new SuccessfulUserCreation(newUserRecord.sarafrikaCorrelationId(), userId));
                return getUserById(userId, newUserRecord.realm())
                        .orElseThrow(() -> new KeycloakException("User created but not found"));
            }
        } catch (Exception e) {
            log.error("Failed to create user for username: {}", newUserRecord.username(), e);
            throw new KeycloakException("User creation failed: " + e.getMessage(), e);
        }
    }


    @Override
    public Optional<UserRepresentation> getUserById(String userId, String realm) {
        log.debug("Attempting to fetch user from Keycloak - userId: {}, realm: {}", userId, realm);
        try {
            UserResource userResource = getUsersResource(realm).get(userId);
            UserRepresentation userRep = userResource.toRepresentation();

            log.info("Successfully retrieved user from Keycloak - userId: {}, username: {}, email: {}",
                    userId, userRep.getUsername(), userRep.getEmail());
            log.debug("Full user representation: {}", userRep.toString());

            return Optional.of(userRep);
        } catch (jakarta.ws.rs.NotFoundException e) {
            log.warn("User not found in Keycloak - userId: {}, realm: {}", userId, realm);
            return Optional.empty();
        } catch (jakarta.ws.rs.ForbiddenException e) {
            log.error("Forbidden: Insufficient permissions to fetch user from Keycloak - userId: {}, realm: {}", userId, realm, e);
            return Optional.empty();
        } catch (jakarta.ws.rs.ProcessingException e) {
            log.error("Connection error: Unable to connect to Keycloak server - userId: {}, realm: {}, error: {}",
                    userId, realm, e.getMessage(), e);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Unexpected error fetching user from Keycloak - userId: {}, realm: {}, error type: {}, message: {}",
                    userId, realm, e.getClass().getName(), e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<UserRepresentation> getUserByUsername(String username, String realm) {
        return getUsersResource(realm)
                .searchByUsername(username, true)
                .stream()
                .findFirst();
    }

    @Override
    @Transactional
    public void updateUser(String userId, UserRepresentation userRepresentation, String realm) {
        try {
            UserResource userResource = getUsersResource(realm).get(userId);
            userResource.update(userRepresentation);
        } catch (Exception e) {
            log.error("Failed to update user", e);
            throw new KeycloakException("User update failed: " + e.getMessage());
        }
    }

    @Override
    public void sendRequiredActionEmail(String userId, List<String> actions, String realm) {
        try {
            getUsersResource(realm).get(userId).executeActionsEmail(actions);
        } catch (Exception e) {
            log.error("Failed to send action email", e);
            throw new KeycloakException("Failed to send email: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void logoutUser(String userId, String realm) {
        try {
            getUsersResource(realm).get(userId).logout();
        } catch (Exception e) {
            log.error("Failed to logout user", e);
            throw new KeycloakException("Logout failed: " + e.getMessage());
        }
    }

    // Private helper methods

    private UsersResource getUsersResource(String realm) {
        return keycloak.realm(realm).users();
    }

    private static UserRepresentation createUserRepresentation(UserCreationEvent userRecord, Map<String, List<String>> attributes) {

        UserRepresentation user = new UserRepresentation();
        user.setEnabled(true);
        user.setFirstName(userRecord.firstName());
        user.setLastName(userRecord.lastName());
        user.setUsername(userRecord.username());
        user.setEmail(userRecord.username());
        user.setEmailVerified(false);
        user.setEnabled(userRecord.active());
        user.setRequiredActions(DEFAULT_REQUIRED_ACTIONS);
        user.setAttributes(attributes);
        return user;
    }

    private String extractCreatedUserId(Response response) {
        String location = response.getHeaderString("Location");
        return location.substring(location.lastIndexOf("/") + 1);
    }

    private void handleResponse(Response response, String username) {
        if (response.getStatus() != Response.Status.CREATED.getStatusCode()) {
            String error = response.readEntity(String.class);
            log.error("Creation failed. Status: {}, Error: {}", response.getStatus(), error);

            throw switch (response.getStatus()) {
                case 409 -> new KeycloakException("User exists: " + username);
                case 400 -> new KeycloakException("Invalid data: " + error);
                case 401 -> new KeycloakException("Unauthorized");
                case 403 -> new KeycloakException("Permission denied");
                default -> new KeycloakException("Creation failed: " + error);
            };
        }
    }
}