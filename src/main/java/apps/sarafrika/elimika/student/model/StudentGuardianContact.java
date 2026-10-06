package apps.sarafrika.elimika.student.model;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import apps.sarafrika.elimika.student.util.converter.GuardianContactStatusConverter;
import apps.sarafrika.elimika.student.util.converter.GuardianRelationshipTypeConverter;
import apps.sarafrika.elimika.student.util.enums.GuardianContactStatus;
import apps.sarafrika.elimika.student.util.enums.GuardianRelationshipType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/** A guardian a student named: linked, or holding an emailed invitation (only its token hash is stored). */
@Entity
@Table(name = "student_guardian_contacts")
@Getter
@Setter
@NoArgsConstructor
public class StudentGuardianContact extends BaseEntity {

    @Column(name = "student_uuid")
    private UUID studentUuid;

    @Column(name = "guardian_email")
    private String guardianEmail;

    @Column(name = "guardian_name")
    private String guardianName;

    @Column(name = "guardian_phone")
    private String guardianPhone;

    @Convert(converter = GuardianRelationshipTypeConverter.class)
    @Column(name = "relationship_type")
    private GuardianRelationshipType relationshipType;

    @Column(name = "position")
    private int position;

    @Convert(converter = GuardianContactStatusConverter.class)
    @Column(name = "contact_status")
    private GuardianContactStatus status;

    @Column(name = "guardian_user_uuid")
    private UUID guardianUserUuid;

    @Column(name = "link_uuid")
    private UUID linkUuid;

    @Column(name = "token_hash")
    private String tokenHash;

    @Column(name = "invitation_expires_at")
    private LocalDateTime invitationExpiresAt;

    @Column(name = "invitation_sent_at")
    private LocalDateTime invitationSentAt;

    @Column(name = "invitation_send_count")
    private int invitationSendCount;

    @Column(name = "linked_at")
    private LocalDateTime linkedAt;

    @Column(name = "declined_at")
    private LocalDateTime declinedAt;

    @Column(name = "removed_at")
    private LocalDateTime removedAt;

    @Column(name = "invited_by")
    private UUID invitedBy;

    public boolean isInvitationOpen(LocalDateTime now) {
        return status == GuardianContactStatus.INVITED
                && tokenHash != null
                && invitationExpiresAt != null
                && invitationExpiresAt.isAfter(now);
    }
}
