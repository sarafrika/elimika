package apps.sarafrika.elimika.tenancy.services.impl;

import apps.sarafrika.elimika.authentication.spi.KeycloakRegistration;
import apps.sarafrika.elimika.authentication.spi.KeycloakUserService;
import apps.sarafrika.elimika.notifications.preferences.spi.NotificationPreferencesService;
import apps.sarafrika.elimika.shared.event.user.RegistrationActionsEmailRequestedEvent;
import apps.sarafrika.elimika.shared.event.user.UserDomainMappingEvent;
import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import apps.sarafrika.elimika.systemconfig.dto.AgeGateDecision;
import apps.sarafrika.elimika.systemconfig.service.RuleEvaluationService;
import apps.sarafrika.elimika.tenancy.config.RegistrationProperties;
import apps.sarafrika.elimika.tenancy.dto.RegistrationRequestDTO;
import apps.sarafrika.elimika.tenancy.entity.AccountRegistration;
import apps.sarafrika.elimika.tenancy.entity.User;
import apps.sarafrika.elimika.tenancy.internal.CaptchaVerifier;
import apps.sarafrika.elimika.tenancy.repository.AccountRegistrationRepository;
import apps.sarafrika.elimika.tenancy.repository.UserRepository;
import apps.sarafrika.elimika.tenancy.services.DomainApprovalService;
import apps.sarafrika.elimika.tenancy.services.UserNumberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RegistrationServiceImplTest {

    private static final String REALM = "elimika";

    @Mock
    private KeycloakUserService keycloakUserService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AccountRegistrationRepository registrationRepository;
    @Mock
    private UserNumberService userNumberService;
    @Mock
    private DomainApprovalService domainApprovalService;
    @Mock
    private NotificationPreferencesService notificationPreferencesService;
    @Mock
    private RuleEvaluationService ruleEvaluationService;
    @Mock
    private CaptchaVerifier captchaVerifier;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private PlatformTransactionManager transactionManager;

    private RegistrationServiceImpl service;
    private final UUID userUuid = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new RegistrationServiceImpl(keycloakUserService, userRepository, registrationRepository,
                userNumberService, domainApprovalService, notificationPreferencesService, ruleEvaluationService,
                captchaVerifier, new RegistrationProperties(), eventPublisher, transactionManager, REALM);
        when(captchaVerifier.verify(any(), any())).thenReturn(true);
        when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(keycloakUserService.getUserByUsername(anyString(), eq(REALM))).thenReturn(Optional.empty());
        when(keycloakUserService.registerUser(any(), eq(REALM))).thenReturn("kc-1");
        when(userNumberService.nextUserNo()).thenReturn("U-1");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setUuid(userUuid);
            return user;
        });
        when(ruleEvaluationService.evaluateAgeGate(any(), any())).thenReturn(AgeGateDecision.allow());
    }

    @Test
    void registrationSendsIdentityToKeycloakAndRecordsAPendingDomain() {
        service.register(request("course_creator"), "10.0.0.1");

        ArgumentCaptor<KeycloakRegistration> identity = ArgumentCaptor.forClass(KeycloakRegistration.class);
        verify(keycloakUserService).registerUser(identity.capture(), eq(REALM));
        assertThat(identity.getValue().email()).isEqualTo("amina@example.com");
        assertThat(identity.getValue().phoneNumber()).isEqualTo("+254700000000");

        ArgumentCaptor<AccountRegistration> registration = ArgumentCaptor.forClass(AccountRegistration.class);
        verify(registrationRepository).save(registration.capture());
        assertThat(registration.getValue().getRequestedDomain()).isEqualTo("course_creator");
        verify(domainApprovalService).request(userUuid, UserDomain.course_creator);
        verify(eventPublisher).publishEvent(new UserDomainMappingEvent(userUuid, "course_creator"));
        verify(eventPublisher).publishEvent(any(RegistrationActionsEmailRequestedEvent.class));
    }

    @Test
    void anExistingAccountIsSkippedWithoutTouchingKeycloak() {
        when(keycloakUserService.getUserByUsername("amina@example.com", REALM))
                .thenReturn(Optional.of(new UserRepresentation()));

        service.register(request("student"), null);

        verify(keycloakUserService, never()).registerUser(any(), any());
        verify(domainApprovalService, never()).request(any(), any());
    }

    @Test
    void theKeycloakAccountIsRemovedWhenTheElimikaSideFails() {
        when(registrationRepository.save(any())).thenThrow(new IllegalStateException("database down"));

        assertThatThrownBy(() -> service.register(request("student"), null))
                .isInstanceOf(IllegalStateException.class);

        verify(keycloakUserService).deleteUser("kc-1", REALM);
    }

    @Test
    void adminAndParentAreNeverSelfRegisterable() {
        assertThatThrownBy(() -> service.register(request("admin"), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be self-registered");
        assertThatThrownBy(() -> service.register(request("parent"), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be self-registered");
        verify(keycloakUserService, never()).registerUser(any(), any());
    }

    @Test
    void aFailedCaptchaStopsRegistration() {
        when(captchaVerifier.verify(any(), any())).thenReturn(false);

        assertThatThrownBy(() -> service.register(request("student"), null))
                .isInstanceOf(IllegalArgumentException.class);
        verify(keycloakUserService, never()).registerUser(any(), any());
    }

    @Test
    void resendOnlyEmailsWhileActionsAreOutstanding() {
        User user = new User();
        user.setKeycloakId("kc-1");
        when(userRepository.findByEmailIgnoreCase("amina@example.com")).thenReturn(Optional.of(user));
        UserRepresentation done = new UserRepresentation();
        done.setRequiredActions(List.of());
        when(keycloakUserService.getUserById("kc-1", REALM)).thenReturn(Optional.of(done));

        service.resendActionsEmail("Amina@Example.com");
        verify(eventPublisher, never()).publishEvent(any(RegistrationActionsEmailRequestedEvent.class));

        UserRepresentation pending = new UserRepresentation();
        pending.setRequiredActions(List.of("UPDATE_PASSWORD"));
        when(keycloakUserService.getUserById("kc-1", REALM)).thenReturn(Optional.of(pending));

        service.resendActionsEmail("amina@example.com");
        verify(eventPublisher).publishEvent(any(RegistrationActionsEmailRequestedEvent.class));
    }

    private static RegistrationRequestDTO request(String domain) {
        return new RegistrationRequestDTO("Amina", null, "Otieno", " Amina@Example.com ", "+254700000000",
                LocalDate.of(1995, 4, 2), "FEMALE", domain, true, null);
    }
}
