package apps.sarafrika.elimika.tenancy.services.impl;

import apps.sarafrika.elimika.notifications.api.NotificationType;
import apps.sarafrika.elimika.shared.event.notification.NotificationRequestedEvent;
import apps.sarafrika.elimika.shared.event.student.GuardianConsentRecordedEvent;
import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.systemconfig.dto.AgeGateDecision;
import apps.sarafrika.elimika.systemconfig.dto.RuleContext;
import apps.sarafrika.elimika.systemconfig.service.RuleEvaluationService;
import apps.sarafrika.elimika.tenancy.dto.AcceptInvitationRequestDTO;
import apps.sarafrika.elimika.tenancy.dto.AcceptInvitationResultDTO;
import apps.sarafrika.elimika.tenancy.dto.GuardianConsentRequestDTO;
import apps.sarafrika.elimika.tenancy.dto.GuardianDetailsRequestDTO;
import apps.sarafrika.elimika.tenancy.dto.MyInvitationDTO;
import apps.sarafrika.elimika.tenancy.dto.PublicGuardianInvitationDTO;
import apps.sarafrika.elimika.tenancy.dto.PublicInvitationDTO;
import apps.sarafrika.elimika.tenancy.entity.Organisation;
import apps.sarafrika.elimika.tenancy.entity.OrganisationInvitation;
import apps.sarafrika.elimika.tenancy.entity.OrganisationInvitationClass;
import apps.sarafrika.elimika.tenancy.entity.User;
import apps.sarafrika.elimika.tenancy.entity.UserDomain;
import apps.sarafrika.elimika.tenancy.entity.UserOrganisationDomainMapping;
import apps.sarafrika.elimika.tenancy.internal.InvitationLinkFactory;
import apps.sarafrika.elimika.tenancy.internal.InvitationTokenService;
import apps.sarafrika.elimika.tenancy.repository.OrganisationInvitationClassRepository;
import apps.sarafrika.elimika.tenancy.repository.OrganisationInvitationRepository;
import apps.sarafrika.elimika.tenancy.repository.OrganisationRepository;
import apps.sarafrika.elimika.tenancy.repository.UserDomainRepository;
import apps.sarafrika.elimika.tenancy.repository.UserOrganisationDomainMappingRepository;
import apps.sarafrika.elimika.tenancy.repository.UserRepository;
import apps.sarafrika.elimika.tenancy.services.InvitationAcceptanceService;
import apps.sarafrika.elimika.tenancy.services.UserService;
import apps.sarafrika.elimika.tenancy.util.enums.ConsentSource;
import apps.sarafrika.elimika.tenancy.util.enums.InvitationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Default {@link InvitationAcceptanceService}.
 * <p>
 * Every path here converges on {@link #completeAffiliation}, which is the single place an
 * invitation turns into a {@code user_organisation_domain_mapping} row. Sending an
 * invitation never reaches it.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2026-07-28
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvitationAcceptanceServiceImpl implements InvitationAcceptanceService {

    private static final String AGE_GATE_RULE_KEY = "student.onboarding.age_gate";
    private static final String DEFAULT_SHARE_SCOPE = "FULL";

    private final OrganisationInvitationRepository invitationRepository;
    private final OrganisationInvitationClassRepository invitationClassRepository;
    private final OrganisationRepository organisationRepository;
    private final UserRepository userRepository;
    private final UserDomainRepository userDomainRepository;
    private final UserOrganisationDomainMappingRepository mappingRepository;
    private final UserService userService;
    private final RuleEvaluationService ruleEvaluationService;
    private final InvitationTokenService tokenService;
    private final InvitationLinkFactory linkFactory;
    private final ApplicationEventPublisher eventPublisher;

    // ================================
    // READING AN INVITATION
    // ================================

    @Override
    @Transactional(readOnly = true)
    public PublicInvitationDTO lookupByToken(String rawToken) {
        return toPublicDTO(requireInvitationByToken(rawToken));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MyInvitationDTO> listForUser(UUID userUuid) {
        String email = requireUser(userUuid).getEmail();
        if (email == null || email.isBlank()) {
            return List.of();
        }

        return invitationRepository.findLiveByRecipientEmail(email).stream()
                .map(invitation -> new MyInvitationDTO(
                        invitation.getUuid(),
                        organisationName(invitation.getOrganisationUuid()),
                        invitation.getOrganisationUuid(),
                        userFullName(invitation.getInviterUserUuid()),
                        domainName(invitation.getDomainUuid()),
                        invitation.getMessage(),
                        classUuids(invitation.getUuid()).size(),
                        invitation.getStatus(),
                        invitation.isRequiresGuardianConsent(),
                        invitation.getExpiresAt(),
                        invitation.getCreatedDate()))
                .toList();
    }

    // ================================
    // ACCEPTING AND DECLINING
    // ================================

    @Override
    @Transactional
    public AcceptInvitationResultDTO acceptByToken(String rawToken,
                                                    AcceptInvitationRequestDTO request,
                                                    UUID actingUserUuid) {
        return accept(requireInvitationByToken(rawToken), request, actingUserUuid);
    }

    @Override
    @Transactional
    public AcceptInvitationResultDTO acceptByUuid(UUID invitationUuid,
                                                   AcceptInvitationRequestDTO request,
                                                   UUID actingUserUuid) {
        return accept(requireActionable(requireInvitation(invitationUuid)), request, actingUserUuid);
    }

    private AcceptInvitationResultDTO accept(OrganisationInvitation invitation,
                                             AcceptInvitationRequestDTO request,
                                             UUID actingUserUuid) {
        User actor = requireUser(actingUserUuid);
        requireAddressedTo(invitation, actor);

        if (invitation.getStatus() == InvitationStatus.AWAITING_GUARDIAN_CONSENT) {
            throw new IllegalStateException(
                    "This invitation is waiting on a guardian's consent and cannot be accepted directly.");
        }

        // Bind the invitation to the account now acting on it. For someone who registered
        // after being invited, this is the first point at which the account exists.
        invitation.setRecipientUserUuid(actor.getUuid());

        LocalDate dateOfBirth = resolveDateOfBirth(actor, request);
        if (isMinor(dateOfBirth)) {
            return routeToGuardian(invitation, actor);
        }

        return completeAffiliation(invitation, actor.getUuid(), ConsentSource.INVITATION, actor.getUuid());
    }

    @Override
    @Transactional
    public void declineByToken(String rawToken, UUID actingUserUuid) {
        decline(requireInvitationByToken(rawToken), actingUserUuid);
    }

    @Override
    @Transactional
    public void declineByUuid(UUID invitationUuid, UUID actingUserUuid) {
        decline(requireActionable(requireInvitation(invitationUuid)), actingUserUuid);
    }

    private void decline(OrganisationInvitation invitation, UUID actingUserUuid) {
        requireAddressedTo(invitation, requireUser(actingUserUuid));

        invitation.setStatus(InvitationStatus.DECLINED);
        invitation.setDeclinedAt(LocalDateTime.now());
        invalidateTokens(invitation);
        invitationRepository.save(invitation);

        log.info("Invitation {} declined by user {}", invitation.getUuid(), actingUserUuid);
    }

    // ================================
    // GUARDIAN BRANCH
    // ================================

    /**
     * Hands the decision to a guardian. Deliberately creates no affiliation: a minor
     * cannot consent to one for themselves.
     */
    private AcceptInvitationResultDTO routeToGuardian(OrganisationInvitation invitation, User minor) {
        invitation.setRequiresGuardianConsent(true);
        invitation.setStatus(InvitationStatus.AWAITING_GUARDIAN_CONSENT);
        invitationRepository.save(invitation);

        log.info("Invitation {} routed to guardian consent for minor {}", invitation.getUuid(), minor.getUuid());
        return new AcceptInvitationResultDTO(
                invitation.getStatus(),
                false,
                true,
                invitation.getOrganisationUuid(),
                organisationName(invitation.getOrganisationUuid()),
                List.of(),
                "You are under the age we can accept consent from directly. "
                        + "Tell us who your parent or guardian is and we will ask them to approve this.");
    }

    @Override
    @Transactional
    public PublicInvitationDTO submitGuardianDetails(String rawToken,
                                                      GuardianDetailsRequestDTO details,
                                                      UUID actingUserUuid) {
        OrganisationInvitation invitation = requireInvitationByToken(rawToken);
        User actor = requireUser(actingUserUuid);
        requireAddressedTo(invitation, actor);

        if (invitation.getStatus() != InvitationStatus.AWAITING_GUARDIAN_CONSENT) {
            throw new IllegalStateException("This invitation is not waiting for guardian details.");
        }

        String guardianEmail = details.guardianEmail().trim().toLowerCase(Locale.ROOT);
        if (guardianEmail.equalsIgnoreCase(invitation.getRecipientEmail())) {
            throw new IllegalArgumentException("A guardian must be someone other than the student.");
        }

        invitation.setGuardianEmail(guardianEmail);
        invitation.setGuardianName(details.guardianName().trim());
        invitation.setGuardianRelationshipType(details.guardianRelationshipType().trim().toUpperCase(Locale.ROOT));
        invitation.setGuardianPhone(details.guardianPhone() == null ? null : details.guardianPhone().trim());
        // The guardian gets their own token so the child's link cannot be used to consent.
        String guardianRawToken = tokenService.generateRawToken();
        invitation.setGuardianConsentTokenHash(tokenService.hash(guardianRawToken));
        OrganisationInvitation saved = invitationRepository.save(invitation);

        publishGuardianConsentEmail(saved, guardianRawToken, actor);

        log.info("Guardian nominated for invitation {}", invitation.getUuid());
        return toPublicDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PublicGuardianInvitationDTO lookupGuardianRequestByToken(String rawToken) {
        OrganisationInvitation invitation = requireGuardianInvitationByToken(rawToken);

        return new PublicGuardianInvitationDTO(
                organisationName(invitation.getOrganisationUuid()),
                invitation.getOrganisationUuid(),
                invitation.getRecipientName(),
                maskEmail(invitation.getRecipientEmail()),
                invitation.getGuardianName(),
                invitation.getGuardianRelationshipType(),
                domainName(invitation.getDomainUuid()),
                classUuids(invitation.getUuid()).size(),
                invitation.getStatus(),
                invitation.isActionable(),
                invitation.getExpiresAt());
    }

    @Override
    @Transactional
    public AcceptInvitationResultDTO guardianConsent(String rawToken,
                                                      GuardianConsentRequestDTO request,
                                                      UUID guardianUserUuid) {
        OrganisationInvitation invitation = requireGuardianInvitationByToken(rawToken);
        User guardian = requireUser(guardianUserUuid);
        requireNominatedGuardian(invitation, guardian);

        UUID minorUserUuid = invitation.getRecipientUserUuid();
        if (minorUserUuid == null) {
            throw new IllegalStateException(
                    "The student has not yet signed in to accept, so consent cannot be recorded.");
        }

        invitation.setGuardianUserUuid(guardian.getUuid());
        invitation.setGuardianConsentedAt(LocalDateTime.now());

        AcceptInvitationResultDTO result =
                completeAffiliation(invitation, minorUserUuid, ConsentSource.GUARDIAN, guardian.getUuid());

        String shareScope = request.shareScope() == null || request.shareScope().isBlank()
                ? DEFAULT_SHARE_SCOPE
                : request.shareScope().trim().toUpperCase(Locale.ROOT);

        // The guardian link belongs to the student module, which owns the learner profile
        // this points at - and for a brand-new invitee that profile is only created once
        // the student domain assignment above has been processed.
        eventPublisher.publishEvent(new GuardianConsentRecordedEvent(
                minorUserUuid,
                guardian.getUuid(),
                invitation.getGuardianRelationshipType(),
                shareScope,
                invitation.getOrganisationUuid()));

        log.info("Guardian {} consented to invitation {} for minor {}",
                guardian.getUuid(), invitation.getUuid(), minorUserUuid);
        return result;
    }

    @Override
    @Transactional
    public void guardianDecline(String rawToken, UUID guardianUserUuid) {
        OrganisationInvitation invitation = requireGuardianInvitationByToken(rawToken);
        requireNominatedGuardian(invitation, requireUser(guardianUserUuid));

        invitation.setStatus(InvitationStatus.DECLINED);
        invitation.setDeclinedAt(LocalDateTime.now());
        invitation.setGuardianUserUuid(guardianUserUuid);
        invalidateTokens(invitation);
        invitationRepository.save(invitation);

        log.info("Guardian {} declined invitation {}", guardianUserUuid, invitation.getUuid());
    }

    // ================================
    // THE ONE PLACE AN AFFILIATION IS BORN
    // ================================

    private AcceptInvitationResultDTO completeAffiliation(OrganisationInvitation invitation,
                                                           UUID memberUserUuid,
                                                           ConsentSource consentSource,
                                                           UUID consentGrantedByUserUuid) {
        // An existing member keeps the affiliation and the consent record they already
        // gave: re-forming it would silently overwrite their original role, branch and
        // the record of how they first consented. The offer still stands for the classes
        // it carries.
        boolean alreadyAffiliated = mappingRepository
                .existsByUserUuidAndOrganisationUuidAndActiveTrueAndDeletedFalse(
                        memberUserUuid, invitation.getOrganisationUuid());

        if (alreadyAffiliated) {
            log.info("User {} is already affiliated to organisation {}; leaving the existing "
                            + "affiliation and consent untouched",
                    memberUserUuid, invitation.getOrganisationUuid());
        } else {
            String domainName = domainName(invitation.getDomainUuid());
            userService.assignUserToOrganisation(
                    memberUserUuid, invitation.getOrganisationUuid(), domainName, invitation.getBranchUuid());
            stampConsent(invitation, memberUserUuid, consentSource, consentGrantedByUserUuid);
        }

        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitation.setAcceptedAt(LocalDateTime.now());
        invitation.setAcceptedUserUuid(memberUserUuid);
        invalidateTokens(invitation);
        invitationRepository.save(invitation);
        publishAcceptedEmail(invitation, memberUserUuid);

        List<UUID> surfaced = classUuids(invitation.getUuid());
        log.info("Invitation {} accepted; user {} affiliated to organisation {} via {}",
                invitation.getUuid(), memberUserUuid, invitation.getOrganisationUuid(), consentSource.getValue());

        String joined = alreadyAffiliated
                ? "You are already a member of this organisation."
                : "You have joined the organisation.";
        String classes = surfaced.isEmpty()
                ? ""
                : " " + surfaced.size() + " class(es) are now available for you to enrol in.";

        return new AcceptInvitationResultDTO(
                InvitationStatus.ACCEPTED,
                true,
                false,
                invitation.getOrganisationUuid(),
                organisationName(invitation.getOrganisationUuid()),
                surfaced,
                joined + classes);
    }

    /**
     * Records how the affiliation was consented to, so it can always be traced back to a
     * consenting act rather than to an administrator's say-so.
     */
    private void stampConsent(OrganisationInvitation invitation,
                              UUID memberUserUuid,
                              ConsentSource consentSource,
                              UUID consentGrantedByUserUuid) {
        Optional<UserOrganisationDomainMapping> mapping = mappingRepository
                .findByUserUuidAndOrganisationUuidAndActiveTrueAndDeletedFalse(
                        memberUserUuid, invitation.getOrganisationUuid());

        if (mapping.isEmpty()) {
            log.warn("No active mapping found to stamp consent on for user {} in organisation {}",
                    memberUserUuid, invitation.getOrganisationUuid());
            return;
        }

        UserOrganisationDomainMapping affiliation = mapping.get();
        affiliation.setConsentGrantedAt(LocalDateTime.now());
        affiliation.setConsentSource(consentSource);
        affiliation.setConsentGrantedByUserUuid(consentGrantedByUserUuid);
        affiliation.setInvitationUuid(invitation.getUuid());
        mappingRepository.save(affiliation);
    }

    // ================================
    // EMAILS
    // ================================

    /**
     * Asks the nominated guardian to decide, on their own link.
     */
    private void publishGuardianConsentEmail(OrganisationInvitation invitation, String rawToken, User minor) {
        // Template variables are copied into an immutable map downstream, which rejects
        // nulls outright - so every value here must be non-null.
        Map<String, Object> variables = new HashMap<>();
        variables.put("guardianName", orElse(invitation.getGuardianName(), "there"));
        variables.put("studentName", displayName(invitation, minor));
        variables.put("organisationName", orElse(organisationName(invitation.getOrganisationUuid()), "An organisation"));
        variables.put("roleName", orElse(domainName(invitation.getDomainUuid()), "student"));
        variables.put("relationshipType", orElse(invitation.getGuardianRelationshipType(), "GUARDIAN"));
        variables.put("consentLink", linkFactory.guardianConsentLink(rawToken));
        variables.put("expiresAt", invitation.getExpiresAt() == null ? "" : invitation.getExpiresAt().toString());

        eventPublisher.publishEvent(NotificationRequestedEvent.email(
                null,
                invitation.getGuardianEmail(),
                invitation.getGuardianName(),
                NotificationType.GUARDIAN_CONSENT_REQUEST.getValue(),
                variables));
    }

    /**
     * Lets the organisation know their invitation landed, so the pending list is not the
     * only way to find out.
     */
    private void publishAcceptedEmail(OrganisationInvitation invitation, UUID memberUserUuid) {
        String inviterEmail = userRepository.findByUuid(invitation.getInviterUserUuid())
                .map(User::getEmail)
                .orElse(null);
        if (inviterEmail == null) {
            return;
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("recipientName", orElse(userFullName(memberUserUuid), "Someone"));
        variables.put("organisationName", orElse(organisationName(invitation.getOrganisationUuid()), "your organisation"));
        variables.put("roleName", orElse(domainName(invitation.getDomainUuid()), "member"));
        variables.put("membersLink", linkFactory.organisationInvitationsLink());

        eventPublisher.publishEvent(NotificationRequestedEvent.email(
                invitation.getInviterUserUuid(),
                inviterEmail,
                orElse(userFullName(invitation.getInviterUserUuid()), ""),
                NotificationType.ORGANISATION_INVITATION_ACCEPTED.getValue(),
                variables));
    }

    private String displayName(OrganisationInvitation invitation, User user) {
        if (invitation.getRecipientName() != null && !invitation.getRecipientName().isBlank()) {
            return invitation.getRecipientName();
        }
        String fullName = (safe(user.getFirstName()) + " " + safe(user.getLastName())).trim();
        return fullName.isEmpty() ? "your child" : fullName;
    }

    // ================================
    // AGE GATE
    // ================================

    /**
     * Establishes the date of birth to judge against, persisting it on the user's own
     * profile when they have supplied it for the first time.
     */
    private LocalDate resolveDateOfBirth(User actor, AcceptInvitationRequestDTO request) {
        LocalDate supplied = request == null ? null : request.dateOfBirth();

        if (supplied != null) {
            if (actor.getDob() == null) {
                actor.setDob(supplied);
                userRepository.save(actor);
            }
            return supplied;
        }

        if (actor.getDob() != null) {
            return actor.getDob();
        }

        throw new IllegalArgumentException(
                "Your date of birth is needed before this invitation can be accepted.");
    }

    private boolean isMinor(LocalDate dateOfBirth) {
        AgeGateDecision decision = ruleEvaluationService.evaluateAgeGate(
                dateOfBirth,
                new RuleContext(AGE_GATE_RULE_KEY, null, null, null, null,
                        OffsetDateTime.now(ZoneOffset.UTC)));
        return !decision.allowed();
    }

    // ================================
    // LOOKUPS AND GUARDS
    // ================================

    private OrganisationInvitation requireInvitationByToken(String rawToken) {
        OrganisationInvitation invitation = invitationRepository
                .findByTokenHash(tokenService.hash(rawToken))
                .orElseThrow(() -> new ResourceNotFoundException("This invitation link is not valid."));
        return requireActionable(invitation);
    }

    private OrganisationInvitation requireGuardianInvitationByToken(String rawToken) {
        OrganisationInvitation invitation = invitationRepository
                .findByGuardianConsentTokenHash(tokenService.hash(rawToken))
                .orElseThrow(() -> new ResourceNotFoundException("This consent link is not valid."));
        return requireActionable(invitation);
    }

    private OrganisationInvitation requireInvitation(UUID invitationUuid) {
        return invitationRepository.findByUuid(invitationUuid)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Invitation not found with UUID: " + invitationUuid));
    }

    private OrganisationInvitation requireActionable(OrganisationInvitation invitation) {
        if (!invitation.isActionable()) {
            throw new IllegalStateException(
                    "This invitation is no longer open (" + invitation.getStatus().getValue().toLowerCase(Locale.ROOT) + ").");
        }
        return invitation;
    }

    /**
     * An invitation is addressed to an email, not to whoever happens to hold the link.
     */
    private void requireAddressedTo(OrganisationInvitation invitation, User actor) {
        if (actor.getEmail() == null || !actor.getEmail().equalsIgnoreCase(invitation.getRecipientEmail())) {
            log.warn("User {} attempted to act on invitation {} addressed to someone else",
                    actor.getUuid(), invitation.getUuid());
            throw new AccessDeniedException("This invitation was sent to a different email address.");
        }
    }

    private void requireNominatedGuardian(OrganisationInvitation invitation, User guardian) {
        if (invitation.getGuardianEmail() == null
                || guardian.getEmail() == null
                || !guardian.getEmail().equalsIgnoreCase(invitation.getGuardianEmail())) {
            log.warn("User {} attempted to consent on invitation {} without being the nominated guardian",
                    guardian.getUuid(), invitation.getUuid());
            throw new AccessDeniedException("This consent request was sent to a different email address.");
        }
    }

    /**
     * Burns both links once an invitation reaches a terminal state, so a forwarded email
     * cannot be replayed.
     */
    private void invalidateTokens(OrganisationInvitation invitation) {
        invitation.setTokenHash(tokenService.hash(tokenService.generateRawToken()));
        invitation.setGuardianConsentTokenHash(null);
    }

    private User requireUser(UUID userUuid) {
        return userRepository.findByUuid(userUuid)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with UUID: " + userUuid));
    }

    // ================================
    // MAPPING
    // ================================

    private PublicInvitationDTO toPublicDTO(OrganisationInvitation invitation) {
        return new PublicInvitationDTO(
                organisationName(invitation.getOrganisationUuid()),
                invitation.getOrganisationUuid(),
                userFullName(invitation.getInviterUserUuid()),
                domainName(invitation.getDomainUuid()),
                maskEmail(invitation.getRecipientEmail()),
                invitation.getRecipientName(),
                invitation.getRecipientUserUuid() != null,
                invitation.getMessage(),
                classUuids(invitation.getUuid()).size(),
                invitation.getStatus(),
                invitation.isActionable(),
                invitation.isRequiresGuardianConsent(),
                invitation.getExpiresAt());
    }

    private List<UUID> classUuids(UUID invitationUuid) {
        return invitationClassRepository.findByInvitationUuid(invitationUuid).stream()
                .map(OrganisationInvitationClass::getClassDefinitionUuid)
                .toList();
    }

    private String organisationName(UUID organisationUuid) {
        return organisationRepository.findByUuid(organisationUuid)
                .map(Organisation::getName)
                .orElse(null);
    }

    private String domainName(UUID domainUuid) {
        return userDomainRepository.findByUuid(domainUuid)
                .map(UserDomain::getDomainName)
                .orElseThrow(() -> new ResourceNotFoundException("Domain not found: " + domainUuid));
    }

    private String userFullName(UUID userUuid) {
        if (userUuid == null) {
            return null;
        }
        return userRepository.findByUuid(userUuid)
                .map(user -> (safe(user.getFirstName()) + " " + safe(user.getLastName())).trim())
                .filter(name -> !name.isEmpty())
                .orElse(null);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String orElse(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    /**
     * Shows enough of an address for the recipient to recognise it as theirs, without
     * disclosing it to whoever else might open the link.
     */
    private static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        String local = email.substring(0, at);
        String domain = email.substring(at);
        if (local.length() <= 2) {
            return local.charAt(0) + "***" + domain;
        }
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + domain;
    }
}
