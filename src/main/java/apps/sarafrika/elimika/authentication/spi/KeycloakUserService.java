package apps.sarafrika.elimika.authentication.spi;

import apps.sarafrika.elimika.shared.event.user.UserCreationEvent;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.List;
import java.util.Optional;

public interface KeycloakUserService {
    // Core CRUD Operations
    UserRepresentation createUser(UserCreationEvent newUserRecord);
    Optional<UserRepresentation> getUserById(String userId, String realm);
    Optional<UserRepresentation> getUserByUsername(String username, String realm);
    List<UserRepresentation> getAllUsers(String realm);
    void updateUser(String userId, UserRepresentation userRepresentation, String realm);
    void deleteUser(String userId, String realm);

    // User Management Operations
    void enableUser(String userId, String realm);
    void disableUser(String userId, String realm);
    void resetPassword(String userId, String newPassword, String realm);

    // Email Operations
    void sendVerificationEmail(String userId, String realm);
    void sendPasswordResetEmail(String userId, String realm);
    void sendRequiredActionEmail(String userId, List<String> actions, String realm);

    // Role Operations
    List<RoleRepresentation> getUserRoles(String userId, String realm);
    void assignRole(String userId, String roleName, String realm);
    void removeRole(String userId, String roleName, String realm);

    // Group Operations
    List<GroupRepresentation> getUserGroups(String userId, String realm);
    void addUserToGroup(String userId, String groupId, String realm);
    void removeUserFromGroup(String userId, String groupId, String realm);

    // Session Management
    void logoutUser(String userId, String realm);
    void revokeAllSessions(String userId, String realm);
}