package apps.sarafrika.elimika.tenancy.entity;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import apps.sarafrika.elimika.tenancy.util.enums.InvitationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A token-bearing offer for someone to join an organisation.
 * <p>
 * An invitation is deliberately <em>not</em> an affiliation. The corresponding
 * {@link UserOrganisationDomainMapping} row is written only when the invitee - or, for a
 * minor, their guardian - explicitly accepts.
 * <p>
 * {@code guardianRelationshipType} is held as a plain string rather than the student
 * module's {@code GuardianRelationshipType} enum because the Spring Modulith boundary
 * forbids a {@code tenancy -> student} dependency. Allowed values are enforced by the
 * check constraint on the table and validated at the service boundary.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2026-07-28
 */
@Entity
@Table(name = "organisation_invitations")
@Getter
@Setter
@ToString(exclude = {"tokenHash", "guardianConsentTokenHash"})
@NoArgsConstructor
public class OrganisationInvitation extends BaseEntity {

    @Column(name = "token_hash")
    private String tokenHash;

    @Column(name = "organisation_uuid")
    private UUID organisationUuid;

    @Column(name = "branch_uuid")
    private UUID branchUuid;

    @Column(name = "domain_uuid")
    private UUID domainUuid;

    @Column(name = "recipient_email")
    private String recipientEmail;

    @Column(name = "recipient_name")
    private String recipientName;

    /** Set at send time when the invited email already belongs to a platform user. */
    @Column(name = "recipient_user_uuid")
    private UUID recipientUserUuid;

    @Column(name = "inviter_user_uuid")
    private UUID inviterUserUuid;

    @Column(name = "status")
    private InvitationStatus status;

    @Column(name = "message")
    private String message;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "declined_at")
    private LocalDateTime declinedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "accepted_user_uuid")
    private UUID acceptedUserUuid;

    // ================================
    // GUARDIAN CONSENT (MINORS)
    // ================================

    @Column(name = "requires_guardian_consent")
    private boolean requiresGuardianConsent;

    @Column(name = "guardian_email")
    private String guardianEmail;

    @Column(name = "guardian_name")
    private String guardianName;

    /** One of PARENT, GUARDIAN, SPONSOR. See the class-level note on typing. */
    @Column(name = "guardian_relationship_type")
    private String guardianRelationshipType;

    @Column(name = "guardian_phone")
    private String guardianPhone;

    @Column(name = "guardian_consent_token_hash")
    private String guardianConsentTokenHash;

    @Column(name = "guardian_consented_at")
    private LocalDateTime guardianConsentedAt;

    @Column(name = "guardian_user_uuid")
    private UUID guardianUserUuid;

    /**
     * Whether this offer can still be acted upon: live status and not past its expiry.
     */
    public boolean isActionable() {
        return status != null
                && status.isLive()
                && expiresAt != null
                && expiresAt.isAfter(LocalDateTime.now());
    }
}
