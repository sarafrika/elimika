package apps.sarafrika.elimika.common.security;

import apps.sarafrika.elimika.authentication.services.KeycloakUserService;
import apps.sarafrika.elimika.common.exceptions.RecordNotFoundException;
import apps.sarafrika.elimika.tenancy.repository.UserRepository;
import apps.sarafrika.elimika.tenancy.services.UserService;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserSyncFilter implements Filter {

    private final UserRepository userRepository;
    private final KeycloakUserService keycloakUserService;
    private final UserService userService;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        // Only process authenticated requests
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof JwtAuthenticationToken jwtToken && authentication.isAuthenticated()) {
            String keycloakUserId = jwtToken.getToken().getClaimAsString("sub");

            if (keycloakUserId != null) {
                ensureUserExists(keycloakUserId);
            }
        }

        chain.doFilter(request, response);
    }

    private void ensureUserExists(String keycloakUserId) {
        try {
            boolean userExists = userRepository.existsByKeycloakId(keycloakUserId);

            if (!userExists) {
                log.info("User not found in database, creating user for Keycloak ID: {}", keycloakUserId);

                UserRepresentation userRepresentation = keycloakUserService
                        .getUserById(keycloakUserId, "elimika")
                        .orElseThrow(() -> new RecordNotFoundException("User not found in Keycloak with ID: " + keycloakUserId));

                userService.createUser(userRepresentation);
                log.info("Successfully created user in database for Keycloak ID: {}", keycloakUserId);
            } else {
                log.debug("User already exists in database for Keycloak ID: {}", keycloakUserId);
            }
        } catch (RecordNotFoundException e) {
            log.error("User not found in Keycloak for ID: {}", keycloakUserId, e);
            // Don't throw exception here as it might block the request
            // Consider implementing a retry mechanism or alerting system
        } catch (Exception e) {
            log.error("Failed to ensure user exists for Keycloak ID: {}", keycloakUserId, e);
            // Don't throw exception here to avoid blocking legitimate requests
            // Consider implementing proper error handling based on your requirements
        }
    }
}