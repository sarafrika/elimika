package apps.sarafrika.elimika.course.mappers;

import apps.sarafrika.elimika.course.dto.PrerequisiteTypeDTO;
import apps.sarafrika.elimika.course.model.PrerequisiteType;

/**
 * Utility class for mapping between PrerequisiteType entities and DTOs
 */
public class PrerequisiteTypeMapper {

    /**
     * Private constructor to prevent instantiation
     */
    private PrerequisiteTypeMapper() {
        // Utility class - do not instantiate
    }

    /**
     * Converts a PrerequisiteType entity to a PrerequisiteTypeDTO
     *
     * @param entity the PrerequisiteType entity to convert
     * @return the corresponding PrerequisiteTypeDTO
     */
    public static PrerequisiteTypeDTO toDto(PrerequisiteType entity) {
        if (entity == null) {
            return null;
        }

        return PrerequisiteTypeDTO.builder()
                .uuid(entity.getUuid())
                .name(entity.getName())
                .createdBy(entity.getCreatedBy())
                .lastModifiedBy(entity.getLastModifiedBy())
                .createdDate(entity.getCreatedDate())
                .lastModifiedDate(entity.getLastModifiedDate())
                .build();
    }

    /**
     * Converts a PrerequisiteTypeDTO to a PrerequisiteType entity
     *
     * @param dto the PrerequisiteTypeDTO to convert
     * @return the corresponding PrerequisiteType entity
     */
    public static PrerequisiteType toEntity(PrerequisiteTypeDTO dto) {
        if (dto == null) {
            return null;
        }

        PrerequisiteType prerequisiteType = new PrerequisiteType();
        prerequisiteType.setUuid(dto.uuid());
        prerequisiteType.setName(dto.name());

        return prerequisiteType;
    }
}