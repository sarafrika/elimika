package apps.sarafrika.elimika.shared.security;

import apps.sarafrika.elimika.tenancy.services.UserSyncService;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Servlet filter that ensures authenticated users from Keycloak exist in the local database.
 * This filter intercepts authenticated requests and synchronizes user data from Keycloak
 * to the local database if the user doesn't exist locally.
 *
 * <p>The filter skips processing for certain paths like actuator endpoints, health checks,
 * API documentation, and error pages to avoid unnecessary overhead.</p>
 *
 * <p>Spring Modulith Compliance: This filter uses UserSyncService from the tenancy module
 * instead of directly accessing repositories, maintaining proper module boundaries.</p>
 *
 * @author Wilfred Njuguna
 * @version 2.0
 * @since 2025-10-20
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserSyncFilter implements Filter {

    @Value("${app.keycloak.realm}")
    private String realm;

    private final UserSyncService userSyncService;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String requestPath = httpRequest.getRequestURI();

        // Skip processing for certain paths
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
                    userSyncService.ensureUserExists(keycloakUserId, realm);
                } catch (Exception e) {
                    log.error("Critical error in user sync filter for user ID: {}", keycloakUserId, e);
                    // Continue to avoid blocking legitimate requests
                    // The service layer handles transaction rollback on errors
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
}