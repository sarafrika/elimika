package apps.sarafrika.elimika.tenancy.repository;

import apps.sarafrika.elimika.tenancy.entity.OrganisationDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganisationDocumentRepository extends JpaRepository<OrganisationDocument, Long>,
        JpaSpecificationExecutor<OrganisationDocument> {

    Optional<OrganisationDocument> findByUuid(UUID uuid);

    List<OrganisationDocument> findByOrganisationUuid(UUID organisationUuid);

    List<OrganisationDocument> findByOrganisationUuidAndDocumentTypeUuid(UUID organisationUuid, UUID documentTypeUuid);

    void deleteByUuid(UUID uuid);
}
