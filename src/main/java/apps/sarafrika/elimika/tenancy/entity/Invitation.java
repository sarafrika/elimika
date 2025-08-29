package apps.sarafrika.elimika.tenancy.entity;

import apps.sarafrika.elimika.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing an invitation to join an organization or training branch.
 * Tracks the invitation lifecycle from creation to acceptance/decline.
 */
@Entity
@Table(name = "invitations")
@Getter @Setter @ToString
@SuperBuilder
@NoArgsConstructor @AllArgsConstructor
public class Invitation extends BaseEntity {

    @Column(name = "token")
    private String token;

    @Column(name = "recipient_email")
    private String recipientEmail;

    @Column(name = "recipient_name")
    private String recipientName;

    @Column(name = "organisation_uuid")
    private UUID organisationUuid;

    @Column(name = "branch_uuid")
    private UUID branchUuid;

    @Column(name = "domain_uuid")
    private UUID domainUuid;

    @Column(name = "inviter_uuid")
    private UUID inviterUuid;

    @Column(name = "inviter_name")
    private String inviterName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private InvitationStatus status = InvitationStatus.PENDING;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "declined_at")
    private LocalDateTime declinedAt;

    @Column(name = "user_uuid")
    private UUID userUuid;

    @Column(name = "notes")
    private String notes;

    /**
     * Enumeration for invitation status.
     */
    public enum InvitationStatus {
        PENDING,
        ACCEPTED,
        DECLINED,
        EXPIRED,
        CANCELLED
    }

    /**
     * Checks if the invitation is still valid (not expired and pending).
     */
    public boolean isValid() {
        return status == InvitationStatus.PENDING &&
                expiresAt.isAfter(LocalDateTime.now());
    }

    /**
     * Marks the invitation as expired.
     */
    public void expire() {
        this.status = InvitationStatus.EXPIRED;
    }

    /**
     * Accepts the invitation.
     */
    public void accept(UUID userUuid) {
        this.status = InvitationStatus.ACCEPTED;
        this.acceptedAt = LocalDateTime.now();
        this.userUuid = userUuid;
    }

    /**
     * Declines the invitation.
     */
    public void decline() {
        this.status = InvitationStatus.DECLINED;
        this.declinedAt = LocalDateTime.now();
    }

    /**
     * Cancels the invitation.
     */
    public void cancel() {
        this.status = InvitationStatus.CANCELLED;
    }
}