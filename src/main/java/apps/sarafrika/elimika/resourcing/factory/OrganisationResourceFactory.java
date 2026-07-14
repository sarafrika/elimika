package apps.sarafrika.elimika.resourcing.factory;

import apps.sarafrika.elimika.resourcing.dto.OrganisationResourceDTO;
import apps.sarafrika.elimika.resourcing.model.OrganisationResource;
import apps.sarafrika.elimika.resourcing.spi.ResourceSummary;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class OrganisationResourceFactory {

    public static OrganisationResourceDTO toDTO(OrganisationResource entity) {
        if (entity == null) {
            return null;
        }
        return new OrganisationResourceDTO(
                entity.getUuid(),
                entity.getOrganisationUuid(),
                entity.getBranchUuid(),
                entity.getResourceType(),
                entity.getName(),
                entity.getDescription(),
                entity.getSeatCapacity(),
                entity.getTotalQuantity(),
                entity.getLocationName(),
                entity.getLocationLatitude(),
                entity.getLocationLongitude(),
                entity.getIsActive(),
                entity.getCreatedDate(),
                entity.getLastModifiedDate()
        );
    }

    public static List<OrganisationResourceDTO> toDTOList(List<OrganisationResource> entities) {
        return entities == null ? List.of() : entities.stream().map(OrganisationResourceFactory::toDTO).toList();
    }

    public static OrganisationResource toEntity(OrganisationResourceDTO dto) {
        if (dto == null) {
            return null;
        }
        OrganisationResource entity = new OrganisationResource();
        updateEntityFromDTO(entity, dto);
        return entity;
    }

    public static void updateEntityFromDTO(OrganisationResource entity, OrganisationResourceDTO dto) {
        if (entity == null || dto == null) {
            return;
        }
        entity.setBranchUuid(dto.branchUuid());
        entity.setResourceType(dto.resourceType());
        entity.setName(dto.name());
        entity.setDescription(dto.description());
        entity.setSeatCapacity(dto.seatCapacity());
        entity.setTotalQuantity(dto.totalQuantity());
        entity.setLocationName(dto.locationName());
        entity.setLocationLatitude(dto.locationLatitude());
        entity.setLocationLongitude(dto.locationLongitude());
        if (dto.isActive() != null) {
            entity.setIsActive(dto.isActive());
        }
    }

    public static ResourceSummary toSummary(OrganisationResource entity) {
        if (entity == null) {
            return null;
        }
        return new ResourceSummary(
                entity.getUuid(),
                entity.getOrganisationUuid(),
                entity.getBranchUuid(),
                entity.getResourceType(),
                entity.getName(),
                entity.getSeatCapacity(),
                entity.getTotalQuantity(),
                Boolean.TRUE.equals(entity.getIsActive())
        );
    }
}
