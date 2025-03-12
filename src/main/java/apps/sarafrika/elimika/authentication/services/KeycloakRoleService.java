package apps.sarafrika.elimika.authentication.services;

import org.keycloak.representations.idm.RoleRepresentation;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface KeycloakRoleService {
    RoleRepresentation createRole(String name, String description, String realm);
    Optional<RoleRepresentation> getRoleByName(String name, String realm);
    List<RoleRepresentation> getAllRoles(String realm);
    void updateRole(String roleId, RoleRepresentation roleRepresentation, String realm);
    void deleteRole(String roleId, String realm);

    void assignRoleToUser(String userId, String roleName, String realm);
    void removeRoleFromUser(String userId, String roleId, String realm);
    List<RoleRepresentation> getUserRoles(String userId, String realm);

    void addCompositeRole(String parentRoleId, String childRoleId, String realm);
    void removeCompositeRole(String parentRoleId, String childRoleId, String realm);
    Set<RoleRepresentation> getCompositeRoles(String roleId, String realm);

    List<RoleRepresentation> searchRoles(String searchText, String realm);
    boolean hasRole(String userId, String roleName, String realm);
}
