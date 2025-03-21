package apps.sarafrika.elimika.course.mappers;

import apps.sarafrika.elimika.course.dto.PrerequisiteGroupDTO;
import apps.sarafrika.elimika.course.model.PrerequisiteGroup;

/**
 * Utility class for mapping between PrerequisiteGroup entities and DTOs
 */
public class PrerequisiteGroupMapper {

    /**
     * Private constructor to prevent instantiation
     */
    private PrerequisiteGroupMapper() {
        // Utility class - do not instantiate
    }

    /**
     * Converts a PrerequisiteGroup entity to a PrerequisiteGroupDTO
     *
     * @param entity the PrerequisiteGroup entity to convert
     * @return the corresponding PrerequisiteGroupDTO
     */
    public static PrerequisiteGroupDTO toDto(PrerequisiteGroup entity) {
        if (entity == null) {
            return null;
        }

        return PrerequisiteGroupDTO.builder()
                .uuid(entity.getUuid())
                .courseUuid(entity.getCourseUuid())
                .groupType(entity.getGroupType())
                .createdBy(entity.getCreatedBy())
                .lastModifiedBy(entity.getLastModifiedBy())
                .createdDate(entity.getCreatedDate())
                .lastModifiedDate(entity.getLastModifiedDate())
                .build();
    }

    /**
     * Converts a PrerequisiteGroupDTO to a PrerequisiteGroup entity
     *
     * @param dto the PrerequisiteGroupDTO to convert
     * @return the corresponding PrerequisiteGroup entity
     */
    public static PrerequisiteGroup toEntity(PrerequisiteGroupDTO dto) {
        if (dto == null) {
            return null;
        }

        PrerequisiteGroup prerequisiteGroup = new PrerequisiteGroup();
        prerequisiteGroup.setUuid(dto.uuid());
        prerequisiteGroup.setCourseUuid(dto.courseUuid());
        prerequisiteGroup.setGroupType(dto.groupType());

        return prerequisiteGroup;
    }
}