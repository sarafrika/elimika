package apps.sarafrika.elimika.shared.security;

import apps.sarafrika.elimika.authentication.spi.KeycloakUserService;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import apps.sarafrika.elimika.tenancy.spi.UserManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationSuccessHandler {

    @Value("${app.keycloak.realm}")
    private String realm;

    private final UserLookupService userLookupService;
    private final KeycloakUserService keycloakUserService;
    private final UserManagementService userManagementService;

    @EventListener
    public void handleAuthenticationSuccess(AuthenticationSuccessEvent event) {
        if (event.getAuthentication() instanceof JwtAuthenticationToken jwtToken) {
            String keycloakUserId = jwtToken.getToken().getClaimAsString("sub");

            if (keycloakUserId != null) {
                ensureUserExists(keycloakUserId);
            }
        }
    }

    private void ensureUserExists(String keycloakUserId) {
        try {
            boolean userExists = userLookupService.existsByKeycloakId(keycloakUserId);

            if (!userExists) {
                UserRepresentation userRepresentation = keycloakUserService
                        .getUserById(keycloakUserId, realm)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found in Keycloak"));

                userManagementService.createUser(userRepresentation);
            }
        } catch (Exception e) {
            log.error("Failed to ensure user exists for Keycloak ID: {}", keycloakUserId, e);
        }
    }
}
