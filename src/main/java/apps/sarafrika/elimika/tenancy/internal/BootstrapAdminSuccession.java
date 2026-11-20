package apps.sarafrika.elimika.tenancy.internal;

import apps.sarafrika.elimika.authentication.spi.KeycloakUserService;
import apps.sarafrika.elimika.shared.utils.enums.DomainApprovalStatus;
import apps.sarafrika.elimika.tenancy.config.AdminBootstrapProperties;
import apps.sarafrika.elimika.tenancy.entity.User;
import apps.sarafrika.elimika.tenancy.entity.UserDomain;
import apps.sarafrika.elimika.tenancy.repository.UserDomainMappingRepository;
import apps.sarafrika.elimika.tenancy.repository.UserDomainRepository;
import apps.sarafrika.elimika.tenancy.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Decides when the default admin hands over: once another admin can really log in, the default
 * admin is disabled in Keycloak, logged out and marked inactive (which keeps it retired for this DB).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BootstrapAdminSuccession {

    static final String ADMIN_DOMAIN = "admin";

    private final AdminBootstrapProperties properties;
    private final KeycloakUserService keycloakUserService;
    private final UserRepository userRepository;
    private final UserDomainRepository userDomainRepository;
    private final UserDomainMappingRepository userDomainMappingRepository;

    /** True while the default admin is live in this process; keeps the sign-in hook free otherwise. */
    private final AtomicBoolean active = new AtomicBoolean(false);

    @Value("${app.keycloak.realm}")
    private String keycloakRealm;

    public boolean isActive() {
        return active.get();
    }

    void markActive() {
        active.set(true);
    }

    /** Another approved, active admin whose Keycloak account is enabled with no setup steps pending. */
    @Transactional(readOnly = true)
    public Optional<User> findUsableAdmin() {
        Optional<UserDomain> adminDomain = userDomainRepository.findByDomainName(ADMIN_DOMAIN);
        if (adminDomain.isEmpty()) {
            return Optional.empty();
        }
        return userDomainMappingRepository
                .findByUserDomainUuidAndStatus(adminDomain.get().getUuid(), DomainApprovalStatus.APPROVED)
                .stream()
                .map(mapping -> userRepository.findByUuid(mapping.getUserUuid()))
                .flatMap(Optional::stream)
                .filter(user -> user.isActive() && user.getKeycloakId() != null && !isBootstrap(user))
                .filter(this::hasFinishedKeycloakSetup)
                .findFirst();
    }

    /** Sign-in hook: an approved admin other than the default one authenticating retires it. */
    @Transactional
    public void onAuthenticated(String keycloakUserId) {
        if (!active.get() || keycloakUserId == null) {
            return;
        }
        Optional<User> successor = userRepository.findByKeycloakId(keycloakUserId)
                .filter(user -> user.isActive() && !isBootstrap(user))
                .filter(user -> isApprovedAdmin(user.getUuid()));
        if (successor.isEmpty()) {
            return;
        }
        Optional<User> bootstrap = userRepository.findByEmailIgnoreCase(bootstrapEmail())
                .filter(User::isActive);
        if (bootstrap.isEmpty()) {
            active.set(false);
            return;
        }
        retire(bootstrap.get(), successor.get());
    }

    /** Disables the default admin in Keycloak, ends its sessions and marks it inactive locally. */
    @Transactional
    public void retire(User bootstrap, User successor) {
        if (bootstrap.getKeycloakId() != null) {
            keycloakUserService.getUserById(bootstrap.getKeycloakId(), keycloakRealm).ifPresent(representation -> {
                representation.setEnabled(false);
                keycloakUserService.updateUser(bootstrap.getKeycloakId(), representation, keycloakRealm);
            });
            keycloakUserService.logoutUser(bootstrap.getKeycloakId(), keycloakRealm);
        }
        bootstrap.setActive(false);
        userRepository.save(bootstrap);
        active.set(false);
        log.warn("Default admin {} retired; {} has taken over", bootstrap.getEmail(), successor.getEmail());
    }

    boolean isBootstrap(User user) {
        return user.getEmail() != null && user.getEmail().equalsIgnoreCase(bootstrapEmail());
    }

    String bootstrapEmail() {
        return properties.getEmail() == null ? "" : properties.getEmail().trim().toLowerCase(Locale.ROOT);
    }

    private boolean isApprovedAdmin(UUID userUuid) {
        return userDomainRepository.findByDomainName(ADMIN_DOMAIN)
                .map(domain -> userDomainMappingRepository.existsByUserUuidAndUserDomainUuidAndStatus(
                        userUuid, domain.getUuid(), DomainApprovalStatus.APPROVED))
                .orElse(false);
    }

    private boolean hasFinishedKeycloakSetup(User user) {
        return keycloakUserService.getUserById(user.getKeycloakId(), keycloakRealm)
                .filter(representation -> Boolean.TRUE.equals(representation.isEnabled()))
                .map(UserRepresentation::getRequiredActions)
                .map(actions -> actions == null || actions.isEmpty())
                .orElse(false);
    }
}
