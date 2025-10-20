package apps.sarafrika.elimika.authentication.services.impl;

import apps.sarafrika.elimika.authentication.services.KeycloakRoleService;
import apps.sarafrika.elimika.shared.exceptions.KeycloakException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

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