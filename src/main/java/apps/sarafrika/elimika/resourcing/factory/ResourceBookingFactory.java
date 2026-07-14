package apps.sarafrika.elimika.resourcing.factory;

import apps.sarafrika.elimika.resourcing.dto.ResourceBookingDTO;
import apps.sarafrika.elimika.resourcing.model.ResourceBooking;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ResourceBookingFactory {

    public static ResourceBookingDTO toDTO(ResourceBooking entity) {
        if (entity == null) {
            return null;
        }
        return new ResourceBookingDTO(
                entity.getUuid(),
                entity.getResourceUuid(),
                entity.getOrganisationUuid(),
                entity.getStatus(),
                entity.getQuantity(),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getSourceType(),
                entity.getJobUuid(),
                entity.getClassDefinitionUuid(),
                entity.getScheduledInstanceUuid(),
                entity.getReleasedAt(),
                entity.getReleaseReason()
        );
    }

    public static List<ResourceBookingDTO> toDTOList(List<ResourceBooking> entities) {
        return entities == null ? List.of() : entities.stream().map(ResourceBookingFactory::toDTO).toList();
    }
}
