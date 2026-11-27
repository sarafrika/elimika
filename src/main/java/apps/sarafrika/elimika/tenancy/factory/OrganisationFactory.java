package apps.sarafrika.elimika.tenancy.factory;

import apps.sarafrika.elimika.tenancy.dto.OrganisationDTO;
import apps.sarafrika.elimika.tenancy.entity.Organisation;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class OrganisationFactory {
    public static OrganisationDTO toDTO(Organisation organisation) {
        return new OrganisationDTO(
                organisation.getUuid(),
                organisation.getName(),
                organisation.getDescription(),
                organisation.isActive(),
                organisation.getLicenceNo(),
                organisation.getLocation(),
                organisation.getCountry(),
                organisation.getSlug(),
                organisation.getLatitude(),
                organisation.getLongitude(),
                organisation.getAdminVerified(),
                organisation.getCreatedDate(),
                organisation.getLastModifiedDate(),
                organisation.getVerificationRequestedAt(),
                organisation.getOrganisationType(),
                organisation.getCapabilities() == null ? List.of() : List.copyOf(organisation.getCapabilities())
        );
    }

    public static Organisation toEntity(OrganisationDTO organisationDTO) {
        Organisation organisation = new Organisation();
        organisation.setUuid(organisationDTO.uuid());
        organisation.setName(organisationDTO.name());
        organisation.setDescription(organisationDTO.description());
        organisation.setActive(organisationDTO.active());
        organisation.setLicenceNo(organisationDTO.licenceNo());
        organisation.setLocation(organisationDTO.location());
        organisation.setCountry(organisationDTO.country());
        organisation.setSlug(organisationDTO.slug());
        organisation.setLatitude(organisationDTO.latitude());
        organisation.setLongitude(organisationDTO.longitude());
        organisation.setOrganisationType(organisationDTO.organisationType());
        if (organisationDTO.capabilities() != null) {
            organisation.setCapabilities(new ArrayList<>(organisationDTO.capabilities().stream().distinct().toList()));
        }
        return organisation;
    }
}
