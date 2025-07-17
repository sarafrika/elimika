package apps.sarafrika.elimika.common.security;

import apps.sarafrika.elimika.authentication.services.KeycloakUserService;
import apps.sarafrika.elimika.common.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.tenancy.repository.UserRepository;
import apps.sarafrika.elimika.tenancy.services.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final KeycloakUserService keycloakUserService;
    private final UserService userService;

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
            boolean userExists = userRepository.existsByKeycloakId(keycloakUserId);

            if (!userExists) {
                UserRepresentation userRepresentation = keycloakUserService
                        .getUserById(keycloakUserId, "elimika")
                        .orElseThrow(() -> new ResourceNotFoundException("User not found in Keycloak"));

                userService.createUser(userRepresentation);
            }
        } catch (Exception e) {
            log.error("Failed to ensure user exists for Keycloak ID: {}", keycloakUserId, e);
        }
    }
}
