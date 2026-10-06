package apps.sarafrika.elimika.tenancy.services.impl;

import apps.sarafrika.elimika.authentication.spi.KeycloakRegistration;
import apps.sarafrika.elimika.authentication.spi.KeycloakUserService;
import apps.sarafrika.elimika.notifications.preferences.spi.NotificationPreferencesService;
import apps.sarafrika.elimika.shared.enums.Gender;
import apps.sarafrika.elimika.shared.event.user.RegistrationActionsEmailRequestedEvent;
import apps.sarafrika.elimika.shared.event.user.UserDomainMappingEvent;
import apps.sarafrika.elimika.shared.exceptions.KeycloakException;
import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import apps.sarafrika.elimika.systemconfig.dto.AgeGateDecision;
import apps.sarafrika.elimika.systemconfig.dto.RuleContext;
import apps.sarafrika.elimika.systemconfig.service.RuleEvaluationService;
import apps.sarafrika.elimika.tenancy.config.RegistrationProperties;
import apps.sarafrika.elimika.tenancy.dto.DomainApplicationDTO;
import apps.sarafrika.elimika.tenancy.dto.RegistrationRequestDTO;
import apps.sarafrika.elimika.tenancy.entity.AccountRegistration;
import apps.sarafrika.elimika.tenancy.entity.User;
import apps.sarafrika.elimika.tenancy.internal.CaptchaVerifier;
import apps.sarafrika.elimika.tenancy.repository.AccountRegistrationRepository;
import apps.sarafrika.elimika.tenancy.repository.UserRepository;
import apps.sarafrika.elimika.tenancy.services.DomainApprovalService;
import apps.sarafrika.elimika.tenancy.services.RegistrationService;
import apps.sarafrika.elimika.tenancy.services.UserNumberService;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class RegistrationServiceImpl implements RegistrationService {

    private static final List<String> ACCOUNT_ACTIONS = List.of("VERIFY_EMAIL", "UPDATE_PASSWORD");
    private static final String STUDENT_AGE_GATE_RULE_KEY = "student.onboarding.age_gate";

    private final KeycloakUserService keycloakUserService;
    private final UserRepository userRepository;
    private final AccountRegistrationRepository registrationRepository;
    private final UserNumberService userNumberService;
    private final DomainApprovalService domainApprovalService;
    private final NotificationPreferencesService notificationPreferencesService;
    private final RuleEvaluationService ruleEvaluationService;
    private final CaptchaVerifier captchaVerifier;
    private final RegistrationProperties properties;
    private final ApplicationEventPublisher eventPublisher;
    private final TransactionTemplate transaction;
    private final String realm;

    public RegistrationServiceImpl(KeycloakUserService keycloakUserService,
                                   UserRepository userRepository,
                                   AccountRegistrationRepository registrationRepository,
                                   UserNumberService userNumberService,
                                   DomainApprovalService domainApprovalService,
                                   NotificationPreferencesService notificationPreferencesService,
                                   RuleEvaluationService ruleEvaluationService,
                                   CaptchaVerifier captchaVerifier,
                                   RegistrationProperties properties,
                                   ApplicationEventPublisher eventPublisher,
                                   PlatformTransactionManager transactionManager,
                                   @Value("${app.keycloak.realm}") String realm) {
        this.keycloakUserService = keycloakUserService;
        this.userRepository = userRepository;
        this.registrationRepository = registrationRepository;
        this.userNumberService = userNumberService;
        this.domainApprovalService = domainApprovalService;
        this.notificationPreferencesService = notificationPreferencesService;
        this.ruleEvaluationService = ruleEvaluationService;
        this.captchaVerifier = captchaVerifier;
        this.properties = properties;
        this.eventPublisher = eventPublisher;
        this.transaction = new TransactionTemplate(transactionManager);
        this.realm = realm;
    }

    @Override
    public void register(RegistrationRequestDTO request, String clientIp) {
        if (!captchaVerifier.verify(request.captchaToken(), clientIp)) {
            throw new IllegalArgumentException("Captcha verification failed");
        }
        UserDomain domain = selfRegisterableDomain(request.domain());
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        enforceStudentAgeGate(domain, request);

        if (userRepository.findByEmailIgnoreCase(email).isPresent()
                || keycloakUserService.getUserByUsername(email, realm).isPresent()) {
            // An existing Sarafrika account signs in and applies for the domain instead.
            log.info("Registration skipped: an account already exists for the submitted email");
            return;
        }

        String keycloakId;
        try {
            keycloakId = keycloakUserService.registerUser(toKeycloak(request, email), realm);
        } catch (KeycloakException e) {
            if (e.getMessage() != null && e.getMessage().startsWith("User exists")) {
                log.info("Registration skipped: the account was created concurrently");
                return;
            }
            throw e;
        }

        try {
            transaction.executeWithoutResult(status -> recordRegistration(request, email, domain, keycloakId));
        } catch (RuntimeException e) {
            // Undo the Keycloak account so a retry can register the same email cleanly.
            log.error("Registration failed after the Keycloak account was created; removing it", e);
            try {
                keycloakUserService.deleteUser(keycloakId, realm);
            } catch (RuntimeException cleanup) {
                log.error("Could not remove Keycloak user {} after a failed registration", keycloakId, cleanup);
            }
            throw e;
        }
    }

    @Override
    @Transactional
    public void resendActionsEmail(String email) {
        Optional<User> user = userRepository.findByEmailIgnoreCase(email.trim().toLowerCase(Locale.ROOT))
                .filter(found -> found.getKeycloakId() != null);
        if (user.isEmpty()) {
            return;
        }
        List<String> outstanding = keycloakUserService.getUserById(user.get().getKeycloakId(), realm)
                .map(UserRepresentation::getRequiredActions)
                .orElse(List.of())
                .stream()
                .filter(ACCOUNT_ACTIONS::contains)
                .toList();
        if (!outstanding.isEmpty()) {
            requestActionsEmail(user.get().getKeycloakId(), outstanding);
        }
    }

    @Override
    @Transactional
    public DomainApplicationDTO applyForDomain(UUID userUuid, String rawDomain) {
        UserDomain domain = selfRegisterableDomain(rawDomain);
        DomainApplicationDTO application = domainApprovalService.request(userUuid, domain);
        eventPublisher.publishEvent(new UserDomainMappingEvent(userUuid, domain.name()));
        return application;
    }

    private void recordRegistration(RegistrationRequestDTO request, String email, UserDomain domain, String keycloakId) {
        User user = new User();
        user.setFirstName(request.firstName().trim());
        user.setMiddleName(blankToNull(request.middleName()));
        user.setLastName(request.lastName().trim());
        user.setEmail(email);
        user.setUsername(email);
        user.setPhoneNumber(request.phoneNumber().trim());
        user.setDob(request.dob());
        user.setGender(Gender.fromString(request.gender()));
        user.setUserNo(userNumberService.nextUserNo());
        user.setKeycloakId(keycloakId);
        user.setActive(true);
        User saved = userRepository.save(user);

        AccountRegistration registration = new AccountRegistration();
        registration.setUserUuid(saved.getUuid());
        registration.setRequestedDomain(domain.name());
        registration.setTermsAcceptedAt(now());
        registration.setActionsEmailSentAt(now());
        registrationRepository.save(registration);

        notificationPreferencesService.initializeUserPreferences(saved.getUuid());
        domainApprovalService.request(saved.getUuid(), domain);
        // Profile modules create the empty student/instructor/course creator profile from this.
        eventPublisher.publishEvent(new UserDomainMappingEvent(saved.getUuid(), domain.name()));
        requestActionsEmail(keycloakId, ACCOUNT_ACTIONS);
        log.info("Registered user {} requesting domain {}", saved.getUuid(), domain);
    }

    private void requestActionsEmail(String keycloakId, List<String> actions) {
        eventPublisher.publishEvent(new RegistrationActionsEmailRequestedEvent(keycloakId, realm, actions,
                properties.getClientId(), properties.getRedirectUri(), properties.getActionsEmailLifespanSeconds()));
    }

    private UserDomain selfRegisterableDomain(String raw) {
        UserDomain domain;
        try {
            domain = UserDomain.valueOf(raw.trim().toLowerCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("Unknown domain: " + raw);
        }
        if (!properties.getSelfRegisterableDomains().contains(domain)) {
            throw new IllegalArgumentException("The " + domain + " domain cannot be self-registered");
        }
        return domain;
    }

    private void enforceStudentAgeGate(UserDomain domain, RegistrationRequestDTO request) {
        if (domain != UserDomain.student || request.dob() == null) {
            return;
        }
        AgeGateDecision decision = ruleEvaluationService.evaluateAgeGate(request.dob(), RuleContext.builder()
                .ruleKey(STUDENT_AGE_GATE_RULE_KEY)
                .evaluationInstant(OffsetDateTime.now(ZoneOffset.UTC))
                .build());
        if (!decision.allowed()) {
            throw new IllegalArgumentException(decision.reason());
        }
    }

    private static KeycloakRegistration toKeycloak(RegistrationRequestDTO request, String email) {
        Gender gender = Gender.fromString(request.gender());
        return new KeycloakRegistration(email, request.firstName().trim(), blankToNull(request.middleName()),
                request.lastName().trim(), request.phoneNumber().trim(), request.dob(),
                gender == null ? null : gender.name());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
