package apps.sarafrika.elimika.tenancy.internal;

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

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapRunnerTest {

    @Mock
    private KeycloakUserService keycloakUserService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserDomainRepository userDomainRepository;

    @Mock
    private UserDomainMappingRepository userDomainMappingRepository;

    @Mock
    private UserNumberService userNumberService;

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
                userNumberService
        );
        ReflectionTestUtils.setField(runner, "keycloakRealm", "elimika");
    }

    @Test
    void disabledBootstrapDoesNothing() {
        runner.run(null);

        verifyNoInteractions(
                keycloakUserService,
                userRepository,
                userDomainRepository,
                userDomainMappingRepository,
                userNumberService
        );
    }

    @Test
    void linksExistingKeycloakUserToLocalAdminAndAssignsDomain() {
        properties.setEnabled(true);
        properties.setEmail(" Admin@Example.COM ");
        properties.setFirstName("Ada");
        properties.setMiddleName("M");
        properties.setLastName("Lovelace");
        properties.setPhoneNumber("+254700000000");

        UserRepresentation keycloakUser = new UserRepresentation();
        keycloakUser.setId("kc-admin");

        UUID userUuid = UUID.randomUUID();
        User user = new User();
        user.setUuid(userUuid);
        user.setEmail("admin@example.com");

        UUID domainUuid = UUID.randomUUID();
        UserDomain adminDomain = new UserDomain();
        adminDomain.setUuid(domainUuid);
        adminDomain.setDomainName("admin");

        when(keycloakUserService.getUserByUsername("admin@example.com", "elimika"))
                .thenReturn(Optional.of(keycloakUser));
        when(userRepository.findByKeycloakId("kc-admin")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userDomainRepository.findByDomainName("admin")).thenReturn(Optional.of(adminDomain));
        when(userDomainMappingRepository.existsByUserUuidAndUserDomainUuid(userUuid, domainUuid))
                .thenReturn(false);

        runner.run(null);

        assertThat(user.getKeycloakId()).isEqualTo("kc-admin");
        assertThat(user.isActive()).isTrue();
        assertThat(user.getEmail()).isEqualTo("admin@example.com");
        assertThat(user.getUsername()).isEqualTo("admin@example.com");

        verify(keycloakUserService).updateUser("kc-admin", keycloakUser, "elimika");
        verify(keycloakUserService, never()).createUser(org.mockito.ArgumentMatchers.any());
        verify(userNumberService, never()).nextUserNo();

        ArgumentCaptor<UserDomainMapping> mappingCaptor = ArgumentCaptor.forClass(UserDomainMapping.class);
        verify(userDomainMappingRepository).save(mappingCaptor.capture());
        assertThat(mappingCaptor.getValue().getUserUuid()).isEqualTo(userUuid);
        assertThat(mappingCaptor.getValue().getUserDomainUuid()).isEqualTo(domainUuid);
    }
}
