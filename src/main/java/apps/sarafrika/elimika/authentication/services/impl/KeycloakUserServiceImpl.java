package apps.sarafrika.elimika.authentication.services.impl;

import apps.sarafrika.elimika.authentication.services.KeycloakUserService;
import apps.sarafrika.elimika.common.event.user.SuccessfulUserCreation;
import apps.sarafrika.elimika.common.event.user.UserCreationEvent;
import apps.sarafrika.elimika.common.exceptions.KeycloakException;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

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
                eventPublisher.publishEvent(new SuccessfulUserCreation(newUserRecord.blastWaveId(), userId));
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
        try {
            UserResource userResource = getUsersResource(realm).get(userId);
            return Optional.of(userResource.toRepresentation());
        } catch (Exception e) {
            log.debug("User not found: {}", userId);
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
    @Transactional(readOnly = true)
    public List<UserRepresentation> getAllUsers(String realm) {
        return getUsersResource(realm).list();
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
    @Transactional
    public void deleteUser(String userId, String realm) {
        try {
            getUsersResource(realm).get(userId).remove();
            log.info("Deleted user: {}", userId);
        } catch (Exception e) {
            log.error("User deletion failed", e);
            throw new KeycloakException("Deletion failed", e);
        }
    }

    @Override
    @Transactional
    public void enableUser(String userId, String realm) {
        updateUserEnabled(userId, realm, true);
    }

    @Override
    @Transactional
    public void disableUser(String userId, String realm) {
        updateUserEnabled(userId, realm, false);
    }

    @Override
    @Transactional
    public void resetPassword(String userId, String newPassword, String realm) {
        try {
            CredentialRepresentation credential = createPasswordCredential(newPassword);
            getUsersResource(realm).get(userId).resetPassword(credential);
        } catch (Exception e) {
            log.error("Password reset failed", e);
            throw new KeycloakException("Password reset failed: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void sendVerificationEmail(String userId, String realm) {
        sendRequiredActionEmail(userId, Collections.singletonList("VERIFY_EMAIL"), realm);
    }

    @Override
    @Transactional
    public void sendPasswordResetEmail(String userId, String realm) {
        sendRequiredActionEmail(userId, Collections.singletonList("UPDATE_PASSWORD"), realm);
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
    public List<RoleRepresentation> getUserRoles(String userId, String realm) {
        try {
            return getUsersResource(realm).get(userId).roles().realmLevel().listAll();
        } catch (Exception e) {
            log.error("Failed to get user roles", e);
            throw new KeycloakException("Role fetch failed: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void assignRole(String userId, String roleName, String realm) {
        try {
            RoleRepresentation role = keycloak.realm(realm).roles().get(roleName).toRepresentation();
            getUsersResource(realm).get(userId).roles().realmLevel()
                    .add(Collections.singletonList(role));
        } catch (Exception e) {
            log.error("Failed to assign role", e);
            throw new KeycloakException("Role assignment failed: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void removeRole(String userId, String roleName, String realm) {
        try {
            RoleRepresentation role = keycloak.realm(realm).roles().get(roleName).toRepresentation();
            getUsersResource(realm).get(userId).roles().realmLevel()
                    .remove(Collections.singletonList(role));
        } catch (Exception e) {
            log.error("Failed to remove role", e);
            throw new KeycloakException("Role removal failed: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupRepresentation> getUserGroups(String userId, String realm) {
        try {
            return getUsersResource(realm).get(userId).groups();
        } catch (Exception e) {
            log.error("Failed to get user groups", e);
            throw new KeycloakException("Group fetch failed: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void addUserToGroup(String userId, String groupId, String realm) {
        try {
            getUsersResource(realm).get(userId).joinGroup(groupId);
        } catch (Exception e) {
            log.error("Failed to add user to group", e);
            throw new KeycloakException("Group assignment failed: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void removeUserFromGroup(String userId, String groupId, String realm) {
        try {
            getUsersResource(realm).get(userId).leaveGroup(groupId);
        } catch (Exception e) {
            log.error("Failed to remove user from group", e);
            throw new KeycloakException("Group removal failed: " + e.getMessage());
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

    @Override
    @Transactional
    public void revokeAllSessions(String userId, String realm) {
        try {
            getUsersResource(realm).get(userId).logout();
        } catch (Exception e) {
            log.error("Failed to revoke sessions", e);
            throw new KeycloakException("Session revocation failed: " + e.getMessage());
        }
    }

    // Private helper methods
    private void updateUserEnabled(String userId, String realm, boolean enabled) {
        try {
            UserResource userResource = getUsersResource(realm).get(userId);
            UserRepresentation user = userResource.toRepresentation();
            user.setEnabled(enabled);
            userResource.update(user);
        } catch (Exception e) {
            String action = enabled ? "enable" : "disable";
            log.error("Failed to {} user", action, e);
            throw new KeycloakException("Failed to " + action + " user: " + e.getMessage());
        }
    }

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

    private CredentialRepresentation createPasswordCredential(String password) {
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setTemporary(false);
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        return credential;
    }
}