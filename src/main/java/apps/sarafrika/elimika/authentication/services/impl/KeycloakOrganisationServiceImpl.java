package apps.sarafrika.elimika.authentication.services.impl;

import apps.sarafrika.elimika.authentication.services.KeycloakOrganisationService;
import apps.sarafrika.elimika.common.event.organisation.OrganisationCreationEvent;
import apps.sarafrika.elimika.common.event.organisation.SuccessfulOrganisationCreationEvent;
import apps.sarafrika.elimika.common.exceptions.KeycloakException;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.OrganizationDomainRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakOrganisationServiceImpl implements KeycloakOrganisationService {
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000;

    private final Keycloak keycloak;
    private final ApplicationEventPublisher eventPublisher;
    @Override
    public String createOrganization(String realm, String name, String displayName, String description ,String domain) {

        log.debug("Checking if organization already exists: realm={}, name={}", realm, displayName);
        if (isOrganizationExists(realm, displayName)) {
            throw new KeycloakException("Organization with name '" + name + "' already exists in realm '" + realm + "'");
        }

        OrganizationRepresentation org = new OrganizationRepresentation();
        org.setName(name);
        org.setAlias(displayName);
        org.setDescription(description);
        org.addDomain(new OrganizationDomainRepresentation(domain));

        return retryOperation(() -> {
            try (Response response = keycloak.realm(realm).organizations().create(org)) {
                validateResponse(response);
                String id = extractId(response);
                log.info("Successfully created organization: id={}, name={}", id, displayName);
                return id;
            }
        }, "create organization");
    }

    @Override
    public void deleteOrganization(String realm, String orgId) {
        log.debug("Deleting organization: realm={}, orgId={}", realm, orgId);
        retryOperation(() -> {
            keycloak.realm(realm).organizations().get(orgId).delete();
            log.info("Successfully deleted organization: orgId={}", orgId);
            return null;
        }, "delete organization");
    }

    @Override
    public void addUserToOrganization(String realm, String orgId, String userId) {
        log.debug("Adding user to organization: realm={}, orgId={}, userId={}", realm, orgId, userId);
        retryOperation(() -> {
            keycloak.realm(realm).organizations().get(orgId).members().addMember(userId).close();
            log.info("Successfully added user to organization: userId={}, orgId={}", userId, orgId);
            return null;
        }, "add user to organization");
    }

    @Override
    public void removeUserFromOrganization(String realm, String orgId, String userId) {
        log.debug("Removing user from organization: realm={}, orgId={}, userId={}", realm, orgId, userId);
        retryOperation(() -> {
            keycloak.realm(realm).organizations().get(orgId).members().member(userId).delete();
            log.info("Successfully removed user from organization: userId={}, orgId={}", userId, orgId);
            return null;
        }, "remove user from organization");
    }

    @Override
    public Map<String, String> getOrganizationMetadata(String realm, String orgId) {
        log.debug("Fetching organization metadata: realm={}, orgId={}", realm, orgId);
        return retryOperation(() -> {
            OrganizationRepresentation org = keycloak.realm(realm)
                    .organizations()
                    .get(orgId)
                    .toRepresentation();

            Map<String, String> metadata = new HashMap<>();
            metadata.put("name", org.getName());
            metadata.put("displayName", org.getAlias());

            if (org.getAttributes() != null) {
                org.getAttributes().forEach((key, value) -> {
                    if (value != null && !value.isEmpty()) {
                        metadata.put(key, value.getFirst());
                    }
                });
            }

            return metadata;
        }, "get organization metadata");
    }

    @Override
    public List<OrganizationRepresentation> getAllOrganizations(String realm) {
        try{
            return keycloak.realm(realm).organizations().getAll();
        } catch (Exception e) {
            log.error("Failed to get all organizations: realm={}", realm, e);
            throw new RuntimeException(e);
        }
    }


    @ApplicationModuleListener
    void onOrganisationCreation(OrganisationCreationEvent event) {
        log.debug("Processing organization creation event: name={}, blastWaveId={}", event.name(), event.blastWaveId());
        try {
            String organisationId = createOrganization(event.realm(), event.name(), event.slug(), event.description(), event.domain());
            eventPublisher.publishEvent(new SuccessfulOrganisationCreationEvent(event.blastWaveId(), organisationId));
            log.info("Successfully processed organization creation event: orgId={}", organisationId);
        } catch (Exception e) {
            log.error("Failed to process organization creation event: name={}", event.name(), e);
            throw new KeycloakException("Failed to process organization creation event", e);
        }
    }

    private boolean isOrganizationExists(String realm, String name) {
        List<OrganizationRepresentation> existingOrgs = keycloak.realm(realm)
                .organizations().getAll();
        return existingOrgs.stream()
                .anyMatch(org -> org.getName().equalsIgnoreCase(name));
    }

    private void validateResponse(Response response) {
        int statusCode = response.getStatus();
        if (statusCode != Response.Status.CREATED.getStatusCode()) {
            String error = response.readEntity(String.class);
            log.error("Failed to {}: status={}, error={}", "create organization", statusCode, error);
            throw new KeycloakException("Failed to " + "create organization" + ": " + error);
        }
    }

    private String extractId(Response response) {
        String location = response.getHeaderString("Location");
        if (location == null) {
            throw new KeycloakException("No Location header in response");
        }
        return location.substring(location.lastIndexOf("/") + 1);
    }

    private <T> T retryOperation(RetryableOperation<T> operation, String operationName) {
        Exception lastException = null;
        long backoff = RETRY_DELAY_MS;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                return operation.execute();
            } catch (Exception e) {
                lastException = e;
                log.warn("Attempt {} failed for operation '{}': {}", attempt, operationName, e.getMessage());
                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(backoff + (long) (Math.random() * 200));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new KeycloakException("Operation interrupted", ie);
                    }
                    backoff *= 2;
                }
            }
        }

        throw new KeycloakException("Operation '" + operationName + "' failed after " + MAX_RETRIES + " attempts", lastException);
    }

    @FunctionalInterface
    private interface RetryableOperation<T> {
        T execute() throws Exception;
    }
}
