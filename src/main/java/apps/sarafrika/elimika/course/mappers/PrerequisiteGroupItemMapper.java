package apps.sarafrika.elimika.course.mappers;

import apps.sarafrika.elimika.course.dto.PrerequisiteGroupItemDTO;
import apps.sarafrika.elimika.course.model.PrerequisiteGroupItem;

import java.util.UUID;

/**
 * Utility class for mapping between PrerequisiteGroupItem entities and DTOs
 */
public class PrerequisiteGroupItemMapper {

    /**
     * Private constructor to prevent instantiation
     */
    private PrerequisiteGroupItemMapper() {
        // Utility class - do not instantiate
    }

    /**
     * Converts a PrerequisiteGroupItem entity to a PrerequisiteGroupItemDTO
     *
     * @param entity the PrerequisiteGroupItem entity to convert
     * @return the corresponding PrerequisiteGroupItemDTO
     */
    public static PrerequisiteGroupItemDTO toDto(PrerequisiteGroupItem entity) {
        if (entity == null) {
            return null;
        }

        return new PrerequisiteGroupItemDTO(
                // Convert Long to UUID for prerequisite group UUID
                entity.getPrerequisiteGroupUuid() != null
                        ? UUID.fromString(entity.getPrerequisiteGroupUuid().toString())
                        : null,
                // Convert Long to UUID for prerequisite UUID
                entity.getPrerequisiteUuid() != null
                        ? UUID.fromString(entity.getPrerequisiteUuid().toString())
                        : null,
                entity.getCreatedBy(),
                entity.getLastModifiedBy(),
                entity.getCreatedDate(),
                entity.getLastModifiedDate()
        );
    }

    /**
     * Converts a PrerequisiteGroupItemDTO to a PrerequisiteGroupItem entity
     *
     * @param dto the PrerequisiteGroupItemDTO to convert
     * @return the corresponding PrerequisiteGroupItem entity
     */
    public static PrerequisiteGroupItem toEntity(PrerequisiteGroupItemDTO dto) {
        if (dto == null) {
            return null;
        }

        PrerequisiteGroupItem prerequisiteGroupItem = new PrerequisiteGroupItem();

        // Convert UUID to Long for prerequisite group UUID
        prerequisiteGroupItem.setPrerequisiteGroupUuid(
                dto.prerequisiteGroupUuid() != null
                        ? dto.prerequisiteGroupUuid().getLeastSignificantBits()
                        : null
        );

        // Convert UUID to Long for prerequisite UUID
        prerequisiteGroupItem.setPrerequisiteUuid(
                dto.prerequisiteUuid() != null
                        ? dto.prerequisiteUuid().getLeastSignificantBits()
                        : null
        );

        return prerequisiteGroupItem;
    }
}