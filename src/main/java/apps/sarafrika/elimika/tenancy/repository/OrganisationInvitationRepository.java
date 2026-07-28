package apps.sarafrika.elimika.tenancy.repository;

import apps.sarafrika.elimika.tenancy.entity.OrganisationInvitation;
import apps.sarafrika.elimika.tenancy.util.enums.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link OrganisationInvitation}.
 * <p>
 * Lookups by token go through the stored hash - the raw token is never persisted.
 *
 * @author Wilfred Njuguna
 * @since 1.0
 */
@Repository
public interface OrganisationInvitationRepository extends JpaRepository<OrganisationInvitation, Long> {

    Optional<OrganisationInvitation> findByUuid(UUID uuid);

    Optional<OrganisationInvitation> findByTokenHash(String tokenHash);

    Optional<OrganisationInvitation> findByGuardianConsentTokenHash(String guardianConsentTokenHash);

    List<OrganisationInvitation> findByOrganisationUuidOrderByCreatedDateDesc(UUID organisationUuid);

    List<OrganisationInvitation> findByOrganisationUuidAndStatusInOrderByCreatedDateDesc(
            UUID organisationUuid, Collection<InvitationStatus> statuses);

    /**
     * Finds the live offer for an email within an organisation, if one exists. Backs the
     * "one live offer per recipient" rule enforced by {@code uk_org_invitation_live_recipient}.
     */
    @Query("""
            SELECT i FROM OrganisationInvitation i
            WHERE i.organisationUuid = :organisationUuid
              AND LOWER(i.recipientEmail) = LOWER(:email)
              AND i.status IN (apps.sarafrika.elimika.tenancy.util.enums.InvitationStatus.PENDING,
                               apps.sarafrika.elimika.tenancy.util.enums.InvitationStatus.AWAITING_GUARDIAN_CONSENT)
            """)
    Optional<OrganisationInvitation> findLiveByOrganisationAndEmail(
            @Param("organisationUuid") UUID organisationUuid, @Param("email") String email);

    /**
     * All live offers addressed to an email address, across organisations. Backs the
     * in-app invitation inbox.
     */
    @Query("""
            SELECT i FROM OrganisationInvitation i
            WHERE LOWER(i.recipientEmail) = LOWER(:email)
              AND i.status IN (apps.sarafrika.elimika.tenancy.util.enums.InvitationStatus.PENDING,
                               apps.sarafrika.elimika.tenancy.util.enums.InvitationStatus.AWAITING_GUARDIAN_CONSENT)
            ORDER BY i.createdDate DESC
            """)
    List<OrganisationInvitation> findLiveByRecipientEmail(@Param("email") String email);

    /**
     * Live offers past their expiry, for the expiry sweeper.
     */
    @Query("""
            SELECT i FROM OrganisationInvitation i
            WHERE i.expiresAt < :now
              AND i.status IN (apps.sarafrika.elimika.tenancy.util.enums.InvitationStatus.PENDING,
                               apps.sarafrika.elimika.tenancy.util.enums.InvitationStatus.AWAITING_GUARDIAN_CONSENT)
            """)
    List<OrganisationInvitation> findLapsed(@Param("now") LocalDateTime now);

    long countByOrganisationUuidAndStatus(UUID organisationUuid, InvitationStatus status);
}
