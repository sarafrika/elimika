package apps.sarafrika.elimika.tenancy.entity;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Audit record for bulk invitation uploads keyed by file hash to prevent duplicates.
 */
@Entity
@Table(name = "bulk_invitation_uploads")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BulkInvitationUpload extends BaseEntity {

    @Column(name = "organisation_uuid")
    private UUID organisationUuid;

    @Column(name = "inviter_uuid")
    private UUID inviterUuid;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_hash")
    private String fileHash;
}
