package apps.sarafrika.elimika.authentication.services.impl;

import apps.sarafrika.elimika.authentication.services.KeycloakClientService;
import apps.sarafrika.elimika.common.exceptions.KeycloakException;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class KeycloakClientServiceImpl implements KeycloakClientService {
    private final Keycloak keycloak;

    @Override
    public String createClient(String realm, String clientName) {
        String clientId = generateClientId(clientName);
        log.debug("Creating client: {}", clientId);

        ClientRepresentation client = createClientRepresentation(clientId, clientName);

        try (Response response = keycloak.realm(realm).clients().create(client)) {
            if (response.getStatus() != Response.Status.CREATED.getStatusCode()) {
                String error = response.readEntity(String.class);
                throw new KeycloakException("Client creation failed: " + error);
            }

            String id = extractClientId(response);
            ClientResource clientResource = keycloak.realm(realm).clients().get(id);
            clientResource.generateNewSecret();

            log.info("Created client: {}", clientId);
            return clientId;
        } catch (Exception e) {
            log.error("Client creation failed", e);
            throw new KeycloakException("Failed to create client: " + e.getMessage());
        }
    }

    private String generateClientId(String name) {
        return name.toLowerCase().replaceAll("\\s+", "-") +
                "-" +
                UUID.randomUUID().toString().substring(0, 8);
    }

    @Override
    public void deleteClient(String realm, String clientId) {
        log.debug("Deleting client: {}", clientId);
        try {
            String id = findClientId(realm, clientId);
            keycloak.realm(realm).clients().get(id).remove();
            log.info("Deleted client: {}", clientId);
        } catch (Exception e) {
            log.error("Client deletion failed", e);
            throw new KeycloakException("Failed to delete client: " + e.getMessage());
        }
    }

    @Override
    public boolean clientExists(String realm, String clientId) {
        return keycloak.realm(realm).clients().findByClientId(clientId)
                .stream()
                .findFirst()
                .isPresent();
    }

    private String findClientId(String realm, String clientId) {
        return keycloak.realm(realm).clients().findByClientId(clientId)
                .stream()
                .findFirst()
                .map(ClientRepresentation::getId)
                .orElseThrow(() -> new KeycloakException("Client not found: " + clientId));
    }

    private ClientRepresentation createClientRepresentation(String clientId, String clientName) {
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(clientId);
        client.setName(clientName);
        client.setEnabled(true);
        client.setPublicClient(false);
        client.setStandardFlowEnabled(true);
        client.setDirectAccessGrantsEnabled(true);
        client.setServiceAccountsEnabled(true);
        client.setRedirectUris(List.of("/*"));
        client.setWebOrigins(List.of("*"));
        client.setProtocolMappers(createProtocolMappers());

        return client;
    }

    private List<ProtocolMapperRepresentation> createProtocolMappers() {
        return List.of(
                createMapper("username", "preferred_username", "username"),
                createMapper("email", "email", "email"),
                createMapper("firstName", "given_name", "firstName"),
                createMapper("lastName", "family_name", "lastName")
        );
    }

    private ProtocolMapperRepresentation createMapper(String name, String claimName, String userAttribute) {
        ProtocolMapperRepresentation mapper = new ProtocolMapperRepresentation();
        mapper.setName(name);
        mapper.setProtocol("openid-connect");
        mapper.setProtocolMapper("oidc-usermodel-property-mapper");

        Map<String, String> config = new HashMap<>();
        config.put("userinfo.token.claim", "true");
        config.put("user.attribute", userAttribute);
        config.put("id.token.claim", "true");
        config.put("access.token.claim", "true");
        config.put("claim.name", claimName);
        config.put("jsonType.label", "String");
        mapper.setConfig(config);

        return mapper;
    }

    private String extractClientId(Response response) {
        String location = response.getHeaderString("Location");
        return location.substring(location.lastIndexOf("/") + 1);
    }
}