package apps.sarafrika.elimika.tenancy.services;

import apps.sarafrika.elimika.tenancy.dto.OrganisationDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;
import java.util.UUID;

public interface OrganisationService {
    OrganisationDTO createOrganisation(OrganisationDTO organisationDTO);

    OrganisationDTO getOrganisationByUuid(UUID uuid);

    Page<OrganisationDTO> getAllOrganisations(Pageable pageable);

    OrganisationDTO updateOrganisation(UUID uuid, OrganisationDTO organisationDTO);

    void deleteOrganisation(UUID uuid);

    Page<OrganisationDTO> search(Map<String, String> searchParams, Pageable pageable);
}
