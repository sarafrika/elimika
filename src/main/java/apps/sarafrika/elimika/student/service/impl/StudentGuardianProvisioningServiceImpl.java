package apps.sarafrika.elimika.student.service.impl;

import apps.sarafrika.elimika.notifications.api.NotificationType;
import apps.sarafrika.elimika.shared.event.notification.NotificationRequestedEvent;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.student.dto.GuardianInvitationRegistrationRequestDTO;
import apps.sarafrika.elimika.student.dto.GuardianStudentLinkDTO;
import apps.sarafrika.elimika.student.dto.GuardianStudentLinkRequest;
import apps.sarafrika.elimika.student.dto.MyStudentGuardianInvitationDTO;
import apps.sarafrika.elimika.student.dto.PublicStudentGuardianInvitationDTO;
import apps.sarafrika.elimika.student.dto.StudentGuardianDTO;
import apps.sarafrika.elimika.student.dto.StudentGuardianRequestDTO;
import apps.sarafrika.elimika.student.factory.StudentGuardianLinkFactory;
import apps.sarafrika.elimika.student.internal.GuardianInvitationTokens;
import apps.sarafrika.elimika.student.model.Student;
import apps.sarafrika.elimika.student.model.StudentGuardianContact;
import apps.sarafrika.elimika.student.model.StudentGuardianLink;
import apps.sarafrika.elimika.student.repository.StudentGuardianContactRepository;
import apps.sarafrika.elimika.student.repository.StudentGuardianLinkRepository;
import apps.sarafrika.elimika.student.repository.StudentRepository;
import apps.sarafrika.elimika.student.service.GuardianAccessService;
import apps.sarafrika.elimika.student.service.StudentGuardianProvisioningService;
import apps.sarafrika.elimika.student.util.enums.GuardianContactStatus;
import apps.sarafrika.elimika.student.util.enums.GuardianLinkStatus;
import apps.sarafrika.elimika.student.util.enums.GuardianRelationshipType;
import apps.sarafrika.elimika.student.util.enums.GuardianShareScope;
import apps.sarafrika.elimika.student.util.enums.StudentGuardianState;
import apps.sarafrika.elimika.tenancy.spi.GuardianAccountService;
import apps.sarafrika.elimika.tenancy.spi.GuardianAccountService.InvitedGuardianAccount;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentGuardianProvisioningServiceImpl implements StudentGuardianProvisioningService {

    public static final int MAX_GUARDIANS = 2;
    static final Duration INVITATION_TTL = Duration.ofDays(14);
    private static final Duration RESEND_COOLDOWN = Duration.ofMinutes(1);
    private static final String INVALID_LINK = "This guardian invitation link is not valid.";

    private final StudentGuardianContactRepository contactRepository;
    private final StudentGuardianLinkRepository linkRepository;
    private final StudentRepository studentRepository;
    private final GuardianAccessService guardianAccessService;
    private final UserLookupService userLookupService;
    private final GuardianAccountService guardianAccountService;
    private final GuardianInvitationTokens tokens;
    private final ApplicationEventPublisher eventPublisher;
    private final PlatformTransactionManager transactionManager;

    // ================================
    // STUDENT SIDE
    // ================================

    @Override
    @Transactional
    public void syncGuardians(Student student, List<StudentGuardianRequestDTO> guardians, UUID actorUuid) {
        if (guardians == null) {
            return;
        }
        Map<String, StudentGuardianRequestDTO> wanted = normalise(student, guardians);
        LocalDateTime now = now();

        Map<String, StudentGuardianContact> live = new HashMap<>();
        for (StudentGuardianContact contact : liveContacts(student.getUuid())) {
            if (wanted.containsKey(contact.getGuardianEmail())) {
                live.put(contact.getGuardianEmail(), contact);
            } else {
                remove(contact, now);
            }
        }

        int position = 1;
        for (Map.Entry<String, StudentGuardianRequestDTO> entry : wanted.entrySet()) {
            StudentGuardianContact contact = live.get(entry.getKey());
            if (contact == null) {
                contact = newContact(student, entry.getKey(), actorUuid);
                applyDetails(contact, entry.getValue(), position);
                provision(student, contactRepository.save(contact), actorUuid, now);
            } else {
                // Re-saving a guardian already linked or invited sends nothing again.
                applyDetails(contact, entry.getValue(), position);
                contactRepository.save(contact);
            }
            position++;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudentGuardianDTO> getGuardians(UUID studentUuid) {
        Student student = requireStudent(studentUuid);
        LocalDateTime now = now();
        // An invitee may have been linked another way (e.g. organisation consent); match on email.
        Map<String, StudentGuardianLink> activeByEmail = new HashMap<>();
        for (StudentGuardianLink link : linkRepository.findByStudentUuidAndStatus(student.getUuid(), GuardianLinkStatus.ACTIVE)) {
            String email = userLookupService.getUserEmail(link.getGuardianUserUuid())
                    .map(StudentGuardianProvisioningServiceImpl::normaliseEmail).orElse("user:" + link.getGuardianUserUuid());
            activeByEmail.putIfAbsent(email, link);
        }

        List<StudentGuardianDTO> result = new ArrayList<>();
        for (StudentGuardianContact contact : liveContacts(student.getUuid())) {
            StudentGuardianLink active = activeByEmail.remove(contact.getGuardianEmail());
            result.add(active == null
                    ? toDTO(contact, stateOf(contact, now))
                    : withLink(toDTO(contact, StudentGuardianState.LINKED), active));
        }
        // Guardians linked another way still have access, so they are listed too.
        activeByEmail.values().forEach(link -> result.add(fromLinkOnly(link)));
        return result;
    }

    @Override
    @Transactional
    public StudentGuardianDTO resendInvitation(UUID studentUuid, UUID guardianUuid, UUID actorUuid) {
        Student student = requireStudent(studentUuid);
        StudentGuardianContact contact = contactRepository.findByUuid(guardianUuid)
                .filter(found -> student.getUuid().equals(found.getStudentUuid()))
                .filter(found -> found.getStatus() != GuardianContactStatus.REMOVED)
                .orElseThrow(() -> new ResourceNotFoundException("Guardian %s not found for this student".formatted(guardianUuid)));
        LocalDateTime now = now();

        if (stateOf(contact, now) == StudentGuardianState.LINKED) {
            throw new IllegalStateException("This guardian is already linked to the student.");
        }
        if (contact.getInvitationSentAt() != null
                && contact.getInvitationSentAt().plus(RESEND_COOLDOWN).isAfter(now)) {
            throw new IllegalStateException("An invitation was just sent; please wait a minute before resending.");
        }
        contact.setInvitedBy(actorUuid);
        issueInvitation(student, contact, now);
        return toDTO(contactRepository.save(contact), StudentGuardianState.INVITED);
    }

    // ================================
    // GUARDIAN SIDE
    // ================================

    @Override
    @Transactional(readOnly = true)
    public PublicStudentGuardianInvitationDTO lookupByToken(String rawToken) {
        StudentGuardianContact contact = requireByToken(rawToken);
        Student student = requireStudent(contact.getStudentUuid());
        LocalDateTime now = now();
        return new PublicStudentGuardianInvitationDTO(
                studentName(student),
                contact.getGuardianName(),
                maskEmail(contact.getGuardianEmail()),
                contact.getRelationshipType(),
                stateOf(contact, now),
                contact.isInvitationOpen(now),
                userLookupService.findUserUuidByEmail(contact.getGuardianEmail()).isPresent(),
                contact.getInvitationExpiresAt());
    }

    @Override
    @Transactional
    public GuardianStudentLinkDTO acceptByToken(String rawToken, UUID guardianUserUuid) {
        return accept(requireOpen(requireByToken(rawToken)), guardianUserUuid);
    }

    @Override
    @Transactional
    public void declineByToken(String rawToken) {
        decline(requireOpen(requireByToken(rawToken)));
    }

    @Override
    public GuardianStudentLinkDTO registerAndAccept(String rawToken, GuardianInvitationRegistrationRequestDTO request) {
        // Not transactional: the account is committed by tenancy before the link is written here.
        StudentGuardianContact contact = requireOpen(requireByToken(rawToken));
        String accountExists = "An account already exists for this email. Sign in to accept the invitation.";
        if (userLookupService.findUserUuidByEmail(contact.getGuardianEmail()).isPresent()) {
            throw new IllegalStateException(accountExists);
        }
        UUID guardianUserUuid = guardianAccountService.registerInvitedGuardian(new InvitedGuardianAccount(
                        contact.getGuardianEmail(), request.firstName(), request.lastName(), request.phoneNumber()))
                .orElseThrow(() -> new IllegalStateException(accountExists));

        UUID contactUuid = contact.getUuid();
        return new TransactionTemplate(transactionManager).execute(status -> {
            StudentGuardianContact fresh = requireOpen(contactRepository.findByUuid(contactUuid)
                    .orElseThrow(() -> new ResourceNotFoundException(INVALID_LINK)));
            return link(requireStudent(fresh.getStudentUuid()), fresh, guardianUserUuid, guardianUserUuid, now());
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<MyStudentGuardianInvitationDTO> listOpenInvitationsFor(UUID userUuid) {
        String email = userLookupService.getUserEmail(userUuid).map(StudentGuardianProvisioningServiceImpl::normaliseEmail).orElse(null);
        if (email == null || email.isBlank()) {
            return List.of();
        }
        LocalDateTime now = now();
        return contactRepository.findByGuardianEmailAndStatusIn(email, EnumSet.of(GuardianContactStatus.INVITED)).stream()
                .filter(contact -> contact.isInvitationOpen(now))
                .map(contact -> new MyStudentGuardianInvitationDTO(
                        contact.getUuid(),
                        studentRepository.findByUuid(contact.getStudentUuid()).map(this::studentName).orElse(null),
                        contact.getRelationshipType(),
                        contact.getInvitationExpiresAt()))
                .toList();
    }

    @Override
    @Transactional
    public GuardianStudentLinkDTO acceptByUuid(UUID invitationUuid, UUID guardianUserUuid) {
        return accept(requireOpen(requireByUuid(invitationUuid)), guardianUserUuid);
    }

    @Override
    @Transactional
    public void declineByUuid(UUID invitationUuid, UUID guardianUserUuid) {
        StudentGuardianContact contact = requireOpen(requireByUuid(invitationUuid));
        requireAddressedTo(contact, guardianUserUuid);
        decline(contact);
    }

    // ================================
    // PROVISIONING
    // ================================

    private void provision(Student student, StudentGuardianContact contact, UUID actorUuid, LocalDateTime now) {
        Optional<UUID> existingUser = userLookupService.findUserUuidByEmail(contact.getGuardianEmail());
        if (existingUser.isPresent()) {
            GuardianStudentLinkDTO link = link(student, contact, existingUser.get(), actorUuid, now);
            publishLinkedNotice(student, contact, link.uuid());
        } else {
            issueInvitation(student, contact, now);
        }
    }

    /** Ensures an ACTIVE link (granting {@code parent}, which needs no approval) and marks the contact linked. */
    private GuardianStudentLinkDTO link(Student student, StudentGuardianContact contact, UUID guardianUserUuid,
                                        UUID actorUuid, LocalDateTime now) {
        if (guardianUserUuid.equals(student.getUserUuid())) {
            throw new IllegalArgumentException("A guardian must be someone other than the student.");
        }
        GuardianStudentLinkDTO link = linkRepository
                .findByStudentUuidAndGuardianUserUuidAndStatusIn(student.getUuid(), guardianUserUuid,
                        EnumSet.of(GuardianLinkStatus.ACTIVE))
                .map(active -> StudentGuardianLinkFactory.toDTO(active, studentName(student), contact.getGuardianName()))
                .orElseGet(() -> guardianAccessService.createOrUpdateLink(new GuardianStudentLinkRequest(
                        student.getUuid(),
                        guardianUserUuid,
                        contact.getRelationshipType(),
                        GuardianShareScope.FULL,
                        contact.getPosition() == 1,
                        "Named by the student as a guardian"), actorUuid));

        contact.setStatus(GuardianContactStatus.LINKED);
        contact.setGuardianUserUuid(guardianUserUuid);
        contact.setLinkUuid(link.uuid());
        contact.setLinkedAt(now);
        contact.setTokenHash(null);
        contactRepository.save(contact);
        log.info("Guardian {} linked to student {} from the student's guardian details", guardianUserUuid, student.getUuid());
        return link;
    }

    private void issueInvitation(Student student, StudentGuardianContact contact, LocalDateTime now) {
        String rawToken = tokens.generateRawToken();
        contact.setStatus(GuardianContactStatus.INVITED);
        contact.setTokenHash(tokens.hash(rawToken));
        contact.setInvitationExpiresAt(now.plus(INVITATION_TTL));
        contact.setInvitationSentAt(now);
        contact.setInvitationSendCount(contact.getInvitationSendCount() + 1);
        contact.setDeclinedAt(null);
        contactRepository.save(contact);

        Map<String, Object> variables = baseVariables(student, contact);
        variables.put("actionLink", tokens.invitationLink(rawToken));
        variables.put("expiresAt", contact.getInvitationExpiresAt().format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)));
        eventPublisher.publishEvent(NotificationRequestedEvent.email(
                null,
                contact.getGuardianEmail(),
                contact.getGuardianName(),
                NotificationType.GUARDIAN_LINK_INVITATION.getValue(),
                variables));
        log.info("Guardian invitation {} sent for student {}", contact.getUuid(), student.getUuid());
    }

    private void publishLinkedNotice(Student student, StudentGuardianContact contact, UUID linkUuid) {
        Map<String, Object> variables = baseVariables(student, contact);
        variables.put("actionLink", tokens.guardianDashboardLink());
        String name = studentName(student);
        eventPublisher.publishEvent(new NotificationRequestedEvent(
                null,
                contact.getGuardianUserUuid(),
                contact.getGuardianEmail(),
                contact.getGuardianName(),
                NotificationType.GUARDIAN_LINK_ESTABLISHED.getValue(),
                "NORMAL",
                "INBOX",
                "You can now follow " + name + "'s learning",
                name + " named you as their " + relationshipLabel(contact) + ".",
                tokens.guardianDashboardLink(),
                variables,
                Set.of("in_app", "email"),
                "guardian-link-established:" + linkUuid,
                null,
                null,
                null));
    }

    private GuardianStudentLinkDTO accept(StudentGuardianContact contact, UUID guardianUserUuid) {
        requireAddressedTo(contact, guardianUserUuid);
        return link(requireStudent(contact.getStudentUuid()), contact, guardianUserUuid, guardianUserUuid, now());
    }

    private void decline(StudentGuardianContact contact) {
        contact.setStatus(GuardianContactStatus.DECLINED);
        contact.setDeclinedAt(now());
        contact.setTokenHash(null);
        contactRepository.save(contact);
        log.info("Guardian invitation {} declined", contact.getUuid());
    }

    /** Retracts a pending invitation; an active link is left alone and is revoked through the guardian links API. */
    private void remove(StudentGuardianContact contact, LocalDateTime now) {
        contact.setStatus(GuardianContactStatus.REMOVED);
        contact.setRemovedAt(now);
        contact.setTokenHash(null);
        contactRepository.save(contact);
    }

    // ================================
    // HELPERS
    // ================================

    private Map<String, StudentGuardianRequestDTO> normalise(Student student, List<StudentGuardianRequestDTO> guardians) {
        if (guardians.size() > MAX_GUARDIANS) {
            throw new IllegalArgumentException("A student can name at most " + MAX_GUARDIANS + " guardians.");
        }
        String studentEmail = userLookupService.getUserEmail(student.getUserUuid())
                .map(StudentGuardianProvisioningServiceImpl::normaliseEmail).orElse(null);
        Map<String, StudentGuardianRequestDTO> wanted = new LinkedHashMap<>();
        for (StudentGuardianRequestDTO guardian : guardians) {
            if (guardian == null || guardian.email() == null || guardian.email().isBlank()) {
                throw new IllegalArgumentException("Every guardian needs an email address.");
            }
            String email = normaliseEmail(guardian.email());
            if (email.equals(studentEmail)) {
                throw new IllegalArgumentException("A guardian must be someone other than the student.");
            }
            if (wanted.putIfAbsent(email, guardian) != null) {
                throw new IllegalArgumentException("Each guardian must have a different email address.");
            }
        }
        return wanted;
    }

    private StudentGuardianContact newContact(Student student, String email, UUID actorUuid) {
        StudentGuardianContact contact = new StudentGuardianContact();
        contact.setStudentUuid(student.getUuid());
        contact.setGuardianEmail(email);
        contact.setStatus(GuardianContactStatus.INVITED);
        contact.setInvitedBy(actorUuid);
        return contact;
    }

    private static void applyDetails(StudentGuardianContact contact, StudentGuardianRequestDTO guardian, int position) {
        contact.setGuardianName(guardian.name() == null ? null : guardian.name().trim());
        contact.setGuardianPhone(guardian.phone() == null || guardian.phone().isBlank() ? null : guardian.phone().trim());
        contact.setRelationshipType(relationshipType(guardian.relationshipType()));
        contact.setPosition(position);
    }

    private StudentGuardianState stateOf(StudentGuardianContact contact, LocalDateTime now) {
        return switch (contact.getStatus()) {
            case LINKED -> contact.getLinkUuid() != null && linkRepository.findByUuid(contact.getLinkUuid())
                    .map(link -> link.getStatus() == GuardianLinkStatus.ACTIVE).orElse(false)
                    ? StudentGuardianState.LINKED
                    : StudentGuardianState.REVOKED;
            case INVITED -> contact.isInvitationOpen(now) ? StudentGuardianState.INVITED : StudentGuardianState.EXPIRED;
            case DECLINED -> StudentGuardianState.DECLINED;
            case REMOVED -> StudentGuardianState.REVOKED;
        };
    }

    private StudentGuardianDTO toDTO(StudentGuardianContact contact, StudentGuardianState state) {
        return new StudentGuardianDTO(
                contact.getUuid(),
                contact.getStudentUuid(),
                contact.getGuardianName(),
                contact.getGuardianEmail(),
                contact.getGuardianPhone(),
                contact.getRelationshipType(),
                state,
                contact.getGuardianUserUuid(),
                contact.getLinkUuid(),
                contact.getInvitationSentAt(),
                contact.getInvitationExpiresAt(),
                contact.getLinkedAt());
    }

    private static StudentGuardianDTO withLink(StudentGuardianDTO dto, StudentGuardianLink link) {
        return new StudentGuardianDTO(dto.uuid(), dto.studentUuid(), dto.name(), dto.email(), dto.phone(),
                dto.relationshipType(), StudentGuardianState.LINKED, link.getGuardianUserUuid(), link.getUuid(),
                dto.invitationSentAt(), dto.invitationExpiresAt(),
                dto.linkedAt() != null ? dto.linkedAt() : link.getLinkedDate());
    }

    private StudentGuardianDTO fromLinkOnly(StudentGuardianLink link) {
        return new StudentGuardianDTO(
                null,
                link.getStudentUuid(),
                userLookupService.getUserFullName(link.getGuardianUserUuid()).orElse(null),
                userLookupService.getUserEmail(link.getGuardianUserUuid()).orElse(null),
                null,
                link.getRelationshipType(),
                StudentGuardianState.LINKED,
                link.getGuardianUserUuid(),
                link.getUuid(),
                null,
                null,
                link.getLinkedDate());
    }

    private Map<String, Object> baseVariables(Student student, StudentGuardianContact contact) {
        // Template variables are copied into an immutable map downstream, which rejects nulls.
        Map<String, Object> variables = new HashMap<>();
        variables.put("guardianName", contact.getGuardianName() == null ? "there" : contact.getGuardianName());
        variables.put("studentName", studentName(student));
        variables.put("relationshipLabel", relationshipLabel(contact));
        return variables;
    }

    private String studentName(Student student) {
        if (student.getFullName() != null && !student.getFullName().isBlank()) {
            return student.getFullName();
        }
        return userLookupService.getUserFullName(student.getUserUuid()).filter(name -> !name.isBlank()).orElse("A learner");
    }

    private static String relationshipLabel(StudentGuardianContact contact) {
        return contact.getRelationshipType() == null
                ? "guardian"
                : contact.getRelationshipType().name().toLowerCase(Locale.ROOT);
    }

    private List<StudentGuardianContact> liveContacts(UUID studentUuid) {
        return contactRepository.findByStudentUuidAndStatusNotOrderByPositionAsc(studentUuid, GuardianContactStatus.REMOVED);
    }

    private Student requireStudent(UUID studentUuid) {
        return studentRepository.findByUuid(studentUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Student with UUID %s not found".formatted(studentUuid)));
    }

    private StudentGuardianContact requireByToken(String rawToken) {
        return contactRepository.findByTokenHash(tokens.hash(rawToken))
                .orElseThrow(() -> new ResourceNotFoundException(INVALID_LINK));
    }

    private StudentGuardianContact requireByUuid(UUID invitationUuid) {
        return contactRepository.findByUuid(invitationUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Guardian invitation %s not found".formatted(invitationUuid)));
    }

    private StudentGuardianContact requireOpen(StudentGuardianContact contact) {
        if (!contact.isInvitationOpen(now())) {
            throw new IllegalStateException("This guardian invitation is no longer open ("
                    + stateOf(contact, now()).getValue() + ").");
        }
        return contact;
    }

    /** An invitation is addressed to an email, not to whoever holds the link. */
    private void requireAddressedTo(StudentGuardianContact contact, UUID userUuid) {
        String email = userLookupService.getUserEmail(userUuid).map(StudentGuardianProvisioningServiceImpl::normaliseEmail).orElse(null);
        if (!Objects.equals(email, contact.getGuardianEmail())) {
            log.warn("User {} attempted to accept guardian invitation {} addressed to someone else", userUuid, contact.getUuid());
            throw new AccessDeniedException("This invitation was sent to a different email address.");
        }
    }

    private static GuardianRelationshipType relationshipType(String value) {
        if (value == null || value.isBlank()) {
            return GuardianRelationshipType.GUARDIAN;
        }
        return GuardianRelationshipType.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }

    static String normaliseEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        String local = email.substring(0, at);
        String domain = email.substring(at);
        return local.length() <= 2
                ? local.charAt(0) + "***" + domain
                : local.charAt(0) + "***" + local.charAt(local.length() - 1) + domain;
    }

    private static LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
