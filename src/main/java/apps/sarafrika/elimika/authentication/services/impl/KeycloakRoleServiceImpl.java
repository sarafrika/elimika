package apps.sarafrika.elimika.authentication.services.impl;

import apps.sarafrika.elimika.authentication.services.KeycloakRoleService;
import apps.sarafrika.elimika.common.exceptions.KeycloakException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class KeycloakRoleServiceImpl implements KeycloakRoleService {
    private final Keycloak keycloak;

    @Value("${app.keycloak.admin.clientId}")
    private String clientId;

    @Override
    @Transactional
    public RoleRepresentation createRole(String name, String description, String realm) {
        try {
            RoleRepresentation role = new RoleRepresentation();
            role.setName(name);
            role.setDescription(description);
            role.setClientRole(true);

            ClientResource clientResource = getClientResource(realm);
            if (clientResource == null) {
                throw new KeycloakException("Client not found with ID: " + clientId);
            }

            clientResource.roles().create(role);

            Thread.sleep(100);

            return getRoleByName(name, realm)
                    .orElseThrow(() -> new KeycloakException("Role created but not found"));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KeycloakException("Role creation was interrupted", e);
        } catch (Exception e) {
            log.error("Failed to create role: {}", name, e);
            throw new KeycloakException("Role creation failed: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RoleRepresentation> getRoleByName(String name, String realm) {
        try {
            ClientResource clientResource = getClientResource(realm);
            if (clientResource == null) {
                return Optional.empty();
            }

            RoleRepresentation role = clientResource.roles().get(name).toRepresentation();
            return Optional.ofNullable(role);
        } catch (Exception e) {
            log.debug("Role not found: {}", name, e);
            return Optional.empty();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleRepresentation> getAllRoles(String realm) {
        ClientResource clientResource = getClientResource(realm);
        if (clientResource == null) {
            return Collections.emptyList();
        }
        return clientResource.roles().list();
    }

    @Override
    @Transactional
    public void updateRole(String roleId, RoleRepresentation roleRepresentation, String realm) {
        try {
            ClientResource clientResource = getClientResource(realm);
            if (clientResource == null) {
                throw new KeycloakException("Client not found with ID: " + clientId);
            }

            RoleResource roleResource = clientResource.roles().get(roleId);
            roleResource.update(roleRepresentation);
        } catch (Exception e) {
            log.error("Failed to update role", e);
            throw new KeycloakException("Role update failed: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void deleteRole(String roleId, String realm) {
        try {
            ClientResource clientResource = getClientResource(realm);
            if (clientResource == null) {
                throw new KeycloakException("Client not found with ID: " + clientId);
            }

            clientResource.roles().deleteRole(roleId);
            log.info("Deleted role: {}", roleId);
        } catch (Exception e) {
            log.error("Role deletion failed", e);
            throw new KeycloakException("Deletion failed", e);
        }
    }

    @Override
    @Transactional
    public void assignRoleToUser(String userId, String roleName, String realm) {
        try {
            ClientResource clientResource = getClientResource(realm);
            if (clientResource == null) {
                throw new KeycloakException("Client not found with ID: " + clientId);
            }

            List<RoleRepresentation> roles = getUserRoles(userId, realm);

            keycloak.realm(realm)
                    .users()
                    .get(userId)
                    .roles().clientLevel(clientResource.toRepresentation().getId())
                    .remove(roles);

            RoleRepresentation role = getRoleByName(roleName, realm)
                    .orElseThrow(() -> new KeycloakException("Role not found: " + roleName));


            keycloak.realm(realm)
                    .users()
                    .get(userId)
                    .roles()
                    .clientLevel(clientResource.toRepresentation().getId())
                    .add(Collections.singletonList(role));
        } catch (Exception e) {
            log.error("Failed to assign role to user", e);
            throw new KeycloakException("Role assignment failed: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void removeRoleFromUser(String userId, String roleId, String realm) {
        try {
            ClientResource clientResource = getClientResource(realm);
            if (clientResource == null) {
                throw new KeycloakException("Client not found with ID: " + clientId);
            }

            RoleRepresentation role = getRoleByName(roleId, realm)
                    .orElseThrow(() -> new KeycloakException("Role not found: " + roleId));

            keycloak.realm(realm)
                    .users()
                    .get(userId)
                    .roles()
                    .clientLevel(clientResource.toRepresentation().getId())
                    .remove(Collections.singletonList(role));
        } catch (Exception e) {
            log.error("Failed to remove role from user", e);
            throw new KeycloakException("Role removal failed: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleRepresentation> getUserRoles(String userId, String realm) {
        try {
            ClientResource clientResource = getClientResource(realm);
            if (clientResource == null) {
                throw new KeycloakException("Client not found with ID: " + clientId);
            }

            return keycloak.realm(realm)
                    .users()
                    .get(userId)
                    .roles()
                    .clientLevel(clientResource.toRepresentation().getId())
                    .listAll();
        } catch (Exception e) {
            log.error("Failed to get user roles", e);
            throw new KeycloakException("Failed to retrieve user roles: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void addCompositeRole(String parentRoleId, String childRoleId, String realm) {
        try {
            ClientResource clientResource = getClientResource(realm);
            if (clientResource == null) {
                throw new KeycloakException("Client not found with ID: " + clientId);
            }

            RoleRepresentation childRole = getRoleByName(childRoleId, realm)
                    .orElseThrow(() -> new KeycloakException("Child role not found: " + childRoleId));

            clientResource.roles()
                    .get(parentRoleId)
                    .addComposites(Collections.singletonList(childRole));
        } catch (Exception e) {
            log.error("Failed to add composite role", e);
            throw new KeycloakException("Composite role addition failed: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void removeCompositeRole(String parentRoleId, String childRoleId, String realm) {
        try {
            ClientResource clientResource = getClientResource(realm);
            if (clientResource == null) {
                throw new KeycloakException("Client not found with ID: " + clientId);
            }

            RoleRepresentation childRole = getRoleByName(childRoleId, realm)
                    .orElseThrow(() -> new KeycloakException("Child role not found: " + childRoleId));

            clientResource.roles()
                    .get(parentRoleId)
                    .getRoleComposites()
                    .removeAll(Collections.singletonList(childRole));
        } catch (Exception e) {
            log.error("Failed to remove composite role", e);
            throw new KeycloakException("Composite role removal failed: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Set<RoleRepresentation> getCompositeRoles(String roleId, String realm) {
        try {
            ClientResource clientResource = getClientResource(realm);
            if (clientResource == null) {
                throw new KeycloakException("Client not found with ID: " + clientId);
            }

            return clientResource.roles()
                    .get(roleId)
                    .getRoleComposites();
        } catch (Exception e) {
            log.error("Failed to get composite roles", e);
            throw new KeycloakException("Failed to retrieve composite roles: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleRepresentation> searchRoles(String searchText, String realm) {
        return getAllRoles(realm).stream()
                .filter(role -> role.getName().toLowerCase().contains(searchText.toLowerCase()) ||
                        (role.getDescription() != null &&
                                role.getDescription().toLowerCase().contains(searchText.toLowerCase())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasRole(String userId, String roleName, String realm) {
        try {
            return getUserRoles(userId, realm).stream()
                    .anyMatch(role -> role.getName().equals(roleName));
        } catch (Exception e) {
            log.error("Failed to check user role", e);
            throw new KeycloakException("Role check failed: " + e.getMessage());
        }
    }

    private ClientResource getClientResource(String realm) {
        try {
            ClientsResource clientsResource = keycloak.realm(realm).clients();

            // First try to find by client ID
            List<ClientRepresentation> clients = clientsResource.findByClientId(clientId);
            if (!clients.isEmpty()) {
                String id = clients.get(0).getId();
                return clientsResource.get(id);
            }

            // If not found by client ID, try direct ID lookup
            try {
                return clientsResource.get(clientId);
            } catch (Exception e) {
                log.error("Failed to get client by ID: {}", clientId, e);
                return null;
            }
        } catch (Exception e) {
            log.error("Failed to get client resource", e);
            return null;
        }
    }
}