package apps.sarafrika.elimika.tenancy.services.impl;

import apps.sarafrika.elimika.shared.event.notification.NotificationRequestedEvent;
import apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService;
import apps.sarafrika.elimika.tenancy.dto.SendOrganisationInvitationsRequestDTO;
import apps.sarafrika.elimika.tenancy.dto.OrganisationInvitationDTO;
import apps.sarafrika.elimika.tenancy.dto.SendOrganisationInvitationsResultDTO;
import apps.sarafrika.elimika.tenancy.dto.SendParentInvitationsRequestDTO;
import apps.sarafrika.elimika.systemconfig.dto.AgeGateDecision;
import apps.sarafrika.elimika.systemconfig.service.RuleEvaluationService;
import apps.sarafrika.elimika.tenancy.entity.Organisation;
import apps.sarafrika.elimika.tenancy.entity.OrganisationInvitation;
import apps.sarafrika.elimika.tenancy.entity.StudentGroupMember;
import apps.sarafrika.elimika.tenancy.entity.User;
import apps.sarafrika.elimika.tenancy.entity.UserDomain;
import apps.sarafrika.elimika.tenancy.internal.InvitationLinkFactory;
import apps.sarafrika.elimika.tenancy.internal.InvitationTokenService;
import apps.sarafrika.elimika.tenancy.repository.OrganisationInvitationClassRepository;
import apps.sarafrika.elimika.tenancy.repository.OrganisationInvitationRepository;
import apps.sarafrika.elimika.tenancy.repository.OrganisationRepository;
import apps.sarafrika.elimika.tenancy.repository.TrainingBranchRepository;
import apps.sarafrika.elimika.tenancy.repository.UserDomainRepository;
import apps.sarafrika.elimika.tenancy.repository.UserOrganisationDomainMappingRepository;
import apps.sarafrika.elimika.tenancy.repository.StudentGroupMemberRepository;
import apps.sarafrika.elimika.tenancy.repository.UserRepository;
import apps.sarafrika.elimika.tenancy.spi.StudentGroupLookupService;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrganisationInvitationServiceImplTest {

    private static final UUID ORGANISATION_UUID = UUID.randomUUID();
    private static final UUID DOMAIN_UUID = UUID.randomUUID();
    private static final UUID INVITER_UUID = UUID.randomUUID();

    @Mock
    private OrganisationInvitationRepository invitationRepository;
    @Mock
    private OrganisationInvitationClassRepository invitationClassRepository;
    @Mock
    private OrganisationRepository organisationRepository;
    @Mock
    private UserDomainRepository userDomainRepository;
    @Mock
    private TrainingBranchRepository trainingBranchRepository;
    @Mock
    private UserOrganisationDomainMappingRepository mappingRepository;
    @Mock
    private ClassDefinitionLookupService classDefinitionLookupService;
    @Mock
    private UserLookupService userLookupService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private StudentGroupMemberRepository studentGroupMemberRepository;
    @Mock
    private StudentGroupLookupService studentGroupLookupService;
    @Mock
    private RuleEvaluationService ruleEvaluationService;

    private OrganisationInvitationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OrganisationInvitationServiceImpl(
                invitationRepository,
                invitationClassRepository,
                organisationRepository,
                userDomainRepository,
                trainingBranchRepository,
                mappingRepository,
                userRepository,
                studentGroupMemberRepository,
                studentGroupLookupService,
                classDefinitionLookupService,
                userLookupService,
                new InvitationTokenService(),
                new InvitationLinkFactory("https://elimika.test"),
                eventPublisher,
                ruleEvaluationService);
        when(ruleEvaluationService.evaluateAgeGate(any(), any())).thenReturn(AgeGateDecision.allow());

        UserDomain studentDomain = new UserDomain();
        studentDomain.setUuid(DOMAIN_UUID);
        studentDomain.setDomainName("student");

        when(organisationRepository.findByUuid(ORGANISATION_UUID)).thenReturn(Optional.of(new Organisation()));
        when(userDomainRepository.findByDomainNameAndOrgSupportedTrue("student"))
                .thenReturn(Optional.of(studentDomain));
        when(userDomainRepository.findByUuid(DOMAIN_UUID)).thenReturn(Optional.of(studentDomain));
        when(invitationRepository.save(any(OrganisationInvitation.class)))
                .thenAnswer(call -> {
                    OrganisationInvitation invitation = call.getArgument(0);
                    if (invitation.getUuid() == null) {
                        invitation.setUuid(UUID.randomUUID());
                    }
                    return invitation;
                });
    }

    @Test
    void sendCreatesAPendingOfferAndProvisionsNothing() {
        when(userLookupService.findUserUuidByEmail("jane@example.com")).thenReturn(Optional.empty());

        SendOrganisationInvitationsResultDTO result =
                service.send(ORGANISATION_UUID, request("Jane Doe", "Jane@Example.com "), INVITER_UUID);

        assertThat(result.failed()).isEmpty();
        assertThat(result.sent()).hasSize(1);
        assertThat(result.sent().getFirst().status()).isEqualTo(InvitationStatus.PENDING);
        // Email is normalised so a re-invite with different casing still collides.
        assertThat(result.sent().getFirst().recipientEmail()).isEqualTo("jane@example.com");
        assertThat(result.sent().getFirst().existingPlatformUser()).isFalse();

        // The offer must not create an affiliation.
        verify(mappingRepository, never()).save(any());
    }

    @Test
    void sendStoresOnlyTheTokenHash() {
        when(userLookupService.findUserUuidByEmail(any())).thenReturn(Optional.empty());

        service.send(ORGANISATION_UUID, request("Jane Doe", "jane@example.com"), INVITER_UUID);

        verify(invitationRepository).save(org.mockito.ArgumentMatchers.argThat(invitation -> {
            assertThat(invitation.getTokenHash()).hasSize(64).matches("[0-9a-f]+");
            return true;
        }));
    }

    @Test
    void theInvitationEmailCarriesTheLinkAndNoNullVariables() {
        when(userLookupService.findUserUuidByEmail(any())).thenReturn(Optional.empty());

        // Nothing optional is populated: no recipient name, no message, no inviter on file.
        service.send(ORGANISATION_UUID,
                new SendOrganisationInvitationsRequestDTO(
                        List.of(new SendOrganisationInvitationsRequestDTO.Recipient("jane@example.com", null)),
                        null, "student", null, null, null, null),
                INVITER_UUID);

        ArgumentCaptor<NotificationRequestedEvent> captor =
                ArgumentCaptor.forClass(NotificationRequestedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        NotificationRequestedEvent email = captor.getValue();

        assertThat(email.recipientEmail()).isEqualTo("jane@example.com");
        assertThat(email.deliveryChannels()).containsExactly("email");
        assertThat((String) email.templateVariables().get("invitationLink"))
                .startsWith("https://elimika.test/invitations/");
        // The downstream event copies these into an immutable map, which rejects nulls.
        assertThat(email.templateVariables()).doesNotContainValue(null);
    }

    @Test
    void sendFlagsRecipientsWhoAlreadyHaveAnAccount() {
        UUID existingUser = UUID.randomUUID();
        when(userLookupService.findUserUuidByEmail("jane@example.com")).thenReturn(Optional.of(existingUser));
        when(mappingRepository.existsByUserUuidAndOrganisationUuidAndActiveTrueAndDeletedFalse(
                existingUser, ORGANISATION_UUID)).thenReturn(false);

        SendOrganisationInvitationsResultDTO result =
                service.send(ORGANISATION_UUID, request("Jane Doe", "jane@example.com"), INVITER_UUID);

        assertThat(result.sent()).hasSize(1);
        assertThat(result.sent().getFirst().existingPlatformUser()).isTrue();
        assertThat(result.sent().getFirst().recipientUserUuid()).isEqualTo(existingUser);
    }

    @Test
    void anExistingMemberIsStillInvitable() {
        UUID existingUser = UUID.randomUUID();
        when(userLookupService.findUserUuidByEmail("jane@example.com")).thenReturn(Optional.of(existingUser));
        when(mappingRepository.existsByUserUuidAndOrganisationUuidAndActiveTrueAndDeletedFalse(
                existingUser, ORGANISATION_UUID)).thenReturn(true);

        SendOrganisationInvitationsResultDTO result =
                service.send(ORGANISATION_UUID, request("Jane Doe", "jane@example.com"), INVITER_UUID);

        // An organisation must be able to offer new classes to a student it already has.
        // Acceptance leaves the existing affiliation untouched.
        assertThat(result.failed()).isEmpty();
        assertThat(result.sent()).hasSize(1);
    }

    @Test
    void sendRefusesADuplicateLiveOffer() {
        when(userLookupService.findUserUuidByEmail(any())).thenReturn(Optional.empty());
        when(invitationRepository.findLiveByOrganisationAndEmail(ORGANISATION_UUID, "jane@example.com"))
                .thenReturn(Optional.of(new OrganisationInvitation()));

        SendOrganisationInvitationsResultDTO result =
                service.send(ORGANISATION_UUID, request("Jane Doe", "jane@example.com"), INVITER_UUID);

        assertThat(result.sent()).isEmpty();
        assertThat(result.failed()).singleElement()
                .satisfies(failure -> assertThat(failure.reason()).contains("pending invitation"));
    }

    @Test
    void oneBadRecipientDoesNotCostTheRestOfTheBatch() {
        when(userLookupService.findUserUuidByEmail(any())).thenReturn(Optional.empty());
        when(invitationRepository.findLiveByOrganisationAndEmail(ORGANISATION_UUID, "taken@example.com"))
                .thenReturn(Optional.of(new OrganisationInvitation()));

        SendOrganisationInvitationsRequestDTO request = new SendOrganisationInvitationsRequestDTO(
                List.of(
                        new SendOrganisationInvitationsRequestDTO.Recipient("taken@example.com", "Taken"),
                        new SendOrganisationInvitationsRequestDTO.Recipient("fresh@example.com", "Fresh")),
                null, "student", null, null, null, null);

        SendOrganisationInvitationsResultDTO result = service.send(ORGANISATION_UUID, request, INVITER_UUID);

        assertThat(result.sent()).hasSize(1);
        assertThat(result.failed()).hasSize(1);
    }

    @Test
    void sendDeduplicatesWithinASingleBatch() {
        when(userLookupService.findUserUuidByEmail(any())).thenReturn(Optional.empty());

        SendOrganisationInvitationsRequestDTO request = new SendOrganisationInvitationsRequestDTO(
                List.of(
                        new SendOrganisationInvitationsRequestDTO.Recipient("jane@example.com", "Jane"),
                        new SendOrganisationInvitationsRequestDTO.Recipient("JANE@example.com", "Jane again")),
                null, "student", null, null, null, null);

        SendOrganisationInvitationsResultDTO result = service.send(ORGANISATION_UUID, request, INVITER_UUID);

        // The same person pasted twice yields one invitation, not an error row.
        assertThat(result.sent()).hasSize(1);
        assertThat(result.failed()).isEmpty();
    }

    @Test
    void sendRejectsClassesBelongingToAnotherOrganisation() {
        UUID foreignClass = UUID.randomUUID();
        when(classDefinitionLookupService.findClassDefinitionUuidsByOrganisationUuid(ORGANISATION_UUID))
                .thenReturn(List.of(UUID.randomUUID()));

        SendOrganisationInvitationsRequestDTO request = new SendOrganisationInvitationsRequestDTO(
                List.of(new SendOrganisationInvitationsRequestDTO.Recipient("jane@example.com", "Jane")),
                null, "student", null, List.of(foreignClass), null, null);

        assertThatThrownBy(() -> service.send(ORGANISATION_UUID, request, INVITER_UUID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("do not belong to your organisation");
    }

    @Test
    void sendRejectsADomainOrganisationsMayNotInviteInto() {
        SendOrganisationInvitationsRequestDTO request = new SendOrganisationInvitationsRequestDTO(
                List.of(new SendOrganisationInvitationsRequestDTO.Recipient("jane@example.com", "Jane")),
                null, "course_creator", null, null, null, null);

        assertThatThrownBy(() -> service.send(ORGANISATION_UUID, request, INVITER_UUID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid domain_name");
    }

    @Test
    void revokeRetiresTheOfferAndInvalidatesTheEmailedLink() {
        OrganisationInvitation invitation = liveInvitation();
        String originalHash = invitation.getTokenHash();
        when(invitationRepository.findByUuid(invitation.getUuid())).thenReturn(Optional.of(invitation));
        when(invitationClassRepository.findByInvitationUuid(any())).thenReturn(List.of());

        service.revoke(ORGANISATION_UUID, invitation.getUuid());

        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.REVOKED);
        assertThat(invitation.getRevokedAt()).isNotNull();
        assertThat(invitation.getTokenHash()).isNotEqualTo(originalHash);
    }

    @Test
    void revokeRefusesAnInvitationThatIsAlreadySettled() {
        OrganisationInvitation invitation = liveInvitation();
        invitation.setStatus(InvitationStatus.ACCEPTED);
        when(invitationRepository.findByUuid(invitation.getUuid())).thenReturn(Optional.of(invitation));

        assertThatThrownBy(() -> service.revoke(ORGANISATION_UUID, invitation.getUuid()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only a pending invitation can be revoked");
    }

    @Test
    void anotherOrganisationCannotSeeOrTouchTheInvitation() {
        OrganisationInvitation invitation = liveInvitation();
        when(invitationRepository.findByUuid(invitation.getUuid())).thenReturn(Optional.of(invitation));

        // Reported as not-found rather than forbidden so existence cannot be probed.
        assertThatThrownBy(() -> service.revoke(UUID.randomUUID(), invitation.getUuid()))
                .hasMessageContaining("Invitation not found");
    }

    @Test
    void resendIssuesAFreshLinkAndSupersedesThePreviousOne() {
        OrganisationInvitation invitation = liveInvitation();
        String originalHash = invitation.getTokenHash();
        LocalDateTime originalExpiry = invitation.getExpiresAt();
        when(invitationRepository.findByUuid(invitation.getUuid())).thenReturn(Optional.of(invitation));
        when(invitationClassRepository.findByInvitationUuid(any())).thenReturn(List.of());

        service.resend(ORGANISATION_UUID, invitation.getUuid());

        assertThat(invitation.getTokenHash()).isNotEqualTo(originalHash);
        assertThat(invitation.getExpiresAt()).isAfter(originalExpiry);
        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.PENDING);
    }

    @Test
    void expireLapsedRetiresOffersPastTheirWindow() {
        OrganisationInvitation lapsed = liveInvitation();
        lapsed.setExpiresAt(LocalDateTime.now().minusDays(1));
        when(invitationRepository.findLapsed(any())).thenReturn(List.of(lapsed));

        int expired = service.expireLapsed();

        assertThat(expired).isEqualTo(1);
        assertThat(lapsed.getStatus()).isEqualTo(InvitationStatus.EXPIRED);
        verify(invitationRepository).saveAll(anyList());
    }

    @Test
    void expireLapsedIsANoOpWhenNothingHasLapsed() {
        when(invitationRepository.findLapsed(any())).thenReturn(List.of());

        assertThat(service.expireLapsed()).isZero();
        verify(invitationRepository, never()).saveAll(anyList());
    }


    // ================================
    // STUDENT GROUPS
    // ================================

    @Test
    void aStudentGroupExpandsIntoOneOfferPerMember() {
        UUID groupUuid = UUID.randomUUID();
        UUID memberA = UUID.randomUUID();
        UUID memberB = UUID.randomUUID();
        givenGroupWithMembers(groupUuid, memberA, memberB);
        givenUser(memberA, "amina@example.com", "Amina", "Yusuf");
        givenUser(memberB, "brian@example.com", "Brian", "Otieno");
        when(userLookupService.findUserUuidByEmail(any())).thenReturn(Optional.empty());

        SendOrganisationInvitationsResultDTO result = service.send(ORGANISATION_UUID,
                new SendOrganisationInvitationsRequestDTO(
                        null, List.of(groupUuid), "student", null, null, null, null),
                INVITER_UUID);

        // A group is a convenience for the sender, not a shortcut around consent: each
        // member gets their own offer and decides for themselves.
        assertThat(result.sent()).hasSize(2);
        assertThat(result.sent()).extracting(OrganisationInvitationDTO::recipientEmail)
                .containsExactlyInAnyOrder("amina@example.com", "brian@example.com");
    }

    @Test
    void groupMembersAndExplicitRecipientsAreDeduplicatedByEmail() {
        UUID groupUuid = UUID.randomUUID();
        UUID memberA = UUID.randomUUID();
        givenGroupWithMembers(groupUuid, memberA);
        givenUser(memberA, "amina@example.com", "Amina", "Yusuf");
        when(userLookupService.findUserUuidByEmail(any())).thenReturn(Optional.empty());

        SendOrganisationInvitationsResultDTO result = service.send(ORGANISATION_UUID,
                new SendOrganisationInvitationsRequestDTO(
                        List.of(new SendOrganisationInvitationsRequestDTO.Recipient("AMINA@example.com", "Amina Y")),
                        List.of(groupUuid), "student", null, null, null, null),
                INVITER_UUID);

        assertThat(result.sent()).hasSize(1);
        assertThat(result.failed()).isEmpty();
    }

    @Test
    void anotherOrganisationsGroupCannotBeInvited() {
        UUID foreignGroup = UUID.randomUUID();
        when(studentGroupLookupService.filterGroupsInOrganisation(eq(ORGANISATION_UUID), any()))
                .thenReturn(List.of());

        SendOrganisationInvitationsRequestDTO request = new SendOrganisationInvitationsRequestDTO(
                null, List.of(foreignGroup), "student", null, null, null, null);

        assertThatThrownBy(() -> service.send(ORGANISATION_UUID, request, INVITER_UUID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("do not belong to your organisation");
    }

    @Test
    void sendingWithNobodyToInviteIsRejected() {
        SendOrganisationInvitationsRequestDTO request = new SendOrganisationInvitationsRequestDTO(
                List.of(), List.of(), "student", null, null, null, null);

        assertThatThrownBy(() -> service.send(ORGANISATION_UUID, request, INVITER_UUID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No one to invite");
    }

    // ================================
    // PARENT INVITATIONS
    // ================================

    @Test
    void aParentInvitationAwaitsTheParentAndEmailsOnlyThem() {
        when(userLookupService.findUserUuidByEmail(any())).thenReturn(Optional.empty());

        SendOrganisationInvitationsResultDTO result = service.sendToParents(ORGANISATION_UUID,
                parentRequest(child("Wanjiru", "Kamau", 9, "Mary@Example.com ")), INVITER_UUID);

        assertThat(result.failed()).isEmpty();
        OrganisationInvitationDTO sent = result.sent().getFirst();
        assertThat(sent.status()).isEqualTo(InvitationStatus.AWAITING_GUARDIAN_CONSENT);
        assertThat(sent.parentInvitation()).isTrue();
        assertThat(sent.recipientEmail()).isNull();
        assertThat(sent.recipientName()).isEqualTo("Wanjiru Kamau");
        assertThat(sent.guardianEmail()).isEqualTo("mary@example.com");
        verify(mappingRepository, never()).save(any());

        verify(invitationRepository).save(org.mockito.ArgumentMatchers.argThat(invitation -> {
            assertThat(invitation.getGuardianConsentTokenHash()).hasSize(64);
            assertThat(invitation.getGuardianRelationshipType()).isEqualTo("PARENT");
            assertThat(invitation.getChildDob()).isNotNull();
            return true;
        }));

        ArgumentCaptor<NotificationRequestedEvent> captor = ArgumentCaptor.forClass(NotificationRequestedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        NotificationRequestedEvent email = captor.getValue();
        assertThat(email.recipientEmail()).isEqualTo("mary@example.com");
        assertThat(email.notificationType()).isEqualTo("GUARDIAN_CONSENT_REQUEST");
        assertThat((String) email.templateVariables().get("consentLink")).contains("guardian-invitations");
        assertThat(email.templateVariables()).doesNotContainValue(null);
    }

    @Test
    void childrenWhoAreAdultsBelowTheAgeGateOrAlreadyInvitedAreReportedOneByOne() {
        when(userLookupService.findUserUuidByEmail(any())).thenReturn(Optional.empty());
        when(ruleEvaluationService.evaluateAgeGate(eq(LocalDate.now().minusYears(2).minusDays(30)), any()))
                .thenReturn(AgeGateDecision.rejected("Learners must be at least 5"));
        when(invitationRepository.findLiveParentInvitation(eq(ORGANISATION_UUID), eq("mary@example.com"),
                eq("Already"), eq("Invited"), any())).thenReturn(Optional.of(liveInvitation()));

        SendOrganisationInvitationsResultDTO result = service.sendToParents(ORGANISATION_UUID, parentRequest(
                child("Grown", "Up", 18, "mary@example.com"),
                child("Too", "Young", 2, "mary@example.com"),
                child("Already", "Invited", 10, "mary@example.com"),
                child("Fine", "Child", 10, "mary@example.com")), INVITER_UUID);

        assertThat(result.sent()).extracting(OrganisationInvitationDTO::recipientName).containsExactly("Fine Child");
        assertThat(result.failed()).extracting(SendOrganisationInvitationsResultDTO.Failure::reason)
                .anySatisfy(reason -> assertThat(reason).contains("Grown Up").contains("18 or over"))
                .anySatisfy(reason -> assertThat(reason).contains("Too Young").contains("at least 5"))
                .anySatisfy(reason -> assertThat(reason).contains("Already Invited").contains("pending invitation"));
    }

    @Test
    void aSponsorCannotBeInvitedToDecideForAChild() {
        SendOrganisationInvitationsResultDTO result = service.sendToParents(ORGANISATION_UUID, parentRequest(
                new SendParentInvitationsRequestDTO.Child("Wanjiru", "Kamau", LocalDate.now().minusYears(9),
                        "mary@example.com", null, "sponsor")), INVITER_UUID);

        assertThat(result.sent()).isEmpty();
        assertThat(result.failed().getFirst().reason()).contains("PARENT or GUARDIAN");
    }

    @Test
    void resendingAParentInvitationReplacesOnlyTheParentsLink() {
        when(userLookupService.findUserUuidByEmail(any())).thenReturn(Optional.empty());
        OrganisationInvitation invitation = liveInvitation();
        invitation.setRecipientEmail(null);
        invitation.setRecipientName("Wanjiru Kamau");
        invitation.setChildDob(LocalDate.now().minusYears(9));
        invitation.setStatus(InvitationStatus.AWAITING_GUARDIAN_CONSENT);
        invitation.setGuardianEmail("mary@example.com");
        invitation.setGuardianConsentTokenHash("old-guardian-hash");
        String childTokenHash = invitation.getTokenHash();
        when(invitationRepository.findByUuid(invitation.getUuid())).thenReturn(Optional.of(invitation));

        service.resend(ORGANISATION_UUID, invitation.getUuid());

        assertThat(invitation.getGuardianConsentTokenHash()).isNotEqualTo("old-guardian-hash");
        assertThat(invitation.getTokenHash()).isEqualTo(childTokenHash);
        ArgumentCaptor<NotificationRequestedEvent> captor = ArgumentCaptor.forClass(NotificationRequestedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().recipientEmail()).isEqualTo("mary@example.com");
        assertThat(captor.getValue().notificationType()).isEqualTo("GUARDIAN_CONSENT_REQUEST");
    }

    private static SendParentInvitationsRequestDTO parentRequest(SendParentInvitationsRequestDTO.Child... children) {
        return new SendParentInvitationsRequestDTO(List.of(children), null, null, null, null);
    }

    private static SendParentInvitationsRequestDTO.Child child(String first, String last, int age, String parentEmail) {
        LocalDate dob = LocalDate.now().minusYears(age).minusDays(30);
        return new SendParentInvitationsRequestDTO.Child(first, last, dob, parentEmail, "Mary Kamau", null);
    }

    private void givenGroupWithMembers(UUID groupUuid, UUID... memberUserUuids) {
        when(studentGroupLookupService.filterGroupsInOrganisation(eq(ORGANISATION_UUID), any()))
                .thenReturn(List.of(groupUuid));
        when(studentGroupMemberRepository.findByGroupUuid(groupUuid))
                .thenReturn(java.util.Arrays.stream(memberUserUuids).map(uuid -> {
                    StudentGroupMember member = new StudentGroupMember();
                    member.setStudentUuid(uuid);
                    return member;
                }).toList());
    }

    private void givenUser(UUID userUuid, String email, String first, String last) {
        User user = new User();
        user.setUuid(userUuid);
        user.setEmail(email);
        user.setFirstName(first);
        user.setLastName(last);
        when(userRepository.findByUuid(userUuid)).thenReturn(Optional.of(user));
    }

    // ================================
    // HELPERS
    // ================================

    private SendOrganisationInvitationsRequestDTO request(String name, String email) {
        return new SendOrganisationInvitationsRequestDTO(
                List.of(new SendOrganisationInvitationsRequestDTO.Recipient(email, name)),
                null, "student", null, null, null, null);
    }

    private OrganisationInvitation liveInvitation() {
        OrganisationInvitation invitation = new OrganisationInvitation();
        invitation.setUuid(UUID.randomUUID());
        invitation.setOrganisationUuid(ORGANISATION_UUID);
        invitation.setDomainUuid(DOMAIN_UUID);
        invitation.setRecipientEmail("jane@example.com");
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setTokenHash(new InvitationTokenService().hash("original-token"));
        invitation.setExpiresAt(LocalDateTime.now().plusDays(14));
        return invitation;
    }
}
