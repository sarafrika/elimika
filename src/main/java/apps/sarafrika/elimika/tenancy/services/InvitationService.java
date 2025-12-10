package apps.sarafrika.elimika.tenancy.services;

import apps.sarafrika.elimika.tenancy.dto.InvitationDTO;
import apps.sarafrika.elimika.tenancy.dto.InvitationPreviewDTO;
import apps.sarafrika.elimika.tenancy.dto.UserDTO;
import apps.sarafrika.elimika.tenancy.dto.BulkInvitationResultDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for managing organization and branch invitations.
 * Handles the invitation lifecycle from creation to acceptance/decline.
 */
public interface InvitationService {

    // ================================
    // INVITATION CREATION
    // ================================

    /**
     * Creates and sends an organization invitation via email.
     *
     * @param recipientEmail the recipient's email address
     * @param recipientName the recipient's full name
     * @param organisationUuid the organization UUID
     * @param domainName the role/domain being offered
     * @param branchUuid optional training branch UUID
     * @param inviterUuid the UUID of the user sending the invitation
     * @param notes optional notes for the invitation
     * @return the created invitation
     */
    InvitationDTO createOrganisationInvitation(
            String recipientEmail,
            String recipientName,
            UUID organisationUuid,
            String domainName,
            UUID branchUuid,
            UUID inviterUuid,
            String notes);

    /**
     * Processes a CSV or XLSX upload to create multiple invitations.
     *
     * @param organisationUuid the organisation the invitations target
     * @param inviterUuid the UUID of the user sending all invitations in the batch
     * @param file the uploaded CSV or XLSX file
     * @return summary of processed invitations and row-level errors
     */
    BulkInvitationResultDTO processBulkInvitations(UUID organisationUuid, UUID inviterUuid, MultipartFile file);

    /**
     * Creates and sends a training branch invitation via email.
     *
     * @param recipientEmail the recipient's email address
     * @param recipientName the recipient's full name
     * @param branchUuid the training branch UUID
     * @param domainName the role/domain being offered
     * @param inviterUuid the UUID of the user sending the invitation
     * @param notes optional notes for the invitation
     * @return the created invitation
     */
    InvitationDTO createBranchInvitation(
            String recipientEmail,
            String recipientName,
            UUID branchUuid,
            String domainName,
            UUID inviterUuid,
            String notes);

    // ================================
    // INVITATION MANAGEMENT
    // ================================

    /**
     * Accepts an invitation and creates the user-organization relationship.
     *
     * @param token the invitation token
     * @param userUuid the UUID of the accepting user
     * @return the user data with new organization relationship
     */
    UserDTO acceptInvitation(String token, UUID userUuid);

    /**
     * Declines an invitation.
     *
     * @param token the invitation token
     * @param userUuid the UUID of the declining user (for validation)
     */
    void declineInvitation(String token, UUID userUuid);

    /**
     * Cancels a pending invitation.
     *
     * @param invitationUuid the invitation UUID
     * @param cancellerUuid the UUID of the user cancelling (must be inviter or admin)
     */
    void cancelInvitation(UUID invitationUuid, UUID cancellerUuid);

    /**
     * Resends an invitation email.
     *
     * @param invitationUuid the invitation UUID
     * @param resenderUuid the UUID of the user resending (must be inviter or admin)
     */
    void resendInvitation(UUID invitationUuid, UUID resenderUuid);

    // ================================
    // INVITATION QUERIES
    // ================================

    /**
     * Gets invitation details by token.
     *
     * @param token the invitation token
     * @return the invitation details
     */
    InvitationDTO getInvitationByToken(String token);

    /**
     * Gets all pending invitations for a recipient email.
     *
     * @param recipientEmail the recipient's email address
     * @return list of pending invitations
     */
    List<InvitationDTO> getPendingInvitationsForEmail(String recipientEmail);

    /**
     * Gets all invitations for an organization.
     *
     * @param organisationUuid the organization UUID
     * @return list of invitations
     */
    List<InvitationDTO> getOrganisationInvitations(UUID organisationUuid);

