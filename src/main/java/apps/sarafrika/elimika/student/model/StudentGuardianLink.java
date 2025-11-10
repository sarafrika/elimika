package apps.sarafrika.elimika.student.model;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import apps.sarafrika.elimika.student.util.enums.GuardianLinkStatus;
import apps.sarafrika.elimika.student.util.enums.GuardianRelationshipType;
import apps.sarafrika.elimika.student.util.enums.GuardianShareScope;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.Builder.Default;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "student_guardian_links")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class StudentGuardianLink extends BaseEntity {

    @Column(name = "student_uuid")
    private UUID studentUuid;

    @Column(name = "guardian_user_uuid")
    private UUID guardianUserUuid;

    @Enumerated(EnumType.STRING)
    @Column(name = "relationship_type")
    private GuardianRelationshipType relationshipType;

    @Enumerated(EnumType.STRING)
    @Column(name = "share_scope")
    @Default
    private GuardianShareScope shareScope = GuardianShareScope.FULL;

    @Enumerated(EnumType.STRING)
    @Column(name = "link_status")
    @Default
    private GuardianLinkStatus status = GuardianLinkStatus.PENDING;

    @Column(name = "is_primary")
    private boolean primaryGuardian;

    @Column(name = "invited_by")
    private UUID invitedBy;

    @Column(name = "linked_date")
    private LocalDateTime linkedDate;

    @Column(name = "revoked_date")
    private LocalDateTime revokedDate;

    @Column(name = "revoked_by")
    private UUID revokedBy;

    @Column(name = "notes")
    private String notes;
}
