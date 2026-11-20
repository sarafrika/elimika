package apps.sarafrika.elimika.tenancy.internal;

import apps.sarafrika.elimika.authentication.spi.KeycloakRegistration;
import apps.sarafrika.elimika.authentication.spi.KeycloakUserService;
import apps.sarafrika.elimika.tenancy.config.AdminBootstrapProperties;
import apps.sarafrika.elimika.tenancy.entity.User;
import apps.sarafrika.elimika.tenancy.entity.UserDomain;
import apps.sarafrika.elimika.tenancy.entity.UserDomainMapping;
import apps.sarafrika.elimika.tenancy.repository.UserDomainMappingRepository;
import apps.sarafrika.elimika.tenancy.repository.UserDomainRepository;
import apps.sarafrika.elimika.tenancy.repository.UserRepository;
import apps.sarafrika.elimika.tenancy.services.UserNumberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapRunnerTest {

    private static final String EMAIL = "admin@elimika.local";

    @Mock private KeycloakUserService keycloakUserService;
    @Mock private UserRepository userRepository;
    @Mock private UserDomainRepository userDomainRepository;
    @Mock private UserDomainMappingRepository userDomainMappingRepository;
    @Mock private UserNumberService userNumberService;
    @Mock private BootstrapAdminSuccession succession;

    private AdminBootstrapProperties properties;
    private AdminBootstrapRunner runner;

    @BeforeEach
    void setUp() {
        properties = new AdminBootstrapProperties();
        runner = new AdminBootstrapRunner(
                properties,
                keycloakUserService,
                userRepository,
                userDomainRepository,
                userDomainMappingRepository,
                userNumberService,
                succession,
                new TransactionTemplate(mock(PlatformTransactionManager.class))
        );
        ReflectionTestUtils.setField(runner, "keycloakRealm", "elimika");
    }

    @Test
    void disabledBootstrapDoesNothing() {
        properties.setEnabled(false);

        runner.run(null);

        verifyNoInteractions(keycloakUserService, userRepository, userDomainRepository,
                userDomainMappingRepository, userNumberService, succession);
    }

    @Test
    void newDatabaseGetsADefaultAdminThatCanLogInAndApprove() {
        properties.setPassword("Configured#123");
        UserRepresentation created = keycloakUser("kc-new", false);
        UUID domainUuid = adminDomain();

        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());
        when(succession.findUsableAdmin()).thenReturn(Optional.empty());
        when(keycloakUserService.getUserByUsername(EMAIL, "elimika")).thenReturn(Optional.empty());
        when(keycloakUserService.registerUser(any(KeycloakRegistration.class), eq("elimika"))).thenReturn("kc-new");
        when(keycloakUserService.getUserById("kc-new", "elimika")).thenReturn(Optional.of(created));
        when(userNumberService.nextUserNo()).thenReturn("USR-1");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findByKeycloakId("kc-new")).thenReturn(Optional.empty());
        when(userDomainMappingRepository.existsByUserUuidAndUserDomainUuid(any(), eq(domainUuid))).thenReturn(false);

        runner.run(null);

        assertThat(created.isEnabled()).isTrue();
        assertThat(created.isEmailVerified()).isTrue();
        assertThat(created.getRequiredActions()).containsExactly("UPDATE_PASSWORD");
        verify(keycloakUserService).updateUser("kc-new", created, "elimika");
        verify(keycloakUserService).setTemporaryPassword("kc-new", "Configured#123", "elimika");
        verify(keycloakUserService, never()).createUser(any());

        ArgumentCaptor<UserDomainMapping> mapping = ArgumentCaptor.forClass(UserDomainMapping.class);
        verify(userDomainMappingRepository).save(mapping.capture());
        assertThat(mapping.getValue().getUserDomainUuid()).isEqualTo(domainUuid);
        verify(succession).markActive();
    }

    @Test
    void generatesATemporaryPasswordWhenNoneIsConfigured() {
        User user = localUser(true);
        UserRepresentation disabled = keycloakUser("kc-admin", false);
        adminDomain();

        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(succession.findUsableAdmin()).thenReturn(Optional.empty());
        when(keycloakUserService.getUserByUsername(EMAIL, "elimika")).thenReturn(Optional.of(disabled));
        when(userRepository.findByKeycloakId("kc-admin")).thenReturn(Optional.of(user));
        when(userDomainMappingRepository.existsByUserUuidAndUserDomainUuid(eq(user.getUuid()), any())).thenReturn(true);

        runner.run(null);

        ArgumentCaptor<String> password = ArgumentCaptor.forClass(String.class);
        verify(keycloakUserService).setTemporaryPassword(eq("kc-admin"), password.capture(), eq("elimika"));
        assertThat(password.getValue()).hasSize(20);
        assertThat(disabled.isEnabled()).isTrue();
    }

    @Test
    void anEnabledKeycloakAccountKeepsItsPasswordEvenOnANewDatabase() {
        UserRepresentation enabled = keycloakUser("kc-admin", true);
        adminDomain();

        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());
        when(succession.findUsableAdmin()).thenReturn(Optional.empty());
        when(keycloakUserService.getUserByUsername(EMAIL, "elimika")).thenReturn(Optional.of(enabled));
        when(userRepository.findByKeycloakId("kc-admin")).thenReturn(Optional.empty());
        when(userNumberService.nextUserNo()).thenReturn("USR-1");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        runner.run(null);

        verify(keycloakUserService, never()).setTemporaryPassword(anyString(), anyString(), anyString());
        assertThat(enabled.getRequiredActions()).containsExactly("VERIFY_EMAIL", "UPDATE_PASSWORD");
        verify(succession).markActive();
    }

    @Test
    void aRetiredDefaultAdminStaysRetired() {
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(localUser(false)));

        runner.run(null);

        verifyNoInteractions(keycloakUserService);
        verify(succession, never()).findUsableAdmin();
        verify(succession, never()).markActive();
    }

    @Test
    void retiresTheDefaultAdminAtStartupWhenARealAdminIsReady() {
        User bootstrap = localUser(true);
        User successor = new User();
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(bootstrap));
        when(succession.findUsableAdmin()).thenReturn(Optional.of(successor));

        runner.run(null);

        verify(succession).retire(bootstrap, successor);
        verify(keycloakUserService, never()).getUserByUsername(anyString(), anyString());
    }

    @Test
    void existingInstallationsWithRealAdminsGetNoDefaultAdmin() {
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());
        when(succession.findUsableAdmin()).thenReturn(Optional.of(new User()));

        runner.run(null);

        verifyNoInteractions(keycloakUserService, userNumberService);
        verify(succession, never()).retire(any(), any());
    }

    @Test
    void anUnreachableKeycloakDoesNotStopTheApplication() {
        when(userRepository.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());
        when(succession.findUsableAdmin()).thenReturn(Optional.empty());
        when(keycloakUserService.getUserByUsername(EMAIL, "elimika")).thenThrow(new IllegalStateException("down"));

        assertThatCode(() -> runner.run(null)).doesNotThrowAnyException();
        verify(succession, never()).markActive();
    }

    private User localUser(boolean active) {
        User user = new User();
        user.setUuid(UUID.randomUUID());
        user.setEmail(EMAIL);
        user.setActive(active);
        return user;
    }

    private UserRepresentation keycloakUser(String id, boolean enabled) {
        UserRepresentation representation = new UserRepresentation();
        representation.setId(id);
        representation.setEnabled(enabled);
        representation.setRequiredActions(List.of("VERIFY_EMAIL", "UPDATE_PASSWORD"));
        return representation;
    }

    private UUID adminDomain() {
        UserDomain domain = new UserDomain();
        domain.setUuid(UUID.randomUUID());
        domain.setDomainName("admin");
        when(userDomainRepository.findByDomainName("admin")).thenReturn(Optional.of(domain));
        return domain.getUuid();
    }
}
