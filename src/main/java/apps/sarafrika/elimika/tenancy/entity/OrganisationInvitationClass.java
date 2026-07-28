package apps.sarafrika.elimika.tenancy.entity;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

/**
 * A class named in an invitation.
 * <p>
 * These are <em>surfaced</em> to the invitee once they accept - they are never
 * auto-enrolled. Enrolment remains a separate action so the commerce paywall and the
 * student's own commitment decision stay intact.
 *
 * @author Wilfred Njuguna
 * @version 1.0
 * @since 2026-07-28
 */
@Entity
@Table(name = "organisation_invitation_classes")
@Getter
@Setter
@ToString
@NoArgsConstructor
public class OrganisationInvitationClass extends BaseEntity {

    @Column(name = "invitation_uuid")
    private UUID invitationUuid;

    @Column(name = "class_definition_uuid")
    private UUID classDefinitionUuid;

    public OrganisationInvitationClass(UUID invitationUuid, UUID classDefinitionUuid) {
        this.invitationUuid = invitationUuid;
        this.classDefinitionUuid = classDefinitionUuid;
    }
}
