package apps.sarafrika.elimika.tenancy.services.impl;

import apps.sarafrika.elimika.authentication.spi.KeycloakUserService;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.tenancy.repository.UserRepository;
import apps.sarafrika.elimika.tenancy.services.UserService;
import apps.sarafrika.elimika.tenancy.services.UserSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of UserSyncService that synchronizes users from Keycloak to local database.
 * Handles the transactional aspects of user synchronization ensuring data consistency.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2025-10-20
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserSyncServiceImpl implements UserSyncService {

    private final UserRepository userRepository;
    private final KeycloakUserService keycloakUserService;
    private final UserService userService;

    @Override
    @Transactional
    public void ensureUserExists(String keycloakUserId, String realm) {
        log.debug("Checking if user exists for Keycloak ID: {}", keycloakUserId);

        if (userRepository.existsByKeycloakId(keycloakUserId)) {
            log.debug("User already exists in database for Keycloak ID: {}", keycloakUserId);
            return;
        }

        log.info("User not found in database, creating user for Keycloak ID: {} in realm: {}", keycloakUserId, realm);

        try {
            UserRepresentation userRepresentation = keycloakUserService
                    .getUserById(keycloakUserId, realm)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "User not found in Keycloak with ID: " + keycloakUserId));

            log.debug("Retrieved user from Keycloak: username={}, email={}",
                    userRepresentation.getUsername(), userRepresentation.getEmail());

            userService.createUser(userRepresentation);
            log.info("Successfully created user in database for Keycloak ID: {}", keycloakUserId);

        } catch (ResourceNotFoundException e) {
            log.error("User not found in Keycloak for ID: {}", keycloakUserId, e);
            throw new IllegalStateException("Critical: User exists in JWT but not in Keycloak", e);
        } catch (Exception e) {
            log.error("Failed to ensure user exists for Keycloak ID: {}", keycloakUserId, e);
            throw new RuntimeException("Critical error during user synchronization", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean userExists(String keycloakUserId) {
        return userRepository.existsByKeycloakId(keycloakUserId);
    }
}
