package apps.sarafrika.elimika.tenancy.internal;

import apps.sarafrika.elimika.authentication.spi.KeycloakUserService;
import apps.sarafrika.elimika.shared.utils.enums.DomainApprovalStatus;
import apps.sarafrika.elimika.tenancy.config.AdminBootstrapProperties;
import apps.sarafrika.elimika.tenancy.entity.User;
import apps.sarafrika.elimika.tenancy.entity.UserDomain;
import apps.sarafrika.elimika.tenancy.entity.UserDomainMapping;
import apps.sarafrika.elimika.tenancy.repository.UserDomainMappingRepository;
import apps.sarafrika.elimika.tenancy.repository.UserDomainRepository;
import apps.sarafrika.elimika.tenancy.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BootstrapAdminSuccessionTest {

    @Mock private KeycloakUserService keycloakUserService;
    @Mock private UserRepository userRepository;
    @Mock private UserDomainRepository userDomainRepository;
    @Mock private UserDomainMappingRepository userDomainMappingRepository;

    private BootstrapAdminSuccession succession;
    private UUID adminDomainUuid;

    @BeforeEach
    void setUp() {
        succession = new BootstrapAdminSuccession(new AdminBootstrapProperties(), keycloakUserService,
                userRepository, userDomainRepository, userDomainMappingRepository);
        ReflectionTestUtils.setField(succession, "keycloakRealm", "elimika");
        adminDomainUuid = UUID.randomUUID();
    }

    @Test
    void anAdminWhoFinishedKeycloakSetupIsUsable() {
        User realAdmin = user("real@example.com", "kc-real");
        approvedAdmins(user("admin@elimika.local", "kc-boot"), realAdmin);
        when(keycloakUserService.getUserById("kc-real", "elimika")).thenReturn(Optional.of(keycloakUser(true, List.of())));

        assertThat(succession.findUsableAdmin()).contains(realAdmin);
        verify(keycloakUserService, never()).getUserById("kc-boot", "elimika");
    }

    @Test
    void anAdminStillSettingUpTheirAccountDoesNotCount() {
        approvedAdmins(user("real@example.com", "kc-real"));
        when(keycloakUserService.getUserById("kc-real", "elimika"))
                .thenReturn(Optional.of(keycloakUser(true, List.of("UPDATE_PASSWORD"))));

        assertThat(succession.findUsableAdmin()).isEmpty();
    }

    @Test
    void signInHookDoesNothingOnceRetired() {
        succession.onAuthenticated("kc-real");

        verifyNoInteractions(userRepository, keycloakUserService);
    }

    @Test
    void anotherAdminSigningInRetiresTheDefaultAdmin() {
        succession.markActive();
        User realAdmin = user("real@example.com", "kc-real");
        User bootstrap = user("admin@elimika.local", "kc-boot");
        UserRepresentation bootstrapKeycloak = keycloakUser(true, List.of());
        adminDomain();
        when(userRepository.findByKeycloakId("kc-real")).thenReturn(Optional.of(realAdmin));
        when(userDomainMappingRepository.existsByUserUuidAndUserDomainUuidAndStatus(
                realAdmin.getUuid(), adminDomainUuid, DomainApprovalStatus.APPROVED)).thenReturn(true);
        when(userRepository.findByEmailIgnoreCase("admin@elimika.local")).thenReturn(Optional.of(bootstrap));
        when(keycloakUserService.getUserById("kc-boot", "elimika")).thenReturn(Optional.of(bootstrapKeycloak));

        succession.onAuthenticated("kc-real");

        assertThat(bootstrapKeycloak.isEnabled()).isFalse();
        verify(keycloakUserService).updateUser("kc-boot", bootstrapKeycloak, "elimika");
        verify(keycloakUserService).logoutUser("kc-boot", "elimika");
        assertThat(bootstrap.isActive()).isFalse();
        verify(userRepository).save(bootstrap);
        assertThat(succession.isActive()).isFalse();
    }

    @Test
    void theDefaultAdminSigningInDoesNotRetireItself() {
        succession.markActive();
        when(userRepository.findByKeycloakId("kc-boot"))
                .thenReturn(Optional.of(user("admin@elimika.local", "kc-boot")));

        succession.onAuthenticated("kc-boot");

        verify(keycloakUserService, never()).logoutUser(anyString(), anyString());
        assertThat(succession.isActive()).isTrue();
    }

    @Test
    void aNonAdminSigningInDoesNotRetireIt() {
        succession.markActive();
        User student = user("student@example.com", "kc-student");
        adminDomain();
        when(userRepository.findByKeycloakId("kc-student")).thenReturn(Optional.of(student));
        when(userDomainMappingRepository.existsByUserUuidAndUserDomainUuidAndStatus(
                student.getUuid(), adminDomainUuid, DomainApprovalStatus.APPROVED)).thenReturn(false);

        succession.onAuthenticated("kc-student");

        verify(userRepository, never()).save(any());
        assertThat(succession.isActive()).isTrue();
    }

    private void approvedAdmins(User... users) {
        adminDomain();
        List<UserDomainMapping> mappings = new java.util.ArrayList<>();
        for (User user : users) {
            when(userRepository.findByUuid(user.getUuid())).thenReturn(Optional.of(user));
            mappings.add(UserDomainMapping.of(user.getUuid(), adminDomainUuid, DomainApprovalStatus.APPROVED));
        }
        when(userDomainMappingRepository.findByUserDomainUuidAndStatus(adminDomainUuid, DomainApprovalStatus.APPROVED))
                .thenReturn(mappings);
    }

    private void adminDomain() {
        UserDomain domain = new UserDomain();
        domain.setUuid(adminDomainUuid);
        domain.setDomainName("admin");
        when(userDomainRepository.findByDomainName("admin")).thenReturn(Optional.of(domain));
    }

    private User user(String email, String keycloakId) {
        User user = new User();
        user.setUuid(UUID.randomUUID());
        user.setEmail(email);
        user.setKeycloakId(keycloakId);
        return user;
    }

    private UserRepresentation keycloakUser(boolean enabled, List<String> requiredActions) {
        UserRepresentation representation = new UserRepresentation();
        representation.setEnabled(enabled);
        representation.setRequiredActions(requiredActions);
        return representation;
    }
}
