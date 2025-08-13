package apps.sarafrika.elimika.tenancy.services.impl;

import apps.sarafrika.elimika.common.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.common.util.EmailUtility;
import apps.sarafrika.elimika.tenancy.dto.InvitationDTO;
import apps.sarafrika.elimika.tenancy.dto.UserDTO;
import apps.sarafrika.elimika.tenancy.entity.*;
import apps.sarafrika.elimika.tenancy.factory.InvitationFactory;
import apps.sarafrika.elimika.tenancy.repository.*;
import apps.sarafrika.elimika.tenancy.services.InvitationService;
import apps.sarafrika.elimika.tenancy.services.UserService;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvitationServiceImpl implements InvitationService {

    private final InvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final OrganisationRepository organisationRepository;
    private final TrainingBranchRepository trainingBranchRepository;
    private final UserDomainRepository userDomainRepository;
    private final UserDomainMappingRepository userDomainMappingRepository;
    private final UserOrganisationDomainMappingRepository userOrganisationDomainMappingRepository;

    private final EmailUtility emailUtility;
    private final UserService userService;

    @Value("${app.invitation.expiry-days:7}")
    private int invitationExpiryDays;

    // ================================
    // INVITATION CREATION
    // ================================

    @Override
    @Transactional
    public InvitationDTO createOrganisationInvitation(
            String recipientEmail,
            String recipientName,
            UUID organisationUuid,
            String domainName,
            UUID branchUuid,
            UUID inviterUuid,
            String notes) {

        log.debug("Creating organization invitation for {} to organization {}", recipientEmail, organisationUuid);

        // Validate inputs
        validateInvitationInputs(recipientEmail, recipientName, organisationUuid, domainName, inviterUuid);
        validateInvitationRules(recipientEmail, domainName);

        // Check for existing pending invitation
        if (hasPendingInvitation(recipientEmail, organisationUuid)) {
            throw new IllegalStateException("A pending invitation already exists for this user and organization");
        }

        // Get entities
        Organisation organisation = findOrganisationOrThrow(organisationUuid);
        UserDomain domain = findDomainByNameOrThrow(domainName);
        User inviter = findUserOrThrow(inviterUuid);
        TrainingBranch branch = null;

        if (branchUuid != null) {
            branch = findTrainingBranchOrThrow(branchUuid);
            if (!branch.getOrganisationUuid().equals(organisationUuid)) {
                throw new IllegalArgumentException("Training branch does not belong to the specified organization");
            }
        }

        // Create invitation
        Invitation invitation = Invitation.builder()
                .token(emailUtility.generateInvitationToken())
                .recipientEmail(recipientEmail.toLowerCase().trim())
                .recipientName(recipientName.trim())
                .organisationUuid(organisationUuid)
                .branchUuid(branchUuid)
                .domainUuid(domain.getUuid())
                .inviterUuid(inviterUuid)
                .inviterName(inviter.getFirstName() + " " + inviter.getLastName())
                .status(Invitation.InvitationStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusDays(invitationExpiryDays))
                .notes(notes)
                .createdBy(inviter.getEmail())
                .build();

        invitation = invitationRepository.save(invitation);

        // Send invitation email asynchronously
        sendInvitationEmailAsync(invitation, organisation, branch, domain);

        log.info("Created organization invitation {} for {} to organization {}",
                invitation.getUuid(), recipientEmail, organisationUuid);

        return InvitationFactory.toDTO(invitation);
    }

    @Override
    @Transactional
    public InvitationDTO createBranchInvitation(
            String recipientEmail,
            String recipientName,
            UUID branchUuid,
            String domainName,
            UUID inviterUuid,
            String notes) {

        log.debug("Creating branch invitation for {} to branch {}", recipientEmail, branchUuid);

        TrainingBranch branch = findTrainingBranchOrThrow(branchUuid);

        return createOrganisationInvitation(
                recipientEmail,
                recipientName,
                branch.getOrganisationUuid(),
                domainName,
                branchUuid,
                inviterUuid,
                notes
        );
    }

    // ================================
    // INVITATION MANAGEMENT
    // ================================

    @Override
    @Transactional
    public UserDTO acceptInvitation(String token, UUID userUuid) {
        log.debug("Accepting invitation with token {} by user {}", token, userUuid);

        // Validate invitation and user (extracted common logic)
        ValidationResult validationResult = validateInvitationAndUser(token, userUuid);
        Invitation invitation = validationResult.invitation();
        User user = validationResult.user();

        // Get domain information
        UserDomain domain = findDomainByUuidOrThrow(invitation.getDomainUuid());
        String domainName = domain.getDomainName();

        // Check if user is already in the organization
        boolean alreadyMember = userOrganisationDomainMappingRepository
                .existsByUserUuidAndOrganisationUuidAndActiveTrueAndDeletedFalse(
                        userUuid, invitation.getOrganisationUuid());

        if (alreadyMember) {
            throw new IllegalStateException("User is already a member of this organization");
        }

        // Accept the invitation
        invitation.accept(userUuid);
        invitationRepository.save(invitation);

        // Handle domain assignment based on role type
        if ("admin".equals(domainName) || "organisation_user".equals(domainName)) {
            // For invitation-only roles, add domain to standalone domains
            addStandaloneDomainToUser(user, domain);
        }

        // Create user-organization relationship using UserService
        UserDTO updatedUser = userService.assignUserToOrganisation(
                userUuid,
                invitation.getOrganisationUuid(),
                domainName,
                invitation.getBranchUuid()
        );

        // Send confirmation email asynchronously
        sendAcceptanceConfirmationEmailAsync(invitation, user);

        log.info("Accepted invitation {} by user {}", invitation.getUuid(), userUuid);

        return updatedUser;
    }

    @Override
    @Transactional
    public void declineInvitation(String token, UUID userUuid) {
        log.debug("Declining invitation with token {} by user {}", token, userUuid);

        // Validate invitation and user (extracted common logic)
        ValidationResult validationResult = validateInvitationAndUser(token, userUuid);
        Invitation invitation = validationResult.invitation();
        User user = validationResult.user();

        // Decline the invitation
        invitation.decline();
        invitationRepository.save(invitation);

        // Send decline notification to inviter/admin asynchronously
        sendDeclineNotificationEmailAsync(invitation, user);

        log.info("Declined invitation {} by user {}", invitation.getUuid(), userUuid);
    }

    @Override
    @Transactional
    public void cancelInvitation(UUID invitationUuid, UUID cancellerUuid) {
        log.debug("Cancelling invitation {} by user {}", invitationUuid, cancellerUuid);

        Invitation invitation = findInvitationByUuidOrThrow(invitationUuid);

        // Validate canceller has permission (inviter or organization admin)
        if (canManageInvitation(invitation, cancellerUuid)) {
            throw new IllegalArgumentException("User does not have permission to cancel this invitation");
        }

        invitation.cancel();
        invitationRepository.save(invitation);

        log.info("Cancelled invitation {} by user {}", invitationUuid, cancellerUuid);
    }

    @Override
    @Transactional
    public void resendInvitation(UUID invitationUuid, UUID resenderUuid) {
        log.debug("Resending invitation {} by user {}", invitationUuid, resenderUuid);

        Invitation invitation = findInvitationByUuidOrThrow(invitationUuid);

        // Validate resender has permission
        if (canManageInvitation(invitation, resenderUuid)) {
            throw new IllegalArgumentException("User does not have permission to resend this invitation");
        }

        // Only resend pending invitations
        if (invitation.getStatus() != Invitation.InvitationStatus.PENDING) {
            throw new IllegalStateException("Can only resend pending invitations");
        }

        // Extend expiry date
        invitation.setExpiresAt(LocalDateTime.now().plusDays(invitationExpiryDays));
        invitationRepository.save(invitation);

        // Resend email
        Organisation organisation = findOrganisationOrThrow(invitation.getOrganisationUuid());
        TrainingBranch branch = invitation.getBranchUuid() != null ?
                findTrainingBranchOrThrow(invitation.getBranchUuid()) : null;
        UserDomain domain = findDomainByUuidOrThrow(invitation.getDomainUuid());

        sendInvitationEmailAsync(invitation, organisation, branch, domain);

        log.info("Resent invitation {} by user {}", invitationUuid, resenderUuid);
    }

    // ================================
    // INVITATION QUERIES
    // ================================

    @Override
    @Transactional(readOnly = true)
    public InvitationDTO getInvitationByToken(String token) {
        Invitation invitation = findInvitationByTokenOrThrow(token);
        return InvitationFactory.toDTO(invitation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvitationDTO> getPendingInvitationsForEmail(String recipientEmail) {
        List<Invitation> invitations = invitationRepository
                .findByRecipientEmailAndStatus(recipientEmail.toLowerCase().trim(), Invitation.InvitationStatus.PENDING);

        return invitations.stream()
                .map(InvitationFactory::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvitationDTO> getOrganisationInvitations(UUID organisationUuid) {
        List<Invitation> invitations = invitationRepository
                .findByOrganisationUuidOrderByCreatedDateDesc(organisationUuid);

        return invitations.stream()
                .map(InvitationFactory::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvitationDTO> getBranchInvitations(UUID branchUuid) {
        List<Invitation> invitations = invitationRepository
                .findByBranchUuidOrderByCreatedDateDesc(branchUuid);

        return invitations.stream()
                .map(InvitationFactory::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvitationDTO> getInvitationsSentByUser(UUID inviterUuid) {
        List<Invitation> invitations = invitationRepository
                .findByInviterUuidOrderByCreatedDateDesc(inviterUuid);

        return invitations.stream()
                .map(InvitationFactory::toDTO)
                .collect(Collectors.toList());
    }

    // ================================
    // VALIDATION METHODS
    // ================================

    @Override
    @Transactional(readOnly = true)
    public boolean isInvitationValid(String token) {
        return invitationRepository.findByToken(token)
                .map(Invitation::isValid)
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasPendingInvitation(String recipientEmail, UUID organisationUuid) {
        return invitationRepository.existsByRecipientEmailAndOrganisationUuidAndStatus(
                recipientEmail.toLowerCase().trim(), organisationUuid, Invitation.InvitationStatus.PENDING);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasPendingBranchInvitation(String recipientEmail, UUID organisationUuid, UUID branchUuid) {
        return invitationRepository.existsByRecipientEmailAndOrganisationUuidAndBranchUuidAndStatus(
                recipientEmail.toLowerCase().trim(), organisationUuid, branchUuid, Invitation.InvitationStatus.PENDING);
    }

    // ================================
    // MAINTENANCE METHODS
    // ================================

    @Override
    @Transactional
    public int markExpiredInvitations() {
        log.debug("Marking expired invitations");
        try {
            int markedCount = invitationRepository.markExpiredInvitations();
            if (markedCount > 0) {
                log.info("Marked {} invitations as expired", markedCount);
            } else {
                log.debug("No invitations to mark as expired");
            }
            return markedCount;
        } catch (Exception e) {
            log.error("Error marking expired invitations", e);
            throw new RuntimeException("Failed to mark expired invitations: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public int sendExpiryReminders(int hoursBeforeExpiry) {
        log.debug("Sending expiry reminders for invitations expiring in {} hours", hoursBeforeExpiry);

        try {
            LocalDateTime reminderTime = LocalDateTime.now().plusHours(hoursBeforeExpiry);
            List<Invitation> expiringInvitations = invitationRepository.findInvitationsExpiringBefore(reminderTime);

            if (expiringInvitations.isEmpty()) {
                log.debug("No invitations found expiring within {} hours", hoursBeforeExpiry);
                return 0;
            }

            log.info("Found {} invitations expiring within {} hours", expiringInvitations.size(), hoursBeforeExpiry);

            // Filter invitations that should receive reminders
            List<Invitation> invitationsToRemind = expiringInvitations.stream()
                    .filter(invitation -> {
                        long hoursUntilExpiry = java.time.Duration.between(
                                LocalDateTime.now(),
                                invitation.getExpiresAt()
                        ).toHours();

                        // Send reminders for invitations expiring in 1-48 hours
                        return hoursUntilExpiry > 0 && hoursUntilExpiry <= 48;
                    })
                    .toList();

            log.info("Sending expiry reminders to {} invitations", invitationsToRemind.size());

            // Send reminders asynchronously
            invitationsToRemind.forEach(this::sendExpiryReminderEmailAsync);

            return invitationsToRemind.size();

        } catch (Exception e) {
            log.error("Error sending expiry reminders", e);
            throw new RuntimeException("Failed to send expiry reminders: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public int cleanupOldInvitations(int daysOld) {
        log.debug("Cleaning up invitations older than {} days", daysOld);

        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
            int deletedCount = invitationRepository.deleteOldInvitations(cutoffDate);

            if (deletedCount > 0) {
                log.info("Cleaned up {} old invitations", deletedCount);
            } else {
                log.debug("No old invitations to clean up");
            }

            return deletedCount;

        } catch (Exception e) {
            log.error("Error cleaning up old invitations", e);
            throw new RuntimeException("Failed to cleanup old invitations: " + e.getMessage(), e);
        }
    }

    // ================================
    // PRIVATE HELPER METHODS
    // ================================

    /**
     * Validates invitation and user for accept/decline operations
     */
    private ValidationResult validateInvitationAndUser(String token, UUID userUuid) {
        Invitation invitation = findInvitationByTokenOrThrow(token);

        // Validate invitation
        if (!invitation.isValid()) {
            throw new IllegalStateException("Invitation is no longer valid or has expired");
        }

        User user = findUserOrThrow(userUuid);

        // Validate user email matches invitation
        if (!user.getEmail().equalsIgnoreCase(invitation.getRecipientEmail())) {
            throw new IllegalArgumentException("User email does not match invitation recipient");
        }

        return new ValidationResult(invitation, user);
    }

    /**
     * Inner class to hold validation results
     */
    private record ValidationResult(Invitation invitation, User user) {

    }

    private void validateInvitationInputs(String recipientEmail, String recipientName,
                                          UUID organisationUuid, String domainName, UUID inviterUuid) {
        if (!emailUtility.isValidEmail(recipientEmail)) {
            throw new IllegalArgumentException("Invalid email address");
        }
        if (recipientName == null || recipientName.trim().isEmpty()) {
            throw new IllegalArgumentException("Recipient name is required");
        }
        if (organisationUuid == null) {
            throw new IllegalArgumentException("Organization UUID is required");
        }
        if (domainName == null || domainName.trim().isEmpty()) {
            throw new IllegalArgumentException("Domain name is required");
        }
        if (inviterUuid == null) {
            throw new IllegalArgumentException("Inviter UUID is required");
        }
    }

    /**
     * Validates invitation rules based on domain and user existence
     */
    private void validateInvitationRules(String recipientEmail, String domainName) {
        Optional<User> existingUser = userRepository.findByEmail(recipientEmail);

        // For admin and organisation_user roles, user must already exist
        if ("admin".equals(domainName) || "organisation_user".equals(domainName)) {
            if (existingUser.isEmpty()) {
                throw new IllegalStateException("User with email " + recipientEmail +
                        " must register on the platform before being invited as " + domainName +
                        ". Please ask them to create an account first.");
            }
        }

        // For student/instructor roles, check if user already has the domain
        if (existingUser.isPresent()) {
            List<String> userDomains = getUserDomainsForUser(existingUser.get().getUuid());

            if ("student".equals(domainName) && !userDomains.contains("student")) {
                // User exists but doesn't have student domain - this is fine for organization invitation
                log.debug("Existing user {} being invited as student to organization", recipientEmail);
            } else if ("instructor".equals(domainName) && !userDomains.contains("instructor")) {
                // User exists but doesn't have instructor domain - this is fine for organization invitation
                log.debug("Existing user {} being invited as instructor to organization", recipientEmail);
            }
        }
    }

    private boolean canManageInvitation(Invitation invitation, UUID userUuid) {
        // User is the inviter
        if (invitation.getInviterUuid().equals(userUuid)) {
            return false;
        }

        // User is an admin in the organization
        return userService.hasUserRoleInOrganisation(userUuid, invitation.getOrganisationUuid(), "admin") &&
                userService.hasUserRoleInOrganisation(userUuid, invitation.getOrganisationUuid(), "organisation_user");
    }

    private String getDomainNameByUuid(UUID domainUuid) {
        return userDomainRepository.findByUuid(domainUuid)
                .map(UserDomain::getDomainName)
                .orElseThrow(() -> new ResourceNotFoundException("Domain not found"));
    }

    /**
     * Adds a standalone domain to a user (not organisation-specific)
     */
    private void addStandaloneDomainToUser(User user, UserDomain domain) {
        // Check if mapping already exists
        if (!userDomainMappingRepository.existsByUserUuidAndUserDomainUuid(user.getUuid(), domain.getUuid())) {
            UserDomainMapping mapping = new UserDomainMapping(null, user.getUuid(), domain.getUuid(), null, null);
            userDomainMappingRepository.save(mapping);
            log.info("Added standalone domain {} to user {}", domain.getDomainName(), user.getUuid());
        }
    }

    /**
     * Gets all domains for a user from both standalone and organisation mappings
     */
    private List<String> getUserDomainsForUser(UUID userUuid) {
        Set<String> allDomains = new HashSet<>();

        // Get standalone domains
        List<UserDomainMapping> standaloneMappings = userDomainMappingRepository.findByUserUuid(userUuid);
        for (UserDomainMapping mapping : standaloneMappings) {
            userDomainRepository.findByUuid(mapping.getUserDomainUuid())
                    .ifPresent(domain -> allDomains.add(domain.getDomainName()));
        }

        // Get domains from organisation mappings
        List<UserOrganisationDomainMapping> orgMappings =
                userOrganisationDomainMappingRepository.findActiveByUser(userUuid);
        for (UserOrganisationDomainMapping mapping : orgMappings) {
            userDomainRepository.findByUuid(mapping.getDomainUuid())
                    .ifPresent(domain -> allDomains.add(domain.getDomainName()));
        }

        return new ArrayList<>(allDomains);
    }

    // ================================
    // EMAIL SENDING METHODS (ASYNC) - UPDATED WITH COMPLETE IMPLEMENTATIONS
    // ================================

    @Async
    void sendInvitationEmailAsync(Invitation invitation, Organisation organisation,
                                  TrainingBranch branch, UserDomain domain) {
        try {
            if (branch != null) {
                emailUtility.sendBranchInvitation(
                        invitation.getRecipientEmail(),
                        invitation.getRecipientName(),
                        organisation.getName(),
                        branch.getBranchName(),
                        branch.getAddress(),
                        domain.getDomainName(),
                        invitation.getInviterName(),
                        invitation.getToken()
                );
            } else {
                emailUtility.sendOrganizationInvitation(
                        invitation.getRecipientEmail(),
                        invitation.getRecipientName(),
                        organisation.getName(),
                        null,
                        domain.getDomainName(),
                        null, // branchName is null for org-level invitations
                        invitation.getInviterName(),
                        invitation.getToken()
                );
            }
            log.info("Successfully sent invitation email for invitation {}", invitation.getUuid());
        } catch (MessagingException e) {
            log.error("Failed to send invitation email for invitation {}: {}", invitation.getUuid(), e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error sending invitation email for invitation {}: {}", invitation.getUuid(), e.getMessage());
        }
    }

    @Async
    void sendAcceptanceConfirmationEmailAsync(Invitation invitation, User user) {
        try {
            Organisation organisation = findOrganisationOrThrow(invitation.getOrganisationUuid());
            TrainingBranch branch = invitation.getBranchUuid() != null ?
                    findTrainingBranchOrThrow(invitation.getBranchUuid()) : null;
            String domainName = getDomainNameByUuid(invitation.getDomainUuid());

            emailUtility.sendInvitationAcceptedConfirmation(
                    user.getEmail(),
                    user.getFirstName() + " " + user.getLastName(),
                    organisation.getName(),
                    branch != null ? branch.getBranchName() : null,
                    domainName
            );
            log.info("Successfully sent acceptance confirmation email for invitation {}", invitation.getUuid());
        } catch (MessagingException e) {
            log.error("Failed to send acceptance confirmation email for invitation {}: {}",
                    invitation.getUuid(), e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error sending acceptance confirmation email for invitation {}: {}",
                    invitation.getUuid(), e.getMessage());
        }
    }

    @Async
    void sendDeclineNotificationEmailAsync(Invitation invitation, User user) {
        try {
            User inviter = findUserOrThrow(invitation.getInviterUuid());
            Organisation organisation = findOrganisationOrThrow(invitation.getOrganisationUuid());
            TrainingBranch branch = invitation.getBranchUuid() != null ?
                    findTrainingBranchOrThrow(invitation.getBranchUuid()) : null;

            emailUtility.sendInvitationDeclinedNotification(
                    inviter.getEmail(),
                    inviter.getFirstName() + " " + inviter.getLastName(),
                    user.getFirstName() + " " + user.getLastName(),
                    user.getEmail(),
                    organisation.getName(),
                    branch != null ? branch.getBranchName() : null
            );
            log.info("Successfully sent decline notification email for invitation {}", invitation.getUuid());
        } catch (MessagingException e) {
            log.error("Failed to send decline notification email for invitation {}: {}",
                    invitation.getUuid(), e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error sending decline notification email for invitation {}: {}",
                    invitation.getUuid(), e.getMessage());
        }
    }

    @Async
    void sendExpiryReminderEmailAsync(Invitation invitation) {
        try {
            log.debug("Sending expiry reminder for invitation {}", invitation.getUuid());

            // Get related entities for email content
            Organisation organisation = findOrganisationOrThrow(invitation.getOrganisationUuid());
            TrainingBranch branch = invitation.getBranchUuid() != null ?
                    findTrainingBranchOrThrow(invitation.getBranchUuid()) : null;
            UserDomain domain = findDomainByUuidOrThrow(invitation.getDomainUuid());

            // Calculate hours remaining until expiry
            long hoursRemaining = java.time.Duration.between(
                    LocalDateTime.now(),
                    invitation.getExpiresAt()
            ).toHours();

            // Don't send reminder if already expired or expiring in less than 1 hour
            if (hoursRemaining <= 0) {
                log.debug("Skipping expiry reminder for invitation {} - already expired", invitation.getUuid());
                return;
            }

            if (hoursRemaining > 48) {
                log.debug("Skipping expiry reminder for invitation {} - too early ({}h remaining)",
                        invitation.getUuid(), hoursRemaining);
                return;
            }

            emailUtility.sendInvitationExpiryReminder(
                    invitation.getRecipientEmail(),
                    invitation.getRecipientName(),
                    organisation.getName(),
                    null,
                    domain.getDomainName(),
                    branch != null ? branch.getBranchName() : null,
                    invitation.getInviterName(),
                    invitation.getToken(),
                    hoursRemaining
            );

            log.info("Successfully sent expiry reminder email for invitation {} ({}h remaining)",
                    invitation.getUuid(), hoursRemaining);

        } catch (MessagingException e) {
            log.error("Failed to send expiry reminder email for invitation {}: {}",
                    invitation.getUuid(), e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error sending expiry reminder email for invitation {}: {}",
                    invitation.getUuid(), e.getMessage());
        }
    }

    // ================================
    // ENTITY FINDER METHODS
    // ================================

    private Invitation findInvitationByTokenOrThrow(String token) {
        return invitationRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found for token: " + token));
    }

    private Invitation findInvitationByUuidOrThrow(UUID uuid) {
        return invitationRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found for UUID: " + uuid));
    }

    private User findUserOrThrow(UUID uuid) {
        return userRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("User not found for UUID: " + uuid));
    }

    private Organisation findOrganisationOrThrow(UUID uuid) {
        return organisationRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Organisation not found for UUID: " + uuid));
    }

    private TrainingBranch findTrainingBranchOrThrow(UUID uuid) {
        return trainingBranchRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Training branch not found for UUID: " + uuid));
    }

    private UserDomain findDomainByNameOrThrow(String domainName) {
        return userDomainRepository.findByDomainName(domainName)
                .orElseThrow(() -> new IllegalArgumentException("Invalid domain name: " + domainName));
    }

    private UserDomain findDomainByUuidOrThrow(UUID uuid) {
        return userDomainRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Domain not found for UUID: " + uuid));
    }
}