package apps.sarafrika.elimika.tenancy.services.impl;

import apps.sarafrika.elimika.notifications.api.NotificationType;
import apps.sarafrika.elimika.shared.event.notification.NotificationRequestedEvent;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.shared.spi.ClassDefinitionLookupService;
import apps.sarafrika.elimika.tenancy.dto.OrganisationInvitationDTO;
import apps.sarafrika.elimika.tenancy.dto.SendOrganisationInvitationsRequestDTO;
import apps.sarafrika.elimika.tenancy.dto.SendOrganisationInvitationsResultDTO;
import apps.sarafrika.elimika.tenancy.entity.Organisation;
import apps.sarafrika.elimika.tenancy.entity.OrganisationInvitation;
import apps.sarafrika.elimika.tenancy.entity.OrganisationInvitationClass;
import apps.sarafrika.elimika.tenancy.entity.User;
import apps.sarafrika.elimika.tenancy.entity.UserDomain;
import apps.sarafrika.elimika.tenancy.factory.OrganisationInvitationFactory;
import apps.sarafrika.elimika.tenancy.internal.InvitationLinkFactory;
import apps.sarafrika.elimika.tenancy.internal.InvitationTokenService;
import apps.sarafrika.elimika.tenancy.repository.OrganisationInvitationClassRepository;
import apps.sarafrika.elimika.tenancy.repository.OrganisationInvitationRepository;
import apps.sarafrika.elimika.tenancy.repository.OrganisationRepository;
import apps.sarafrika.elimika.tenancy.repository.TrainingBranchRepository;
import apps.sarafrika.elimika.tenancy.repository.UserDomainRepository;
import apps.sarafrika.elimika.tenancy.repository.UserOrganisationDomainMappingRepository;
import apps.sarafrika.elimika.tenancy.repository.UserRepository;
import apps.sarafrika.elimika.tenancy.services.OrganisationInvitationService;
import apps.sarafrika.elimika.tenancy.spi.UserLookupService;
import apps.sarafrika.elimika.tenancy.util.enums.InvitationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Default {@link OrganisationInvitationService}.
 * <p>
 * Sending an invitation writes nothing but the offer itself. Account provisioning and
 * affiliation are deliberately absent here - they belong to the acceptance path, where
 * the invitee actually consents.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2026-07-28
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrganisationInvitationServiceImpl implements OrganisationInvitationService {

    private static final int DEFAULT_EXPIRY_DAYS = 14;

    /**
     * Domains an organisation may invite into. Mirrors the set accepted by
     * {@code OrganisationController.setOrganisationUserDomain} and deliberately excludes
     * platform-only domains such as {@code course_creator} and {@code parent}.
     */
    private static final Set<String> ORG_SCOPED_DOMAINS =
            Set.of("organisation_user", "admin", "instructor", "student");

    private final OrganisationInvitationRepository invitationRepository;
    private final OrganisationInvitationClassRepository invitationClassRepository;
    private final OrganisationRepository organisationRepository;
    private final UserDomainRepository userDomainRepository;
    private final TrainingBranchRepository trainingBranchRepository;
    private final UserOrganisationDomainMappingRepository mappingRepository;
    private final UserRepository userRepository;
    private final ClassDefinitionLookupService classDefinitionLookupService;
    private final UserLookupService userLookupService;
    private final InvitationTokenService tokenService;
    private final InvitationLinkFactory linkFactory;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public SendOrganisationInvitationsResultDTO send(UUID organisationUuid,
                                                     SendOrganisationInvitationsRequestDTO request,
                                                     UUID inviterUserUuid) {
        requireOrganisation(organisationUuid);
        UserDomain domain = resolveOrgScopedDomain(request.domainName());
        UUID branchUuid = validateBranch(organisationUuid, request.branchUuid());
        List<UUID> classUuids = validateClasses(organisationUuid, request.classUuids());

        int expiryDays = request.expiresInDays() == null ? DEFAULT_EXPIRY_DAYS : request.expiresInDays();
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(expiryDays);

        List<OrganisationInvitationDTO> sent = new ArrayList<>();
        List<SendOrganisationInvitationsResultDTO.Failure> failed = new ArrayList<>();
        Set<String> seenInThisBatch = new HashSet<>();

        for (SendOrganisationInvitationsRequestDTO.Recipient recipient : request.recipients()) {
            String email = normaliseEmail(recipient.email());
            try {
                if (!seenInThisBatch.add(email)) {
                    throw new IllegalStateException("This address appears more than once in the batch.");
                }
                OrganisationInvitation invitation =
                        createInvitation(organisationUuid, domain, branchUuid, classUuids,
                                email, recipient.name(), request.message(), expiresAt, inviterUserUuid);
                sent.add(OrganisationInvitationFactory.toDTO(invitation, domain.getDomainName(), classUuids));
            } catch (IllegalStateException | IllegalArgumentException e) {
                log.debug("Skipping invitation for {} in organisation {}: {}", email, organisationUuid, e.getMessage());
                failed.add(new SendOrganisationInvitationsResultDTO.Failure(email, e.getMessage()));
            }
        }

        log.info("Organisation {} invited {} recipient(s) into domain {}; {} skipped",
                organisationUuid, sent.size(), domain.getDomainName(), failed.size());
        return new SendOrganisationInvitationsResultDTO(sent, failed);
    }

    /**
     * Creates a single invitation, refusing anyone who is already affiliated or already
     * holds a live offer from this organisation.
     */
    private OrganisationInvitation createInvitation(UUID organisationUuid,
                                                    UserDomain domain,
                                                    UUID branchUuid,
                                                    List<UUID> classUuids,
                                                    String email,
                                                    String name,
                                                    String message,
                                                    LocalDateTime expiresAt,
                                                    UUID inviterUserUuid) {
        if (invitationRepository.findLiveByOrganisationAndEmail(organisationUuid, email).isPresent()) {
            throw new IllegalStateException("This address already has a pending invitation to your organisation.");
        }

        // Resolved server-side: whether the invitee already has an account decides which
        // journey the acceptance page shows, and is never something the sender declares.
        UUID recipientUserUuid = userLookupService.findUserUuidByEmail(email).orElse(null);
        if (recipientUserUuid != null
                && mappingRepository.existsByUserUuidAndOrganisationUuidAndActiveTrueAndDeletedFalse(
                        recipientUserUuid, organisationUuid)) {
            throw new IllegalStateException("This person is already a member of your organisation.");
        }

        String rawToken = tokenService.generateRawToken();
        OrganisationInvitation invitation = new OrganisationInvitation();
        invitation.setTokenHash(tokenService.hash(rawToken));
        invitation.setOrganisationUuid(organisationUuid);
        invitation.setBranchUuid(branchUuid);
        invitation.setDomainUuid(domain.getUuid());
        invitation.setRecipientEmail(email);
        invitation.setRecipientName(trimToNull(name));
        invitation.setRecipientUserUuid(recipientUserUuid);
        invitation.setInviterUserUuid(inviterUserUuid);
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setMessage(trimToNull(message));
        invitation.setExpiresAt(expiresAt);

        OrganisationInvitation saved = invitationRepository.save(invitation);
        persistClasses(saved.getUuid(), classUuids);
        publishInvitationEmail(saved, rawToken, domain.getDomainName());
        return saved;
    }

    /**
     * Emails the offer. The raw token leaves the application only here - it is not stored,
     * so this link cannot be rebuilt afterwards.
     */
    private void publishInvitationEmail(OrganisationInvitation invitation, String rawToken, String domainName) {
        // Template variables are copied into an immutable map downstream, which rejects
        // nulls outright - so every value here must be non-null.
        Map<String, Object> variables = new HashMap<>();
        variables.put("organisationName", orElse(organisationName(invitation.getOrganisationUuid()), "An organisation"));
        variables.put("inviterName", orElse(userFullName(invitation.getInviterUserUuid()), ""));
        variables.put("recipientName", orElse(invitation.getRecipientName(), "there"));
        variables.put("roleName", orElse(domainName, "member"));
        variables.put("personalMessage", orElse(invitation.getMessage(), ""));
        variables.put("invitationLink", linkFactory.invitationLink(rawToken));
        variables.put("existingUser", invitation.getRecipientUserUuid() != null);
        variables.put("expiresAt", invitation.getExpiresAt() == null ? "" : invitation.getExpiresAt().toString());

        eventPublisher.publishEvent(NotificationRequestedEvent.email(
                invitation.getRecipientUserUuid(),
                invitation.getRecipientEmail(),
                invitation.getRecipientName(),
                NotificationType.ORGANISATION_INVITATION.getValue(),
                variables));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganisationInvitationDTO> listForOrganisation(UUID organisationUuid,
                                                                Collection<InvitationStatus> statuses) {
        requireOrganisation(organisationUuid);

        List<OrganisationInvitation> invitations = (statuses == null || statuses.isEmpty())
                ? invitationRepository.findByOrganisationUuidOrderByCreatedDateDesc(organisationUuid)
                : invitationRepository.findByOrganisationUuidAndStatusInOrderByCreatedDateDesc(organisationUuid, statuses);

        Map<UUID, List<UUID>> classesByInvitation = loadClasses(
                invitations.stream().map(OrganisationInvitation::getUuid).toList());
        Map<UUID, String> domainNames = loadDomainNames(invitations);

        return invitations.stream()
                .map(invitation -> OrganisationInvitationFactory.toDTO(
                        invitation,
                        domainNames.get(invitation.getDomainUuid()),
                        classesByInvitation.getOrDefault(invitation.getUuid(), List.of())))
                .toList();
    }

    @Override
    @Transactional
    public OrganisationInvitationDTO revoke(UUID organisationUuid, UUID invitationUuid) {
        OrganisationInvitation invitation = requireInvitationOfOrganisation(organisationUuid, invitationUuid);

        if (!invitation.getStatus().isLive()) {
            throw new IllegalStateException(
                    "Only a pending invitation can be revoked; this one is " + invitation.getStatus().getValue() + ".");
        }

        invitation.setStatus(InvitationStatus.REVOKED);
        invitation.setRevokedAt(LocalDateTime.now());
        // Stop the emailed link working immediately.
        invitation.setTokenHash(tokenService.hash(tokenService.generateRawToken()));
        invitation.setGuardianConsentTokenHash(null);
        OrganisationInvitation saved = invitationRepository.save(invitation);

        log.info("Invitation {} revoked by organisation {}", invitationUuid, organisationUuid);
        return toDTO(saved);
    }

    @Override
    @Transactional
    public OrganisationInvitationDTO resend(UUID organisationUuid, UUID invitationUuid) {
        OrganisationInvitation invitation = requireInvitationOfOrganisation(organisationUuid, invitationUuid);

        if (!invitation.getStatus().isLive()) {
            throw new IllegalStateException(
                    "Only a pending invitation can be resent; this one is " + invitation.getStatus().getValue() + ".");
        }

        // A resend supersedes the previous link rather than issuing a second working one.
        String rawToken = tokenService.generateRawToken();
        invitation.setTokenHash(tokenService.hash(rawToken));
        invitation.setExpiresAt(LocalDateTime.now().plusDays(DEFAULT_EXPIRY_DAYS));
        OrganisationInvitation saved = invitationRepository.save(invitation);
        publishInvitationEmail(saved, rawToken, domainName(saved.getDomainUuid()));

        log.info("Invitation {} resent by organisation {}", invitationUuid, organisationUuid);
        return toDTO(saved);
    }

    @Override
    @Transactional
    public int expireLapsed() {
        List<OrganisationInvitation> lapsed = invitationRepository.findLapsed(LocalDateTime.now());
        if (lapsed.isEmpty()) {
            return 0;
        }

        lapsed.forEach(invitation -> invitation.setStatus(InvitationStatus.EXPIRED));
        invitationRepository.saveAll(lapsed);

        log.info("Expired {} lapsed organisation invitation(s)", lapsed.size());
        return lapsed.size();
    }

    // ================================
    // HELPERS
    // ================================

    private void requireOrganisation(UUID organisationUuid) {
        organisationRepository.findByUuid(organisationUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Organisation not found with UUID: " + organisationUuid));
    }

    private OrganisationInvitation requireInvitationOfOrganisation(UUID organisationUuid, UUID invitationUuid) {
        OrganisationInvitation invitation = invitationRepository.findByUuid(invitationUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Invitation not found with UUID: " + invitationUuid));

        if (!invitation.getOrganisationUuid().equals(organisationUuid)) {
            // Reported as not-found so one organisation cannot probe another's invitations.
            throw new ResourceNotFoundException("Invitation not found with UUID: " + invitationUuid);
        }
        return invitation;
    }

    private UserDomain resolveOrgScopedDomain(String domainName) {
        String normalised = domainName == null ? "" : domainName.trim().toLowerCase(Locale.ROOT);
        if (!ORG_SCOPED_DOMAINS.contains(normalised)) {
            throw new IllegalArgumentException(
                    "Invalid domain_name. Must be one of: organisation_user, admin, instructor, student");
        }
        return userDomainRepository.findByDomainNameAndOrgSupportedTrue(normalised)
                .orElseThrow(() -> new ResourceNotFoundException("Domain not found: " + normalised));
    }

    private UUID validateBranch(UUID organisationUuid, UUID branchUuid) {
        if (branchUuid == null) {
            return null;
        }
        return trainingBranchRepository.findByUuidAndDeletedFalse(branchUuid)
                .filter(branch -> organisationUuid.equals(branch.getOrganisationUuid()))
                .map(branch -> branchUuid)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Training branch does not belong to this organisation."));
    }

    /**
     * Keeps only classes the organisation actually owns, so an invitation cannot be used
     * to advertise another institution's classes.
     */
    private List<UUID> validateClasses(UUID organisationUuid, List<UUID> requested) {
        if (requested == null || requested.isEmpty()) {
            return List.of();
        }

        Set<UUID> owned = new HashSet<>(
                classDefinitionLookupService.findClassDefinitionUuidsByOrganisationUuid(organisationUuid));
        List<UUID> distinct = new ArrayList<>(new LinkedHashSet<>(requested));

        List<UUID> foreign = distinct.stream().filter(uuid -> !owned.contains(uuid)).toList();
        if (!foreign.isEmpty()) {
            throw new IllegalArgumentException(
                    "These classes do not belong to your organisation: " + foreign);
        }
        return distinct;
    }

    private void persistClasses(UUID invitationUuid, List<UUID> classUuids) {
        if (classUuids.isEmpty()) {
            return;
        }
        invitationClassRepository.saveAll(classUuids.stream()
                .map(classUuid -> new OrganisationInvitationClass(invitationUuid, classUuid))
                .toList());
    }

    private Map<UUID, List<UUID>> loadClasses(List<UUID> invitationUuids) {
        if (invitationUuids.isEmpty()) {
            return Map.of();
        }
        return invitationClassRepository.findByInvitationUuidIn(invitationUuids).stream()
                .collect(Collectors.groupingBy(
                        OrganisationInvitationClass::getInvitationUuid,
                        Collectors.mapping(OrganisationInvitationClass::getClassDefinitionUuid, Collectors.toList())));
    }

    private Map<UUID, String> loadDomainNames(List<OrganisationInvitation> invitations) {
        return invitations.stream()
                .map(OrganisationInvitation::getDomainUuid)
                .distinct()
                .map(userDomainRepository::findByUuid)
                .flatMap(Optional::stream)
                .collect(Collectors.toMap(UserDomain::getUuid, UserDomain::getDomainName));
    }

    private OrganisationInvitationDTO toDTO(OrganisationInvitation invitation) {
        String domainName = userDomainRepository.findByUuid(invitation.getDomainUuid())
                .map(UserDomain::getDomainName)
                .orElse(null);
        List<UUID> classUuids = invitationClassRepository.findByInvitationUuid(invitation.getUuid()).stream()
                .map(OrganisationInvitationClass::getClassDefinitionUuid)
                .toList();
        return OrganisationInvitationFactory.toDTO(invitation, domainName, classUuids);
    }

    private String organisationName(UUID organisationUuid) {
        return organisationRepository.findByUuid(organisationUuid)
                .map(Organisation::getName)
                .orElse(null);
    }

    private String domainName(UUID domainUuid) {
        return userDomainRepository.findByUuid(domainUuid)
                .map(UserDomain::getDomainName)
                .orElse(null);
    }

    private String userFullName(UUID userUuid) {
        if (userUuid == null) {
            return null;
        }
        return userRepository.findByUuid(userUuid)
                .map(user -> ((user.getFirstName() == null ? "" : user.getFirstName())
                        + " " + (user.getLastName() == null ? "" : user.getLastName())).trim())
                .filter(name -> !name.isEmpty())
                .orElse(null);
    }

    private static String orElse(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String normaliseEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
