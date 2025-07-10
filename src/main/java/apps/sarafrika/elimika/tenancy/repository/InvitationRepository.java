package apps.sarafrika.elimika.tenancy.repository;

import apps.sarafrika.elimika.tenancy.entity.Invitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for managing Invitation entities.
 * Provides data access methods for invitation lifecycle management.
 */
@Repository
public interface InvitationRepository extends JpaRepository<Invitation, Long> {

    // ================================
    // BASIC LOOKUP METHODS
    // ================================

    /**
     * Find invitation by UUID.
     */
    Optional<Invitation> findByUuid(UUID uuid);

    /**
     * Find invitation by token.
     */
    Optional<Invitation> findByToken(String token);

    /**
     * Find invitation by token and status.
     */
    Optional<Invitation> findByTokenAndStatus(String token, Invitation.InvitationStatus status);

    // ================================
    // INVITATION STATUS QUERIES
    // ================================

    /**
     * Find all pending invitations for a recipient email.
     */
    List<Invitation> findByRecipientEmailAndStatus(String recipientEmail, Invitation.InvitationStatus status);

    /**
     * Find all pending invitations for an organization.
     */
    List<Invitation> findByOrganisationUuidAndStatus(UUID organisationUuid, Invitation.InvitationStatus status);

    /**
     * Find all pending invitations for a training branch.
     */
    List<Invitation> findByBranchUuidAndStatus(UUID branchUuid, Invitation.InvitationStatus status);

    /**
     * Find all invitations sent by a specific user.
     */
    List<Invitation> findByInviterUuidOrderByCreatedDateDesc(UUID inviterUuid);

    // ================================
    // VALIDATION QUERIES
    // ================================

    /**
     * Check if there's already a pending invitation for email and organization.
     */
    boolean existsByRecipientEmailAndOrganisationUuidAndStatus(
            String recipientEmail,
            UUID organisationUuid,
            Invitation.InvitationStatus status);

    /**
     * Check if there's already a pending invitation for email, organization and branch.
     */
    boolean existsByRecipientEmailAndOrganisationUuidAndBranchUuidAndStatus(
            String recipientEmail,
            UUID organisationUuid,
            UUID branchUuid,
            Invitation.InvitationStatus status);

    // ================================
    // EXPIRATION MANAGEMENT
    // ================================

    /**
     * Find all expired pending invitations.
     */
    @Query("SELECT i FROM Invitation i WHERE i.status = 'PENDING' AND i.expiresAt < CURRENT_TIMESTAMP")
    List<Invitation> findExpiredPendingInvitations();

    /**
     * Find invitations expiring within specified hours.
     */
    @Query("SELECT i FROM Invitation i WHERE i.status = 'PENDING' AND i.expiresAt BETWEEN CURRENT_TIMESTAMP AND :expirationTime")
    List<Invitation> findInvitationsExpiringBefore(@Param("expirationTime") LocalDateTime expirationTime);

    /**
     * Mark expired invitations as expired.
     */
    @Modifying
    @Query("UPDATE Invitation i SET i.status = 'EXPIRED' WHERE i.status = 'PENDING' AND i.expiresAt < CURRENT_TIMESTAMP")
    int markExpiredInvitations();

    // ================================
    // STATISTICS QUERIES
    // ================================

    /**
     * Count invitations by status for an organization.
     */
    long countByOrganisationUuidAndStatus(UUID organisationUuid, Invitation.InvitationStatus status);

    /**
     * Count invitations by status for a training branch.
     */
    long countByBranchUuidAndStatus(UUID branchUuid, Invitation.InvitationStatus status);

    /**
     * Count invitations sent by a user.
     */
    long countByInviterUuid(UUID inviterUuid);

    // ================================
    // ADMIN QUERIES
    // ================================

    /**
     * Find all invitations for an organization (all statuses).
     */
    List<Invitation> findByOrganisationUuidOrderByCreatedDateDesc(UUID organisationUuid);

    /**
     * Find all invitations for a training branch (all statuses).
     */
    List<Invitation> findByBranchUuidOrderByCreatedDateDesc(UUID branchUuid);

    /**
     * Find invitations created within date range.
     */
    @Query("SELECT i FROM Invitation i WHERE i.createdDate BETWEEN :startDate AND :endDate ORDER BY i.createdDate DESC")
    List<Invitation> findByCreatedDateBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // ================================
    // CLEANUP QUERIES
    // ================================

    /**
     * Delete old invitations that are expired, declined, or cancelled.
     */
    @Modifying
    @Query("DELETE FROM Invitation i WHERE i.status IN ('EXPIRED', 'DECLINED', 'CANCELLED') AND i.createdDate < :cutoffDate")
    int deleteOldInvitations(@Param("cutoffDate") LocalDateTime cutoffDate);
}