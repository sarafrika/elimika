package apps.sarafrika.elimika.tenancy.internal;

import apps.sarafrika.elimika.authentication.spi.KeycloakRegistration;
import apps.sarafrika.elimika.authentication.spi.KeycloakUserService;
import apps.sarafrika.elimika.shared.utils.enums.DomainApprovalStatus;
import apps.sarafrika.elimika.tenancy.config.AdminBootstrapProperties;
import apps.sarafrika.elimika.tenancy.entity.User;
import apps.sarafrika.elimika.tenancy.entity.UserDomain;
import apps.sarafrika.elimika.tenancy.entity.UserDomainMapping;
import apps.sarafrika.elimika.tenancy.repository.UserDomainMappingRepository;
import apps.sarafrika.elimika.tenancy.repository.UserDomainRepository;
import apps.sarafrika.elimika.tenancy.repository.UserRepository;
import apps.sarafrika.elimika.tenancy.services.UserNumberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Gives every new database a default admin who can log in and approve, until a real admin takes
 * over (see {@link BootstrapAdminSuccession}). Once retired it stays off for that database.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final String PASSWORD_ALPHABET =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%*-_";
    private static final int GENERATED_PASSWORD_LENGTH = 20;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AdminBootstrapProperties properties;
    private final KeycloakUserService keycloakUserService;
    private final UserRepository userRepository;
    private final UserDomainRepository userDomainRepository;
    private final UserDomainMappingRepository userDomainMappingRepository;
    private final UserNumberService userNumberService;
    private final BootstrapAdminSuccession succession;
    private final TransactionTemplate transactionTemplate;

    @Value("${app.keycloak.realm}")
    private String keycloakRealm;

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            return;
        }
        try {
            transactionTemplate.executeWithoutResult(status -> bootstrap());
        } catch (RuntimeException e) {
            log.error("Default admin bootstrap failed; it will be retried on the next start", e);
        }
    }

    private void bootstrap() {
        BootstrapAdmin admin = validatedAdmin();
        Optional<User> existing = userRepository.findByEmailIgnoreCase(admin.email());

        if (existing.isPresent() && !existing.get().isActive()) {
            log.info("Default admin {} is retired in this database; leaving it disabled", admin.email());
            return;
        }

        Optional<User> successor = succession.findUsableAdmin();
        if (successor.isPresent()) {
            if (existing.isPresent()) {
                succession.retire(existing.get(), successor.get());
            } else {
                log.info("Default admin skipped; real admins are already present");
            }
            return;
        }

        Optional<UserRepresentation> keycloakUser = keycloakUserService.getUserByUsername(admin.email(), keycloakRealm);
        // A password is issued only when the Keycloak account is created or re-enabled, so a live
        // account (e.g. the local qa-admin) keeps the credentials its owner already knows.
        boolean activating = keycloakUser.isEmpty() || !Boolean.TRUE.equals(keycloakUser.get().isEnabled());

        User user = resolveLocalUser(admin, keycloakUser);
        UserRepresentation representation = syncKeycloakUser(admin, keycloakUser, activating);
        String keycloakId = requiredKeycloakId(representation);

        syncLocalUser(user, admin, keycloakId);
        assignAdminDomain(user);

        if (activating) {
            issueTemporaryPassword(admin.email(), keycloakId);
        }
        succession.markActive();
        log.info("Default admin {} is active until a real admin signs in", admin.email());
    }

    private void issueTemporaryPassword(String email, String keycloakId) {
        String configured = blankToNull(properties.getPassword());
        String password = configured != null ? configured : generatePassword();
        keycloakUserService.setTemporaryPassword(keycloakId, password, keycloakRealm);
        if (configured != null) {
            log.warn("Default admin {} ready; log in with APP_BOOTSTRAP_ADMIN_PASSWORD (must be changed)", email);
        } else {
            log.warn("""

                    ==================== DEFAULT ADMIN ====================
                      Email:              {}
                      Temporary password: {}
                      You will be asked to change it at first login.
                    =======================================================""", email, password);
        }
    }

    static String generatePassword() {
        StringBuilder password = new StringBuilder(GENERATED_PASSWORD_LENGTH);
        for (int i = 0; i < GENERATED_PASSWORD_LENGTH; i++) {
            password.append(PASSWORD_ALPHABET.charAt(RANDOM.nextInt(PASSWORD_ALPHABET.length())));
        }
        return password.toString();
    }

    private BootstrapAdmin validatedAdmin() {
        String email = required(properties.getEmail(), "APP_BOOTSTRAP_ADMIN_EMAIL")
                .toLowerCase(Locale.ROOT);
        String firstName = required(properties.getFirstName(), "APP_BOOTSTRAP_ADMIN_FIRST_NAME");
        String lastName = required(properties.getLastName(), "APP_BOOTSTRAP_ADMIN_LAST_NAME");

        return new BootstrapAdmin(
                email,
                firstName,
                blankToNull(properties.getMiddleName()),
                lastName,
                blankToNull(properties.getPhoneNumber()),
                Optional.ofNullable(blankToNull(properties.getDateOfBirth())).map(LocalDate::parse).orElse(null),
                Optional.ofNullable(blankToNull(properties.getGender()))
                        .map(gender -> gender.toUpperCase(Locale.ROOT)).orElse(null)
        );
    }

    private User resolveLocalUser(BootstrapAdmin admin, Optional<UserRepresentation> keycloakUser) {
        Optional<User> userByKeycloakId = keycloakUser
                .map(UserRepresentation::getId)
                .filter(Objects::nonNull)
                .flatMap(userRepository::findByKeycloakId);
        Optional<User> userByEmail = userRepository.findByEmailIgnoreCase(admin.email());

        if (userByKeycloakId.isPresent() && userByEmail.isPresent()
                && !userByKeycloakId.get().getUuid().equals(userByEmail.get().getUuid())) {
            throw new IllegalStateException("Bootstrap admin email and Keycloak id belong to different local users");
        }

        return userByKeycloakId.or(() -> userByEmail)
                .orElseGet(() -> createLocalUser(admin, keycloakUser.map(UserRepresentation::getId).orElse(null)));
    }

    private User createLocalUser(BootstrapAdmin admin, String keycloakId) {
        User user = new User();
        user.setFirstName(admin.firstName());
        user.setMiddleName(admin.middleName());
        user.setLastName(admin.lastName());
        user.setEmail(admin.email());
        user.setUsername(admin.email());
        user.setUserNo(userNumberService.nextUserNo());
        user.setPhoneNumber(admin.phoneNumber());
        user.setActive(true);
        user.setKeycloakId(keycloakId);
        return userRepository.save(user);
    }

    /** Creates the Keycloak user without emailing it; only (re)activation touches its login state. */
    private UserRepresentation syncKeycloakUser(
            BootstrapAdmin admin,
            Optional<UserRepresentation> existingKeycloakUser,
            boolean activating
    ) {
        UserRepresentation representation = existingKeycloakUser.orElseGet(() -> {
            String id = keycloakUserService.registerUser(new KeycloakRegistration(
                    admin.email(), admin.firstName(), admin.middleName(), admin.lastName(),
                    admin.phoneNumber(), admin.dateOfBirth(), admin.gender()), keycloakRealm);
            return keycloakUserService.getUserById(id, keycloakRealm)
                    .orElseThrow(() -> new IllegalStateException("Keycloak lost the default admin it just created"));
        });
        String keycloakId = requiredKeycloakId(representation);
        representation.setFirstName(admin.firstName());
        representation.setLastName(admin.lastName());
        representation.setEmail(admin.email());
        representation.setUsername(admin.email());
        if (activating) {
            representation.setEnabled(true);
            representation.setEmailVerified(true);
            representation.setRequiredActions(List.of("UPDATE_PASSWORD"));
        }
        keycloakUserService.updateUser(keycloakId, representation, keycloakRealm);
        return representation;
    }

    private void syncLocalUser(User user, BootstrapAdmin admin, String keycloakId) {
        userRepository.findByKeycloakId(keycloakId)
                .filter(existing -> !existing.getUuid().equals(user.getUuid()))
                .ifPresent(existing -> {
                    throw new IllegalStateException("Bootstrap Keycloak id is already linked to another local user");
                });

        user.setFirstName(admin.firstName());
        user.setMiddleName(admin.middleName());
        user.setLastName(admin.lastName());
        user.setEmail(admin.email());
        user.setUsername(admin.email());
        user.setPhoneNumber(admin.phoneNumber());
        user.setActive(true);
        user.setKeycloakId(keycloakId);
        userRepository.save(user);
    }

    private void assignAdminDomain(User user) {
        UserDomain adminDomain = userDomainRepository.findByDomainName(BootstrapAdminSuccession.ADMIN_DOMAIN)
                .orElseThrow(() -> new IllegalStateException("Admin user domain is not configured"));

        if (userDomainMappingRepository.existsByUserUuidAndUserDomainUuid(user.getUuid(), adminDomain.getUuid())) {
            return;
        }

        userDomainMappingRepository.save(
                UserDomainMapping.of(user.getUuid(), adminDomain.getUuid(), DomainApprovalStatus.APPROVED));
    }

    private String required(String value, String propertyName) {
        String normalized = blankToNull(value);
        if (normalized == null) {
            throw new IllegalStateException(propertyName + " is required when admin bootstrap is enabled");
        }
        return normalized;
    }

    private String requiredKeycloakId(UserRepresentation representation) {
        if (representation.getId() == null || representation.getId().isBlank()) {
            throw new IllegalStateException("Keycloak did not return an id for the bootstrap admin");
        }
        return representation.getId();
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private record BootstrapAdmin(
            String email,
            String firstName,
            String middleName,
            String lastName,
            String phoneNumber,
            LocalDate dateOfBirth,
            String gender
    ) {
    }
}
