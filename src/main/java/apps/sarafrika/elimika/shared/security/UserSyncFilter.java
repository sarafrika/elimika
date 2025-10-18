package apps.sarafrika.elimika.shared.security;

import apps.sarafrika.elimika.authentication.spi.KeycloakUserService;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserSyncFilter implements Filter {

    @Value("${app.keycloak.realm}")
    private String realm;

    private final UserRepository userRepository;
    private final KeycloakUserService keycloakUserService;
    private final UserService userService;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;

        // Skip processing for certain paths
        String requestPath = httpRequest.getRequestURI();
        if (shouldSkipProcessing(requestPath)) {
            log.debug("Skipping user sync for path: {}", requestPath);
            chain.doFilter(request, response);
            return;
        }

        // Only process authenticated requests
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        log.debug("Processing request: {}, Authentication: {}", requestPath,
                authentication != null ? authentication.getClass().getSimpleName() : "null");

        if (authentication instanceof JwtAuthenticationToken jwtToken && authentication.isAuthenticated()) {
            String keycloakUserId = jwtToken.getToken().getClaimAsString("sub");
            log.debug("Extracted Keycloak user ID: {}", keycloakUserId);

            if (keycloakUserId != null && !keycloakUserId.trim().isEmpty()) {
                try {
                    ensureUserExists(keycloakUserId);
                } catch (Exception e) {
                    log.error("Critical error in user sync filter for user ID: {}", keycloakUserId, e);
                    // Decide whether to continue or fail the request
                    // For now, we'll continue to avoid blocking legitimate requests
                }
            } else {
                log.warn("No valid Keycloak user ID found in JWT token");
            }
        } else {
            log.debug("No authenticated JWT token found, skipping user sync");
        }

        chain.doFilter(request, response);
    }

    private boolean shouldSkipProcessing(String requestPath) {
        return requestPath.startsWith("/actuator/") ||
                requestPath.startsWith("/health/") ||
                requestPath.startsWith("/swagger-ui/") ||
                requestPath.startsWith("/v3/api-docs") ||
                requestPath.equals("/error");
    }

    @Transactional
    public void ensureUserExists(String keycloakUserId) {
        try {
            log.debug("Checking if user exists for Keycloak ID: {}", keycloakUserId);
            boolean userExists = userRepository.existsByKeycloakId(keycloakUserId);

            if (!userExists) {
                log.info("User not found in database, creating user for Keycloak ID: {} in realm : {}", keycloakUserId, realm);

                UserRepresentation userRepresentation = keycloakUserService
                        .getUserById(keycloakUserId, realm)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found in Keycloak with ID: " + keycloakUserId));

                log.debug("Retrieved user from Keycloak: username={}, email={}",
                        userRepresentation.getUsername(), userRepresentation.getEmail());

                userService.createUser(userRepresentation);
                log.info("Successfully created user in database for Keycloak ID: {}", keycloakUserId);

                // Verify the user was created
                boolean verifyExists = userRepository.existsByKeycloakId(keycloakUserId);
                if (!verifyExists) {
                    log.error("User creation verification failed for Keycloak ID: {}", keycloakUserId);
                    throw new RuntimeException("User was not properly created in database");
                }

            } else {
                log.debug("User already exists in database for Keycloak ID: {}", keycloakUserId);
            }
        } catch (ResourceNotFoundException e) {
            log.error("User not found in Keycloak for ID: {}", keycloakUserId, e);
            throw new RuntimeException("Critical: User exists in JWT but not in Keycloak", e);
        } catch (Exception e) {
            log.error("Failed to ensure user exists for Keycloak ID: {}", keycloakUserId, e);
            throw new RuntimeException("Critical error during user synchronization", e);
        }
    }
}