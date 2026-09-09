package apps.sarafrika.elimika.tenancy.services;

import apps.sarafrika.elimika.tenancy.dto.OrganisationDocumentDTO;

import java.util.List;
import java.util.UUID;

public interface OrganisationDocumentService {

    OrganisationDocumentDTO createOrganisationDocument(OrganisationDocumentDTO organisationDocumentDTO);

    List<OrganisationDocumentDTO> getOrganisationDocuments(UUID organisationUuid);

    void deleteOrganisationDocument(UUID organisationUuid, UUID documentUuid);
}
