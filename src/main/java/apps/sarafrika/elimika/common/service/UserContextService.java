package apps.sarafrika.elimika.common.service;

import apps.sarafrika.elimika.common.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.tenancy.entity.User;
import apps.sarafrika.elimika.tenancy.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

/**
 * Service for resolving user context from JWT authentication.
 * Handles the conversion from JWT token subject (Keycloak ID) to internal User UUID.
 *
 * @author System
 * @version 1.0
 * @since 2025-08-29
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserContextService {

    private final UserRepository userRepository;

    /**
     * Gets the current authenticated user's UUID from the security context.
     * Extracts the Keycloak ID from the JWT token and resolves it to the internal User UUID.
     *
     * @return the current user's UUID
     * @throws ResourceNotFoundException if no authenticated user or user not found in database
     */
    public UUID getCurrentUserUuid() {
        String keycloakId = getCurrentKeycloakId()
                .orElseThrow(() -> new ResourceNotFoundException("No authenticated user found"));

        return getUserUuidByKeycloakId(keycloakId);
    }

    /**
     * Gets the current authenticated user's UUID, returning empty if not authenticated.
     *
     * @return Optional containing the current user's UUID, or empty if not authenticated
     */
    public Optional<UUID> getCurrentUserUuidOptional() {
        try {
            return Optional.of(getCurrentUserUuid());
        } catch (ResourceNotFoundException e) {
            log.debug("No authenticated user found: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Gets the current authenticated user's Keycloak ID from the JWT token.
     *
     * @return Optional containing the Keycloak ID, or empty if not authenticated
     */
    public Optional<String> getCurrentKeycloakId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || 
            !authentication.isAuthenticated() || 
            authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }

        if (authentication instanceof JwtAuthenticationToken jwtToken) {
            // The subject in JWT token is the Keycloak user ID
            String keycloakId = jwtToken.getToken().getSubject();
            log.debug("Extracted Keycloak ID from JWT: {}", keycloakId);
            return Optional.ofNullable(keycloakId);
        }

        log.warn("Authentication is not a JWT token: {}", authentication.getClass().getSimpleName());
        return Optional.empty();
    }

    /**
     * Resolves a Keycloak ID to the internal User UUID.
     *
     * @param keycloakId the Keycloak user ID
     * @return the internal User UUID
     * @throws ResourceNotFoundException if user not found
     */
    public UUID getUserUuidByKeycloakId(String keycloakId) {
        User user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found for Keycloak ID: " + keycloakId));
        
        return user.getUuid();
    }

    /**
     * Gets the current authenticated user entity.
     *
     * @return the current authenticated User entity
     * @throws ResourceNotFoundException if no authenticated user or user not found
     */
    public User getCurrentUser() {
        String keycloakId = getCurrentKeycloakId()
                .orElseThrow(() -> new ResourceNotFoundException("No authenticated user found"));

        return userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found for Keycloak ID: " + keycloakId));
    }

    /**
     * Checks if there is a currently authenticated user.
     *
     * @return true if there is an authenticated user, false otherwise
     */
    public boolean isAuthenticated() {
        return getCurrentKeycloakId().isPresent();
    }
}