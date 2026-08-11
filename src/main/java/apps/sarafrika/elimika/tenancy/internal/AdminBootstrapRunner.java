package apps.sarafrika.elimika.tenancy.internal;

import apps.sarafrika.elimika.authentication.spi.KeycloakUserService;
import apps.sarafrika.elimika.shared.event.user.UserCreationEvent;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final String ADMIN_DOMAIN = "admin";

    private final AdminBootstrapProperties properties;
    private final KeycloakUserService keycloakUserService;
    private final UserRepository userRepository;
    private final UserDomainRepository userDomainRepository;
    private final UserDomainMappingRepository userDomainMappingRepository;
    private final UserNumberService userNumberService;

    @Value("${app.keycloak.realm}")
    private String keycloakRealm;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            return;
        }

        BootstrapAdmin admin = validatedAdmin();
        Optional<UserRepresentation> keycloakUser = keycloakUserService.getUserByUsername(admin.email(), keycloakRealm);
        User user = resolveLocalUser(admin, keycloakUser);
        UserRepresentation representation = syncKeycloakUser(admin, user, keycloakUser);
        String keycloakId = requiredKeycloakId(representation);

        syncLocalUser(user, admin, keycloakId);
        assignAdminDomain(user);

        log.info("Platform admin bootstrap completed for {}", admin.email());
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
                blankToNull(properties.getPhoneNumber())
        );
    }

    private User resolveLocalUser(BootstrapAdmin admin, Optional<UserRepresentation> keycloakUser) {
        Optional<User> userByKeycloakId = keycloakUser
                .map(UserRepresentation::getId)
                .filter(Objects::nonNull)
                .flatMap(userRepository::findByKeycloakId);
        Optional<User> userByEmail = userRepository.findByEmail(admin.email());

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

    private UserRepresentation syncKeycloakUser(
            BootstrapAdmin admin,
            User user,
            Optional<UserRepresentation> existingKeycloakUser
    ) {
        if (existingKeycloakUser.isPresent()) {
            UserRepresentation representation = existingKeycloakUser.get();
            String keycloakId = requiredKeycloakId(representation);
            representation.setFirstName(admin.firstName());
            representation.setLastName(admin.lastName());
            representation.setEmail(admin.email());
            representation.setUsername(admin.email());
            representation.setEnabled(true);
            keycloakUserService.updateUser(keycloakId, representation, keycloakRealm);
            return representation;
        }

        return keycloakUserService.createUser(new UserCreationEvent(
                admin.email(),
                admin.firstName(),
                admin.lastName(),
                admin.email(),
                true,
                null,
                keycloakRealm,
                user.getUuid()
        ));
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
        UserDomain adminDomain = userDomainRepository.findByDomainName(ADMIN_DOMAIN)
                .orElseThrow(() -> new IllegalStateException("Admin user domain is not configured"));

        if (userDomainMappingRepository.existsByUserUuidAndUserDomainUuid(user.getUuid(), adminDomain.getUuid())) {
            return;
        }

        UserDomainMapping mapping = new UserDomainMapping();
        mapping.setUserUuid(user.getUuid());
        mapping.setUserDomainUuid(adminDomain.getUuid());
        userDomainMappingRepository.save(mapping);
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
            String phoneNumber
    ) {
    }
}
