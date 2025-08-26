package apps.sarafrika.elimika.tenancy.factory;

import apps.sarafrika.elimika.tenancy.dto.OrganisationDTO;
import apps.sarafrika.elimika.tenancy.entity.Organisation;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class OrganisationFactory {
    public static OrganisationDTO toDTO(Organisation organisation) {
        return new OrganisationDTO(
                organisation.getUuid(),
                organisation.getName(),
                organisation.getDescription(),
                organisation.isActive(),
                organisation.getLicenceNo(),
                organisation.getUserUuid(),
                organisation.getLocation(),
                organisation.getCountry(),
                organisation.getCreatedDate(),
                organisation.getLastModifiedDate()
        );
    }

    public static Organisation toEntity(OrganisationDTO organisationDTO) {
        Organisation organisation = new Organisation();
        organisation.setUuid(organisationDTO.uuid());
        organisation.setName(organisationDTO.name());
        organisation.setDescription(organisationDTO.description());
        organisation.setActive(organisationDTO.active());
        organisation.setLicenceNo(organisationDTO.licenceNo());
        organisation.setUserUuid(organisationDTO.userUuid());
        organisation.setLocation(organisationDTO.location());
        organisation.setCountry(organisationDTO.country());
        return organisation;
    }
}