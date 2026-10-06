package apps.sarafrika.elimika.tenancy.services.impl;

import apps.sarafrika.elimika.shared.event.user.DomainApprovalRequestedEvent;
import apps.sarafrika.elimika.shared.utils.enums.DomainApprovalStatus;
import apps.sarafrika.elimika.shared.utils.enums.UserDomain;
import apps.sarafrika.elimika.tenancy.config.RegistrationProperties;
import apps.sarafrika.elimika.tenancy.dto.AccountStatusDTO;
import apps.sarafrika.elimika.tenancy.dto.DomainApplicationDTO;
import apps.sarafrika.elimika.tenancy.entity.UserDomainMapping;
import apps.sarafrika.elimika.tenancy.internal.DomainApprovalNotifier;
import apps.sarafrika.elimika.tenancy.repository.UserRepository;
import apps.sarafrika.elimika.tenancy.repository.UserDomainMappingRepository;
import apps.sarafrika.elimika.tenancy.repository.UserDomainRepository;
import apps.sarafrika.elimika.tenancy.repository.UserOrganisationDomainMappingRepository;
import apps.sarafrika.elimika.tenancy.util.enums.AccountState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DomainApprovalServiceImplTest {

    private static final UUID STUDENT_DOMAIN = UUID.randomUUID();
    private static final UUID ADMIN_DOMAIN = UUID.randomUUID();

    @Mock
    private UserDomainMappingRepository mappingRepository;
    @Mock
    private UserDomainRepository domainRepository;
    @Mock
    private UserOrganisationDomainMappingRepository organisationMappingRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private DomainApprovalNotifier notifier;
    @Mock
    private UserRepository userRepository;

    private RegistrationProperties properties;
    private DomainApprovalServiceImpl service;
    private final UUID userUuid = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        properties = new RegistrationProperties();
        properties.setApprovalRequiredDomains(EnumSet.of(UserDomain.student));
        service = new DomainApprovalServiceImpl(mappingRepository, domainRepository, organisationMappingRepository,
                properties, eventPublisher, notifier, userRepository);
        when(domainRepository.findByDomainName("student")).thenReturn(Optional.of(domain(STUDENT_DOMAIN, "student")));
        when(domainRepository.findByDomainName("admin")).thenReturn(Optional.of(domain(ADMIN_DOMAIN, "admin")));
        when(domainRepository.findAll()).thenReturn(List.of(domain(STUDENT_DOMAIN, "student"), domain(ADMIN_DOMAIN, "admin")));
        when(mappingRepository.save(any(UserDomainMapping.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(organisationMappingRepository.findActiveByUser(userUuid)).thenReturn(List.of());
    }

    @Test
    void requestHoldsADomainThatNeedsApprovalAsPendingAndTellsAdmins() {
        when(mappingRepository.findByUserUuidAndUserDomainUuid(userUuid, STUDENT_DOMAIN)).thenReturn(List.of());

        DomainApplicationDTO application = service.request(userUuid, UserDomain.student);

        assertThat(application.status()).isEqualTo(DomainApprovalStatus.PENDING);
        verify(eventPublisher).publishEvent(new DomainApprovalRequestedEvent(userUuid, "student"));
        verify(notifier).requested(userUuid, "student");
    }

    @Test
    void requestGrantsADomainThatNeedsNoApproval() {
        when(mappingRepository.findByUserUuidAndUserDomainUuid(userUuid, ADMIN_DOMAIN)).thenReturn(List.of());

        DomainApplicationDTO application = service.request(userUuid, UserDomain.admin);

        assertThat(application.status()).isEqualTo(DomainApprovalStatus.APPROVED);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void requestLeavesAnExistingMappingAlone() {
        UserDomainMapping rejected = UserDomainMapping.of(userUuid, STUDENT_DOMAIN, DomainApprovalStatus.REJECTED);
        when(mappingRepository.findByUserUuidAndUserDomainUuid(userUuid, STUDENT_DOMAIN)).thenReturn(List.of(rejected));

        DomainApplicationDTO application = service.request(userUuid, UserDomain.student);

        assertThat(application.status()).isEqualTo(DomainApprovalStatus.REJECTED);
        verify(mappingRepository, never()).save(any());
    }

    @Test
    void grantApprovesAPendingMappingButNeverOverridesARejection() {
        UserDomainMapping pending = UserDomainMapping.of(userUuid, STUDENT_DOMAIN, DomainApprovalStatus.PENDING);
        when(mappingRepository.findByUserUuidAndUserDomainUuid(userUuid, STUDENT_DOMAIN)).thenReturn(List.of(pending));
        assertThat(service.grant(userUuid, UserDomain.student).status()).isEqualTo(DomainApprovalStatus.APPROVED);

        UserDomainMapping rejected = UserDomainMapping.of(userUuid, STUDENT_DOMAIN, DomainApprovalStatus.REJECTED);
        when(mappingRepository.findByUserUuidAndUserDomainUuid(userUuid, STUDENT_DOMAIN)).thenReturn(List.of(rejected));
        assertThat(service.grant(userUuid, UserDomain.student).status()).isEqualTo(DomainApprovalStatus.REJECTED);
    }

    @Test
    void decideRecordsTheReviewerAndReason() {
        UUID admin = UUID.randomUUID();
        UserDomainMapping pending = UserDomainMapping.of(userUuid, STUDENT_DOMAIN, DomainApprovalStatus.PENDING);
        when(mappingRepository.findByUserUuidAndUserDomainUuid(userUuid, STUDENT_DOMAIN)).thenReturn(List.of(pending));

        DomainApplicationDTO decided = service.decide(userUuid, UserDomain.student, DomainApprovalStatus.REJECTED,
                "Incomplete details", admin);

        assertThat(decided.status()).isEqualTo(DomainApprovalStatus.REJECTED);
        assertThat(decided.reviewReason()).isEqualTo("Incomplete details");
        assertThat(pending.getReviewedBy()).isEqualTo(admin);
        assertThat(decided.reviewedAt()).isNotNull();
        verify(notifier).decided(userUuid, "student", DomainApprovalStatus.REJECTED, "Incomplete details");
    }

    @Test
    void moderateMapsRevokeToSuspended() {
        UserDomainMapping approved = UserDomainMapping.of(userUuid, STUDENT_DOMAIN, DomainApprovalStatus.APPROVED);
        when(mappingRepository.findByUserUuidAndUserDomainUuid(userUuid, STUDENT_DOMAIN)).thenReturn(List.of(approved));

        DomainApplicationDTO decided = service.moderate(userUuid, UserDomain.student, "revoke", null, UUID.randomUUID());

        assertThat(decided.status()).isEqualTo(DomainApprovalStatus.SUSPENDED);
    }

    @Test
    void moderateRefusesProfileReviewedDomainsAndSelfApproval() {
        assertThatThrownBy(() -> service.moderate(userUuid, UserDomain.course_creator, "approve", null, UUID.randomUUID()))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> service.moderate(userUuid, UserDomain.student, "approve", null, userUuid))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void decideRefusesToMoveAMappingBackToPending() {
        assertThatThrownBy(() -> service.decide(userUuid, UserDomain.student, DomainApprovalStatus.PENDING, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void accountIsPendingUntilSomethingIsApproved() {
        when(mappingRepository.findByUserUuid(userUuid))
                .thenReturn(List.of(UserDomainMapping.of(userUuid, STUDENT_DOMAIN, DomainApprovalStatus.PENDING)));
        AccountStatusDTO pending = service.accountStatus(userUuid);
        assertThat(pending.accountState()).isEqualTo(AccountState.PENDING_APPROVAL);
        assertThat(pending.approvedDomains()).isEmpty();
        assertThat(pending.domainApplications()).extracting(DomainApplicationDTO::domain).containsExactly("student");

        when(mappingRepository.findByUserUuid(userUuid))
                .thenReturn(List.of(UserDomainMapping.of(userUuid, STUDENT_DOMAIN, DomainApprovalStatus.APPROVED)));
        AccountStatusDTO active = service.accountStatus(userUuid);
        assertThat(active.accountState()).isEqualTo(AccountState.ACTIVE);
        assertThat(active.approvedDomains()).containsExactly("student");
    }

    @Test
    void accountWithOnlyAWithdrawnDomainIsSuspended() {
        when(mappingRepository.findByUserUuid(userUuid))
                .thenReturn(List.of(UserDomainMapping.of(userUuid, STUDENT_DOMAIN, DomainApprovalStatus.SUSPENDED)));

        assertThat(service.accountStatus(userUuid).accountState()).isEqualTo(AccountState.SUSPENDED);
    }

    private static apps.sarafrika.elimika.tenancy.entity.UserDomain domain(UUID uuid, String name) {
        apps.sarafrika.elimika.tenancy.entity.UserDomain domain = new apps.sarafrika.elimika.tenancy.entity.UserDomain();
        domain.setUuid(uuid);
        domain.setDomainName(name);
        return domain;
    }
}
