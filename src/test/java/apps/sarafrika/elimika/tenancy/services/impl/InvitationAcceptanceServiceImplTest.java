package apps.sarafrika.elimika.tenancy.services.impl;

import apps.sarafrika.elimika.shared.event.student.GuardianConsentRecordedEvent;
import apps.sarafrika.elimika.systemconfig.dto.AgeGateDecision;
import apps.sarafrika.elimika.systemconfig.dto.RuleContext;
import apps.sarafrika.elimika.systemconfig.service.RuleEvaluationService;
import apps.sarafrika.elimika.tenancy.dto.AcceptInvitationRequestDTO;
import apps.sarafrika.elimika.tenancy.dto.AcceptInvitationResultDTO;
import apps.sarafrika.elimika.tenancy.dto.GuardianConsentRequestDTO;
import apps.sarafrika.elimika.tenancy.dto.GuardianDetailsRequestDTO;
import apps.sarafrika.elimika.tenancy.entity.Organisation;
import apps.sarafrika.elimika.tenancy.entity.OrganisationInvitation;
import apps.sarafrika.elimika.tenancy.entity.User;
import apps.sarafrika.elimika.tenancy.entity.UserDomain;
import apps.sarafrika.elimika.tenancy.entity.UserOrganisationDomainMapping;
import apps.sarafrika.elimika.tenancy.internal.InvitationTokenService;
import apps.sarafrika.elimika.tenancy.repository.OrganisationInvitationClassRepository;
import apps.sarafrika.elimika.tenancy.repository.OrganisationInvitationRepository;
import apps.sarafrika.elimika.tenancy.repository.OrganisationRepository;
import apps.sarafrika.elimika.tenancy.repository.UserDomainRepository;
import apps.sarafrika.elimika.tenancy.repository.UserOrganisationDomainMappingRepository;
import apps.sarafrika.elimika.tenancy.repository.UserRepository;
import apps.sarafrika.elimika.tenancy.services.UserService;
import apps.sarafrika.elimika.tenancy.util.enums.ConsentSource;
import apps.sarafrika.elimika.tenancy.util.enums.InvitationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InvitationAcceptanceServiceImplTest {

    private static final UUID ORGANISATION_UUID = UUID.randomUUID();
    private static final UUID DOMAIN_UUID = UUID.randomUUID();
    private static final String RAW_TOKEN = "raw-invitation-token";
    private static final String GUARDIAN_RAW_TOKEN = "raw-guardian-token";

    @Mock private OrganisationInvitationRepository invitationRepository;
    @Mock private OrganisationInvitationClassRepository invitationClassRepository;
    @Mock private OrganisationRepository organisationRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserDomainRepository userDomainRepository;
    @Mock private UserOrganisationDomainMappingRepository mappingRepository;
    @Mock private UserService userService;
    @Mock private RuleEvaluationService ruleEvaluationService;
    @Mock private ApplicationEventPublisher eventPublisher;

    private final InvitationTokenService tokenService = new InvitationTokenService();
    private InvitationAcceptanceServiceImpl service;

    private User invitee;
    private UserOrganisationDomainMapping mapping;

    @BeforeEach
    void setUp() {
        service = new InvitationAcceptanceServiceImpl(
                invitationRepository, invitationClassRepository, organisationRepository,
                userRepository, userDomainRepository, mappingRepository, userService,
                ruleEvaluationService, tokenService, eventPublisher);

        UserDomain studentDomain = new UserDomain();
        studentDomain.setUuid(DOMAIN_UUID);
        studentDomain.setDomainName("student");
        when(userDomainRepository.findByUuid(DOMAIN_UUID)).thenReturn(Optional.of(studentDomain));

        Organisation organisation = new Organisation();
        organisation.setName("Sarafrika Academy");
        when(organisationRepository.findByUuid(ORGANISATION_UUID)).thenReturn(Optional.of(organisation));

        invitee = user("jane@example.com", LocalDate.of(1995, 3, 2));
        mapping = new UserOrganisationDomainMapping();

        when(invitationClassRepository.findByInvitationUuid(any())).thenReturn(List.of());
        when(mappingRepository.findByUserUuidAndOrganisationUuidAndActiveTrueAndDeletedFalse(any(), any()))
                .thenReturn(Optional.of(mapping));
        when(invitationRepository.save(any())).thenAnswer(call -> call.getArgument(0));
        when(ruleEvaluationService.evaluateAgeGate(any(), any(RuleContext.class)))
                .thenReturn(AgeGateDecision.allow());
    }

    // ================================
    // ADULT ACCEPTANCE
    // ================================

    @Test
    void anAdultAcceptingCreatesTheAffiliationAndRecordsTheirConsent() {
        OrganisationInvitation invitation = pendingInvitation();
        givenInvitationForToken(invitation);

        AcceptInvitationResultDTO result = service.acceptByToken(
                RAW_TOKEN, new AcceptInvitationRequestDTO(null, true), invitee.getUuid());

        assertThat(result.affiliated()).isTrue();
        assertThat(result.guardianConsentRequired()).isFalse();
        assertThat(result.status()).isEqualTo(InvitationStatus.ACCEPTED);
        assertThat(invitation.getAcceptedUserUuid()).isEqualTo(invitee.getUuid());

        verify(userService).assignUserToOrganisation(
                eq(invitee.getUuid()), eq(ORGANISATION_UUID), eq("student"), any());
        assertThat(mapping.getConsentSource()).isEqualTo(ConsentSource.INVITATION);
        assertThat(mapping.getConsentGrantedByUserUuid()).isEqualTo(invitee.getUuid());
        assertThat(mapping.getConsentGrantedAt()).isNotNull();
    }

    @Test
    void acceptingBurnsTheLinkSoAForwardedEmailCannotBeReplayed() {
        OrganisationInvitation invitation = pendingInvitation();
        String originalHash = invitation.getTokenHash();
        givenInvitationForToken(invitation);

        service.acceptByToken(RAW_TOKEN, new AcceptInvitationRequestDTO(null, true), invitee.getUuid());

        assertThat(invitation.getTokenHash()).isNotEqualTo(originalHash);
    }

    @Test
    void invitedClassesAreSurfacedNotEnrolled() {
        OrganisationInvitation invitation = pendingInvitation();
        givenInvitationForToken(invitation);
        UUID classUuid = UUID.randomUUID();
        apps.sarafrika.elimika.tenancy.entity.OrganisationInvitationClass link =
                new apps.sarafrika.elimika.tenancy.entity.OrganisationInvitationClass(invitation.getUuid(), classUuid);
        when(invitationClassRepository.findByInvitationUuid(invitation.getUuid())).thenReturn(List.of(link));

        AcceptInvitationResultDTO result = service.acceptByToken(
                RAW_TOKEN, new AcceptInvitationRequestDTO(null, true), invitee.getUuid());

        assertThat(result.surfacedClassUuids()).containsExactly(classUuid);
        assertThat(result.message()).contains("available for you to enrol");
    }

    @Test
    void someoneElseHoldingTheLinkCannotAcceptIt() {
        OrganisationInvitation invitation = pendingInvitation();
        givenInvitationForToken(invitation);
        User bystander = user("someone.else@example.com", LocalDate.of(1990, 1, 1));

        assertThatThrownBy(() -> service.acceptByToken(
                RAW_TOKEN, new AcceptInvitationRequestDTO(null, true), bystander.getUuid()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("different email address");

        verify(userService, never()).assignUserToOrganisation(any(), any(), any(), any());
    }

    @Test
    void anExpiredInvitationCannotBeAccepted() {
        OrganisationInvitation invitation = pendingInvitation();
        invitation.setExpiresAt(LocalDateTime.now().minusDays(1));
        givenInvitationForToken(invitation);

        assertThatThrownBy(() -> service.acceptByToken(
                RAW_TOKEN, new AcceptInvitationRequestDTO(null, true), invitee.getUuid()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no longer open");
    }

    @Test
    void acceptanceNeedsADateOfBirthWhenTheProfileHasNone() {
        OrganisationInvitation invitation = pendingInvitation();
        givenInvitationForToken(invitation);
        invitee.setDob(null);

        assertThatThrownBy(() -> service.acceptByToken(
                RAW_TOKEN, new AcceptInvitationRequestDTO(null, true), invitee.getUuid()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("date of birth");
    }

    @Test
    void aSuppliedDateOfBirthIsSavedToTheProfileWhenItWasMissing() {
        OrganisationInvitation invitation = pendingInvitation();
        givenInvitationForToken(invitation);
        invitee.setDob(null);
        LocalDate dob = LocalDate.of(1999, 6, 1);

        service.acceptByToken(RAW_TOKEN, new AcceptInvitationRequestDTO(dob, true), invitee.getUuid());

        assertThat(invitee.getDob()).isEqualTo(dob);
        verify(userRepository).save(invitee);
    }

    // ================================
    // MINOR / GUARDIAN
    // ================================

    @Test
    void aMinorAcceptingCreatesNoAffiliationAndWaitsForAGuardian() {
        OrganisationInvitation invitation = pendingInvitation();
        givenInvitationForToken(invitation);
        givenTheInviteeIsAMinor();

        AcceptInvitationResultDTO result = service.acceptByToken(
                RAW_TOKEN, new AcceptInvitationRequestDTO(LocalDate.now().minusYears(12), true), invitee.getUuid());

        assertThat(result.affiliated()).isFalse();
        assertThat(result.guardianConsentRequired()).isTrue();
        assertThat(result.status()).isEqualTo(InvitationStatus.AWAITING_GUARDIAN_CONSENT);
        assertThat(invitation.isRequiresGuardianConsent()).isTrue();

        // The whole point: a minor cannot consent to this for themselves.
        verify(userService, never()).assignUserToOrganisation(any(), any(), any(), any());
    }

    @Test
    void nominatingAGuardianIssuesThemTheirOwnSeparateLink() {
        OrganisationInvitation invitation = awaitingGuardianInvitation();
        givenInvitationForToken(invitation);

        service.submitGuardianDetails(RAW_TOKEN,
                new GuardianDetailsRequestDTO("parent@example.com", "Mary Doe", "parent", "+254700000000"),
                invitee.getUuid());

        assertThat(invitation.getGuardianEmail()).isEqualTo("parent@example.com");
        assertThat(invitation.getGuardianName()).isEqualTo("Mary Doe");
        assertThat(invitation.getGuardianRelationshipType()).isEqualTo("PARENT");
        assertThat(invitation.getGuardianPhone()).isEqualTo("+254700000000");
        // Distinct from the child's token, so the child's link cannot be used to consent.
        assertThat(invitation.getGuardianConsentTokenHash())
                .isNotNull()
                .isNotEqualTo(invitation.getTokenHash());
    }

    @Test
    void aMinorCannotNominateThemselvesAsTheirOwnGuardian() {
        OrganisationInvitation invitation = awaitingGuardianInvitation();
        givenInvitationForToken(invitation);

        assertThatThrownBy(() -> service.submitGuardianDetails(RAW_TOKEN,
                new GuardianDetailsRequestDTO("jane@example.com", "Jane Doe", "PARENT", null),
                invitee.getUuid()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("someone other than the student");
    }

    @Test
    void guardianConsentCreatesTheChildsAffiliationAndTheGuardianLink() {
        OrganisationInvitation invitation = awaitingGuardianInvitation();
        invitation.setGuardianEmail("parent@example.com");
        invitation.setGuardianRelationshipType("PARENT");
        invitation.setRecipientUserUuid(invitee.getUuid());
        givenGuardianInvitationForToken(invitation);
        User guardian = user("parent@example.com", LocalDate.of(1980, 1, 1));

        AcceptInvitationResultDTO result = service.guardianConsent(
                GUARDIAN_RAW_TOKEN, new GuardianConsentRequestDTO("ACADEMICS", true), guardian.getUuid());

        assertThat(result.affiliated()).isTrue();
        assertThat(invitation.getGuardianUserUuid()).isEqualTo(guardian.getUuid());
        assertThat(invitation.getGuardianConsentedAt()).isNotNull();

        // Consent is attributed to the guardian, not the child.
        assertThat(mapping.getConsentSource()).isEqualTo(ConsentSource.GUARDIAN);
        assertThat(mapping.getConsentGrantedByUserUuid()).isEqualTo(guardian.getUuid());
        verify(userService).assignUserToOrganisation(
                eq(invitee.getUuid()), eq(ORGANISATION_UUID), eq("student"), any());

        ArgumentCaptor<GuardianConsentRecordedEvent> captor =
                ArgumentCaptor.forClass(GuardianConsentRecordedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().studentUserUuid()).isEqualTo(invitee.getUuid());
        assertThat(captor.getValue().guardianUserUuid()).isEqualTo(guardian.getUuid());
        assertThat(captor.getValue().shareScope()).isEqualTo("ACADEMICS");
    }

    @Test
    void onlyTheNominatedGuardianMayConsent() {
        OrganisationInvitation invitation = awaitingGuardianInvitation();
        invitation.setGuardianEmail("parent@example.com");
        invitation.setRecipientUserUuid(invitee.getUuid());
        givenGuardianInvitationForToken(invitation);
        User impostor = user("stranger@example.com", LocalDate.of(1980, 1, 1));

        assertThatThrownBy(() -> service.guardianConsent(
                GUARDIAN_RAW_TOKEN, new GuardianConsentRequestDTO(null, true), impostor.getUuid()))
                .isInstanceOf(AccessDeniedException.class);

        verify(userService, never()).assignUserToOrganisation(any(), any(), any(), any());
    }

    @Test
    void guardianRefusalLeavesNoAffiliation() {
        OrganisationInvitation invitation = awaitingGuardianInvitation();
        invitation.setGuardianEmail("parent@example.com");
        invitation.setRecipientUserUuid(invitee.getUuid());
        givenGuardianInvitationForToken(invitation);
        User guardian = user("parent@example.com", LocalDate.of(1980, 1, 1));

        service.guardianDecline(GUARDIAN_RAW_TOKEN, guardian.getUuid());

        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.DECLINED);
        verify(userService, never()).assignUserToOrganisation(any(), any(), any(), any());
    }

    @Test
    void aMinorsInvitationCannotBeAcceptedDirectlyOnceItIsWithTheGuardian() {
        OrganisationInvitation invitation = awaitingGuardianInvitation();
        givenInvitationForToken(invitation);

        assertThatThrownBy(() -> service.acceptByToken(
                RAW_TOKEN, new AcceptInvitationRequestDTO(null, true), invitee.getUuid()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("waiting on a guardian");
    }

    // ================================
    // DECLINE
    // ================================

    @Test
    void decliningLeavesNoAffiliation() {
        OrganisationInvitation invitation = pendingInvitation();
        givenInvitationForToken(invitation);

        service.declineByToken(RAW_TOKEN, invitee.getUuid());

        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.DECLINED);
        assertThat(invitation.getDeclinedAt()).isNotNull();
        verify(userService, never()).assignUserToOrganisation(any(), any(), any(), any());
    }

    // ================================
    // PUBLIC LOOKUP
    // ================================

    @Test
    void thePublicLookupMasksTheRecipientAddress() {
        OrganisationInvitation invitation = pendingInvitation();
        givenInvitationForToken(invitation);

        var publicView = service.lookupByToken(RAW_TOKEN);

        assertThat(publicView.maskedRecipientEmail()).isEqualTo("j***e@example.com");
        assertThat(publicView.organisationName()).isEqualTo("Sarafrika Academy");
        assertThat(publicView.actionable()).isTrue();
    }

    // ================================
    // HELPERS
    // ================================

    private void givenInvitationForToken(OrganisationInvitation invitation) {
        when(invitationRepository.findByTokenHash(tokenService.hash(RAW_TOKEN)))
                .thenReturn(Optional.of(invitation));
    }

    private void givenGuardianInvitationForToken(OrganisationInvitation invitation) {
        when(invitationRepository.findByGuardianConsentTokenHash(tokenService.hash(GUARDIAN_RAW_TOKEN)))
                .thenReturn(Optional.of(invitation));
    }

    private void givenTheInviteeIsAMinor() {
        when(ruleEvaluationService.evaluateAgeGate(any(), any(RuleContext.class)))
                .thenReturn(AgeGateDecision.rejected("Below the minimum age"));
    }

    private User user(String email, LocalDate dob) {
        User user = new User();
        user.setUuid(UUID.randomUUID());
        user.setEmail(email);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setDob(dob);
        when(userRepository.findByUuid(user.getUuid())).thenReturn(Optional.of(user));
        return user;
    }

    private OrganisationInvitation pendingInvitation() {
        OrganisationInvitation invitation = new OrganisationInvitation();
        invitation.setUuid(UUID.randomUUID());
        invitation.setOrganisationUuid(ORGANISATION_UUID);
        invitation.setDomainUuid(DOMAIN_UUID);
        invitation.setRecipientEmail("jane@example.com");
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setTokenHash(tokenService.hash(RAW_TOKEN));
        invitation.setExpiresAt(LocalDateTime.now().plusDays(14));
        return invitation;
    }

    private OrganisationInvitation awaitingGuardianInvitation() {
        OrganisationInvitation invitation = pendingInvitation();
        invitation.setStatus(InvitationStatus.AWAITING_GUARDIAN_CONSENT);
        invitation.setRequiresGuardianConsent(true);
        invitation.setGuardianConsentTokenHash(tokenService.hash(GUARDIAN_RAW_TOKEN));
        return invitation;
    }
}
