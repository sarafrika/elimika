package apps.sarafrika.elimika.tenancy.repository;

import apps.sarafrika.elimika.tenancy.entity.BulkInvitationUpload;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BulkInvitationUploadRepository extends JpaRepository<BulkInvitationUpload, Long> {

    boolean existsByOrganisationUuidAndFileHash(UUID organisationUuid, String fileHash);
}
