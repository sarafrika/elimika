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
                organisation.getCode(),
                organisation.getLicenceNo(),
                organisation.getSlug(),
                organisation.getDomain(),
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
        organisation.setCode(organisationDTO.code());
        organisation.setLicenceNo(organisationDTO.licenceNo());
        organisation.setDomain(organisationDTO.domain());
        return organisation;
    }
}