    /**
     * Gets all invitations for a training branch.
     *
     * @param branchUuid the training branch UUID
     * @return list of invitations
     */
    List<InvitationDTO> getBranchInvitations(UUID branchUuid);

    /**
     * Gets all invitations sent by a user.
     *
     * @param inviterUuid the inviter's UUID
     * @return list of sent invitations
     */
    List<InvitationDTO> getInvitationsSentByUser(UUID inviterUuid);

    // ================================
    // VALIDATION METHODS
    // ================================

    /**
     * Validates if an invitation token is valid and not expired.
     *
     * @param token the invitation token
     * @return true if valid and not expired
     */
    boolean isInvitationValid(String token);

    /**
     * Checks if there's already a pending invitation for email and organization.
     *
     * @param recipientEmail the recipient's email
     * @param organisationUuid the organization UUID
     * @return true if pending invitation exists
     */
    boolean hasPendingInvitation(String recipientEmail, UUID organisationUuid);

    /**
     * Checks if there's already a pending invitation for email, organization and branch.
     *
     * @param recipientEmail the recipient's email
     * @param organisationUuid the organization UUID
     * @param branchUuid the training branch UUID
     * @return true if pending invitation exists
     */
    boolean hasPendingBranchInvitation(String recipientEmail, UUID organisationUuid, UUID branchUuid);

    // ================================
    // REACT FRONTEND INTEGRATION
    // ================================

    /**
     * Gets public-safe invitation preview details by token (no authentication required).
     * Used by React frontend to display invitation details before user login/registration.
     *
     * @param token the invitation token
     * @return invitation preview with public-safe information
     * @throws ResourceNotFoundException if token is invalid or invitation not found
     */
    InvitationPreviewDTO previewInvitation(String token);

    /**
     * Accepts an invitation for an authenticated user (Keycloak-authenticated).
     * Used by React frontend after user authentication to process invitation acceptance.
     * Automatically determines user UUID from authenticated context.
     *
     * @param token the invitation token
     * @param authenticatedUserEmail the email of the authenticated user (from JWT/Keycloak)
     * @return the user data with new organization relationship
     * @throws IllegalArgumentException if token is invalid or user email doesn't match invitation
     * @throws IllegalStateException if invitation is expired or already processed
     */
    UserDTO acceptInvitationAuthenticated(String token, String authenticatedUserEmail);

    /**
     * Declines an invitation for an authenticated user (Keycloak-authenticated).
     * Used by React frontend after user authentication to process invitation decline.
     *
     * @param token the invitation token
     * @param authenticatedUserEmail the email of the authenticated user (from JWT/Keycloak)
     * @throws IllegalArgumentException if token is invalid or user email doesn't match invitation
     * @throws IllegalStateException if invitation is expired or already processed
     */
    void declineInvitationAuthenticated(String token, String authenticatedUserEmail);

    /**
     * Processes all pending invitations for a newly authenticated user.
     * Automatically accepts valid invitations that match the user's email.
     * Called after successful Keycloak authentication/registration.
     *
     * @param userEmail the authenticated user's email
     * @return list of accepted invitations
     */
    List<InvitationDTO> processPendingInvitationsForUser(String userEmail);

    // ================================
    // MAINTENANCE METHODS
    // ================================

    /**
     * Marks expired invitations as expired.
     *
     * @return number of invitations marked as expired
     */
    int markExpiredInvitations();

    /**
     * Sends reminder emails for invitations expiring soon.
     *
     * @param hoursBeforeExpiry hours before expiry to send reminder
     * @return number of reminder emails sent
     */
    int sendExpiryReminders(int hoursBeforeExpiry);

    /**
     * Cleans up old invitations (expired, declined, cancelled).
     *
     * @param daysOld invitations older than this many days will be deleted
     * @return number of invitations deleted
     */
    int cleanupOldInvitations(int daysOld);
}
