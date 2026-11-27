package apps.sarafrika.elimika.tenancy.factory;

import apps.sarafrika.elimika.tenancy.dto.OrganisationDTO;
import apps.sarafrika.elimika.tenancy.entity.Organisation;
import apps.sarafrika.elimika.tenancy.util.enums.OrganisationCapability;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

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
                capabilities(organisation.getCapabilities())
        );
    }


    /** Stored values in declaration order; unknown values (none pass the DB CHECK) are dropped. */
    public static List<OrganisationCapability> capabilities(String[] stored) {
        if (stored == null) {
            return List.of();
        }
        Set<String> values = Set.of(stored);
        return Arrays.stream(OrganisationCapability.values())
                .filter(capability -> values.contains(capability.getValue()))
                .toList();
    }

    /** Deduplicated, in declaration order. */
    public static String[] toStored(List<OrganisationCapability> capabilities) {
        if (capabilities == null) {
            return new String[0];
        }
        return Arrays.stream(OrganisationCapability.values())
                .filter(capabilities::contains)
                .map(OrganisationCapability::getValue)
                .toArray(String[]::new);
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
        organisation.setCapabilities(toStored(organisationDTO.capabilities()));
        return organisation;
    }
}