package apps.sarafrika.elimika.student.service.impl;

import apps.sarafrika.elimika.shared.event.notification.NotificationRequestedEvent;
import apps.sarafrika.elimika.student.dto.GuardianStudentLinkDTO;
import apps.sarafrika.elimika.student.dto.GuardianStudentLinkRequest;
import apps.sarafrika.elimika.student.dto.StudentGuardianRequestDTO;
import apps.sarafrika.elimika.student.internal.GuardianInvitationTokens;
import apps.sarafrika.elimika.student.model.Student;
import apps.sarafrika.elimika.student.model.StudentGuardianContact;
import apps.sarafrika.elimika.student.model.StudentGuardianLink;
import apps.sarafrika.elimika.student.repository.StudentGuardianContactRepository;
import apps.sarafrika.elimika.student.repository.StudentGuardianLinkRepository;
import apps.sarafrika.elimika.student.repository.StudentRepository;
import apps.sarafrika.elimika.student.service.GuardianAccessService;
import apps.sarafrika.elimika.student.util.enums.GuardianContactStatus;
import apps.sarafrika.elimika.student.util.enums.GuardianLinkStatus;
import apps.sarafrika.elimika.student.util.enums.GuardianRelationshipType;
import apps.sarafrika.elimika.student.util.enums.GuardianShareScope;
import apps.sarafrika.elimika.tenancy.spi.GuardianAccountService;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentGuardianProvisioningServiceImplTest {

    @Mock private StudentGuardianContactRepository contactRepository;
    @Mock private StudentGuardianLinkRepository linkRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private GuardianAccessService guardianAccessService;
    @Mock private UserLookupService userLookupService;
    @Mock private GuardianAccountService guardianAccountService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private PlatformTransactionManager transactionManager;

    private final GuardianInvitationTokens tokens = new GuardianInvitationTokens("https://elimika.test/");
    private StudentGuardianProvisioningServiceImpl service;
    private Student student;
    private final UUID actor = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new StudentGuardianProvisioningServiceImpl(contactRepository, linkRepository, studentRepository,
                guardianAccessService, userLookupService, guardianAccountService, tokens, eventPublisher,
                transactionManager);
        student = new Student();
        student.setUuid(UUID.randomUUID());
        student.setUserUuid(UUID.randomUUID());
        student.setFullName("Sam Doe");
        lenient().when(userLookupService.getUserEmail(student.getUserUuid())).thenReturn(Optional.of("sam@example.com"));
        lenient().when(contactRepository.save(any(StudentGuardianContact.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(contactRepository.findByStudentUuidAndStatusNotOrderByPositionAsc(student.getUuid(), GuardianContactStatus.REMOVED))
                .thenReturn(List.of());
    }

    @Test
    void existingAccountIsLinkedAtOnceAndNotified() {
        UUID guardianUser = UUID.randomUUID();
        UUID linkUuid = UUID.randomUUID();
        when(userLookupService.findUserUuidByEmail("mary@example.com")).thenReturn(Optional.of(guardianUser));
        when(linkRepository.findByStudentUuidAndGuardianUserUuidAndStatusIn(eq(student.getUuid()), eq(guardianUser), any()))
                .thenReturn(Optional.empty());
        when(guardianAccessService.createOrUpdateLink(any(), eq(actor))).thenReturn(linkDto(linkUuid, guardianUser));

        service.syncGuardians(student, List.of(guardian("Mary Doe", " Mary@Example.com ")), actor);

        ArgumentCaptor<GuardianStudentLinkRequest> request = ArgumentCaptor.forClass(GuardianStudentLinkRequest.class);
        verify(guardianAccessService).createOrUpdateLink(request.capture(), eq(actor));
        assertThat(request.getValue().studentUuid()).isEqualTo(student.getUuid());
        assertThat(request.getValue().guardianUserUuid()).isEqualTo(guardianUser);
        assertThat(request.getValue().relationshipType()).isEqualTo(GuardianRelationshipType.PARENT);
        assertThat(request.getValue().isPrimary()).isTrue();

        StudentGuardianContact saved = lastSavedContact();
        assertThat(saved.getGuardianEmail()).isEqualTo("mary@example.com");
        assertThat(saved.getStatus()).isEqualTo(GuardianContactStatus.LINKED);
        assertThat(saved.getLinkUuid()).isEqualTo(linkUuid);
        assertThat(saved.getTokenHash()).isNull();

        NotificationRequestedEvent event = publishedNotification();
        assertThat(event.notificationType()).isEqualTo("GUARDIAN_LINK_ESTABLISHED");
        assertThat(event.recipientId()).isEqualTo(guardianUser);
        assertThat(event.deliveryChannels()).containsExactlyInAnyOrder("in_app", "email");
    }

    @Test
    void unknownEmailIsInvitedWithAHashedToken() {
        when(userLookupService.findUserUuidByEmail("new@example.com")).thenReturn(Optional.empty());

        service.syncGuardians(student, List.of(guardian("New Parent", "new@example.com")), actor);

        StudentGuardianContact saved = lastSavedContact();
        assertThat(saved.getStatus()).isEqualTo(GuardianContactStatus.INVITED);
        assertThat(saved.getTokenHash()).hasSize(64);
        assertThat(saved.getInvitationSendCount()).isEqualTo(1);
        assertThat(saved.getInvitationExpiresAt()).isAfter(LocalDateTime.now(ZoneOffset.UTC).plusDays(13));
        verify(guardianAccessService, never()).createOrUpdateLink(any(), any());

        NotificationRequestedEvent event = publishedNotification();
        assertThat(event.notificationType()).isEqualTo("GUARDIAN_LINK_INVITATION");
        assertThat(event.recipientEmail()).isEqualTo("new@example.com");
        assertThat((String) event.templateVariables().get("actionLink")).startsWith("https://elimika.test/guardian-links/");
    }

    @Test
    void resavingTheSameGuardianSendsNothingAgain() {
        StudentGuardianContact invited = contact("new@example.com", GuardianContactStatus.INVITED);
        when(contactRepository.findByStudentUuidAndStatusNotOrderByPositionAsc(student.getUuid(), GuardianContactStatus.REMOVED))
                .thenReturn(List.of(invited));

        service.syncGuardians(student, List.of(guardian("Renamed Parent", "NEW@example.com")), actor);

        assertThat(invited.getStatus()).isEqualTo(GuardianContactStatus.INVITED);
        assertThat(invited.getGuardianName()).isEqualTo("Renamed Parent");
        assertThat(invited.getInvitationSendCount()).isEqualTo(1);
        verify(userLookupService, never()).findUserUuidByEmail(anyString());
        verify(guardianAccessService, never()).createOrUpdateLink(any(), any());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void existingActiveLinkIsReusedNotRecreated() {
        UUID guardianUser = UUID.randomUUID();
        StudentGuardianLink active = new StudentGuardianLink();
        active.setUuid(UUID.randomUUID());
        active.setStudentUuid(student.getUuid());
        active.setGuardianUserUuid(guardianUser);
        active.setStatus(GuardianLinkStatus.ACTIVE);
        active.setShareScope(GuardianShareScope.FULL);
        when(userLookupService.findUserUuidByEmail("mary@example.com")).thenReturn(Optional.of(guardianUser));
        when(linkRepository.findByStudentUuidAndGuardianUserUuidAndStatusIn(eq(student.getUuid()), eq(guardianUser), any()))
                .thenReturn(Optional.of(active));

        service.syncGuardians(student, List.of(guardian("Mary Doe", "mary@example.com")), actor);

        verify(guardianAccessService, never()).createOrUpdateLink(any(), any());
        assertThat(lastSavedContact().getLinkUuid()).isEqualTo(active.getUuid());
    }

    @Test
    void removingGuardiansRetractsInvitesButLeavesActiveLinks() {
        StudentGuardianContact invited = contact("pending@example.com", GuardianContactStatus.INVITED);
        StudentGuardianContact linked = contact("linked@example.com", GuardianContactStatus.LINKED);
        linked.setTokenHash(null);
        linked.setLinkUuid(UUID.randomUUID());
        when(contactRepository.findByStudentUuidAndStatusNotOrderByPositionAsc(student.getUuid(), GuardianContactStatus.REMOVED))
                .thenReturn(List.of(invited, linked));

        service.syncGuardians(student, List.of(), actor);

        assertThat(invited.getStatus()).isEqualTo(GuardianContactStatus.REMOVED);
        assertThat(invited.getTokenHash()).isNull();
        assertThat(linked.getStatus()).isEqualTo(GuardianContactStatus.REMOVED);
        verify(guardianAccessService, never()).revokeLink(any(), any(), any());
    }

    @Test
    void nullGuardiansLeaveEverythingUntouched() {
        service.syncGuardians(student, null, actor);

        verify(contactRepository, never()).findByStudentUuidAndStatusNotOrderByPositionAsc(any(), any());
    }

    @Test
    void rejectsTooManyDuplicateOrSelfGuardians() {
        assertThatThrownBy(() -> service.syncGuardians(student, List.of(
                guardian("A", "a@example.com"), guardian("B", "b@example.com"), guardian("C", "c@example.com")), actor))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.syncGuardians(student, List.of(
                guardian("A", "a@example.com"), guardian("A again", "A@example.com")), actor))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.syncGuardians(student, List.of(guardian("Me", "Sam@example.com")), actor))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void acceptingRequiresTheInvitedEmail() {
        String rawToken = tokens.generateRawToken();
        StudentGuardianContact invited = contact("new@example.com", GuardianContactStatus.INVITED);
        invited.setTokenHash(tokens.hash(rawToken));
        UUID stranger = UUID.randomUUID();
        when(contactRepository.findByTokenHash(tokens.hash(rawToken))).thenReturn(Optional.of(invited));
        when(userLookupService.getUserEmail(stranger)).thenReturn(Optional.of("someone@else.com"));

        assertThatThrownBy(() -> service.acceptByToken(rawToken, stranger)).isInstanceOf(AccessDeniedException.class);
        verify(guardianAccessService, never()).createOrUpdateLink(any(), any());
    }

    @Test
    void acceptingWithTheInvitedEmailLinksTheGuardian() {
        String rawToken = tokens.generateRawToken();
        StudentGuardianContact invited = contact("new@example.com", GuardianContactStatus.INVITED);
        invited.setTokenHash(tokens.hash(rawToken));
        UUID guardianUser = UUID.randomUUID();
        UUID linkUuid = UUID.randomUUID();
        when(contactRepository.findByTokenHash(tokens.hash(rawToken))).thenReturn(Optional.of(invited));
        when(userLookupService.getUserEmail(guardianUser)).thenReturn(Optional.of("New@Example.com"));
        when(studentRepository.findByUuid(student.getUuid())).thenReturn(Optional.of(student));
        when(linkRepository.findByStudentUuidAndGuardianUserUuidAndStatusIn(eq(student.getUuid()), eq(guardianUser), any()))
                .thenReturn(Optional.empty());
        when(guardianAccessService.createOrUpdateLink(any(), eq(guardianUser))).thenReturn(linkDto(linkUuid, guardianUser));

        GuardianStudentLinkDTO link = service.acceptByToken(rawToken, guardianUser);

        assertThat(link.uuid()).isEqualTo(linkUuid);
        assertThat(invited.getStatus()).isEqualTo(GuardianContactStatus.LINKED);
        assertThat(invited.getTokenHash()).isNull();
    }

    @Test
    void expiredInvitationCannotBeAccepted() {
        String rawToken = tokens.generateRawToken();
        StudentGuardianContact invited = contact("new@example.com", GuardianContactStatus.INVITED);
        invited.setTokenHash(tokens.hash(rawToken));
        invited.setInvitationExpiresAt(LocalDateTime.now(ZoneOffset.UTC).minusDays(1));
        when(contactRepository.findByTokenHash(tokens.hash(rawToken))).thenReturn(Optional.of(invited));

        assertThatThrownBy(() -> service.acceptByToken(rawToken, UUID.randomUUID()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("expired");
    }

    private StudentGuardianRequestDTO guardian(String name, String email) {
        return new StudentGuardianRequestDTO(name, email, null, "parent");
    }

    private StudentGuardianContact contact(String email, GuardianContactStatus status) {
        StudentGuardianContact contact = new StudentGuardianContact();
        contact.setUuid(UUID.randomUUID());
        contact.setStudentUuid(student.getUuid());
        contact.setGuardianEmail(email);
        contact.setGuardianName("Existing");
        contact.setRelationshipType(GuardianRelationshipType.PARENT);
        contact.setPosition(1);
        contact.setStatus(status);
        contact.setTokenHash("a".repeat(64));
        contact.setInvitationExpiresAt(LocalDateTime.now(ZoneOffset.UTC).plusDays(7));
        contact.setInvitationSentAt(LocalDateTime.now(ZoneOffset.UTC).minusDays(7));
        contact.setInvitationSendCount(1);
        return contact;
    }

    private GuardianStudentLinkDTO linkDto(UUID linkUuid, UUID guardianUser) {
        return new GuardianStudentLinkDTO(linkUuid, student.getUuid(), guardianUser, "Sam Doe", "Mary Doe",
                GuardianRelationshipType.PARENT, GuardianShareScope.FULL, GuardianLinkStatus.ACTIVE, true,
                LocalDateTime.now(ZoneOffset.UTC), null, null);
    }

    private StudentGuardianContact lastSavedContact() {
        ArgumentCaptor<StudentGuardianContact> captor = ArgumentCaptor.forClass(StudentGuardianContact.class);
        verify(contactRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        return captor.getValue();
    }

    private NotificationRequestedEvent publishedNotification() {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(captor.capture());
        return (NotificationRequestedEvent) captor.getValue();
    }
}
