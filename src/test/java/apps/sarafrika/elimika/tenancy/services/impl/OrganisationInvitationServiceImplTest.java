package apps.sarafrika.elimika.tenancy.services.impl;

import apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService;
import apps.sarafrika.elimika.tenancy.dto.SendOrganisationInvitationsRequestDTO;
import apps.sarafrika.elimika.tenancy.dto.SendOrganisationInvitationsResultDTO;
import apps.sarafrika.elimika.tenancy.entity.Organisation;
import apps.sarafrika.elimika.tenancy.entity.OrganisationInvitation;
import apps.sarafrika.elimika.tenancy.entity.UserDomain;
import apps.sarafrika.elimika.tenancy.internal.InvitationTokenService;
import apps.sarafrika.elimika.tenancy.repository.OrganisationInvitationClassRepository;
import apps.sarafrika.elimika.tenancy.repository.OrganisationInvitationRepository;
import apps.sarafrika.elimika.tenancy.repository.OrganisationRepository;
import apps.sarafrika.elimika.tenancy.repository.TrainingBranchRepository;
import apps.sarafrika.elimika.tenancy.repository.UserDomainRepository;
import apps.sarafrika.elimika.tenancy.repository.UserOrganisationDomainMappingRepository;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import apps.sarafrika.elimika.tenancy.util.enums.InvitationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

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
                classDefinitionLookupService,
                userLookupService,
                new InvitationTokenService());

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
    void sendRefusesSomeoneWhoIsAlreadyAMember() {
        UUID existingUser = UUID.randomUUID();
        when(userLookupService.findUserUuidByEmail("jane@example.com")).thenReturn(Optional.of(existingUser));
        when(mappingRepository.existsByUserUuidAndOrganisationUuidAndActiveTrueAndDeletedFalse(
                existingUser, ORGANISATION_UUID)).thenReturn(true);

        SendOrganisationInvitationsResultDTO result =
                service.send(ORGANISATION_UUID, request("Jane Doe", "jane@example.com"), INVITER_UUID);

        assertThat(result.sent()).isEmpty();
        assertThat(result.failed()).singleElement().satisfies(failure -> {
            assertThat(failure.email()).isEqualTo("jane@example.com");
            assertThat(failure.reason()).contains("already a member");
        });
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
                "student", null, null, null, null);

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
                "student", null, null, null, null);

        SendOrganisationInvitationsResultDTO result = service.send(ORGANISATION_UUID, request, INVITER_UUID);

        assertThat(result.sent()).hasSize(1);
        assertThat(result.failed()).singleElement()
                .satisfies(failure -> assertThat(failure.reason()).contains("more than once"));
    }

    @Test
    void sendRejectsClassesBelongingToAnotherOrganisation() {
        UUID foreignClass = UUID.randomUUID();
        when(classDefinitionLookupService.findClassDefinitionUuidsByOrganisationUuid(ORGANISATION_UUID))
                .thenReturn(List.of(UUID.randomUUID()));

        SendOrganisationInvitationsRequestDTO request = new SendOrganisationInvitationsRequestDTO(
                List.of(new SendOrganisationInvitationsRequestDTO.Recipient("jane@example.com", "Jane")),
                "student", null, List.of(foreignClass), null, null);

        assertThatThrownBy(() -> service.send(ORGANISATION_UUID, request, INVITER_UUID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("do not belong to your organisation");
    }

    @Test
    void sendRejectsADomainOrganisationsMayNotInviteInto() {
        SendOrganisationInvitationsRequestDTO request = new SendOrganisationInvitationsRequestDTO(
                List.of(new SendOrganisationInvitationsRequestDTO.Recipient("jane@example.com", "Jane")),
                "course_creator", null, null, null, null);

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
    // HELPERS
    // ================================

    private SendOrganisationInvitationsRequestDTO request(String name, String email) {
        return new SendOrganisationInvitationsRequestDTO(
                List.of(new SendOrganisationInvitationsRequestDTO.Recipient(email, name)),
                "student", null, null, null, null);
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
