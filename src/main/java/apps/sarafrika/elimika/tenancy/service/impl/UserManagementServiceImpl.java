package apps.sarafrika.elimika.tenancy.service.impl;

import apps.sarafrika.elimika.tenancy.internal.BootstrapAdminSuccession;
import apps.sarafrika.elimika.tenancy.services.UserService;
import apps.sarafrika.elimika.tenancy.services.UserSyncService;
import apps.sarafrika.elimika.tenancy.spi.UserManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;

/**
 * Implementation of User Management Service SPI
 * <p>
 * Provides user creation and synchronization operations for other modules
 * by delegating to internal tenancy services.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2025-10-23
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserManagementServiceImpl implements UserManagementService {

    private final UserService userService;
    private final UserSyncService userSyncService;
    private final BootstrapAdminSuccession bootstrapAdminSuccession;

    @Override
    public void createUser(UserRepresentation userRep) {
        userService.createUser(userRep);
    }

    @Override
    public void ensureUserExists(String keycloakUserId, String realm) {
        userSyncService.ensureUserExists(keycloakUserId, realm);
        if (bootstrapAdminSuccession.isActive()) {
            try {
                bootstrapAdminSuccession.onAuthenticated(keycloakUserId);
            } catch (RuntimeException e) {
                log.error("Could not hand over from the default admin; will retry on next sign-in", e);
            }
        }
    }
}