package apps.sarafrika.elimika.course.mappers;

import apps.sarafrika.elimika.course.dto.PrerequisiteDTO;
import apps.sarafrika.elimika.course.model.Prerequisite;

/**
 * Utility class for mapping between Prerequisite entities and DTOs
 */
public class PrerequisiteMapper {

    /**
     * Private constructor to prevent instantiation
     */
    private PrerequisiteMapper() {
        // Utility class - do not instantiate
    }

    /**
     * Converts a Prerequisite entity to a PrerequisiteDTO
     *
     * @param entity the Prerequisite entity to convert
     * @return the corresponding PrerequisiteDTO
     */
    public static PrerequisiteDTO toDto(Prerequisite entity) {
        if (entity == null) {
            return null;
        }

        return PrerequisiteDTO.builder()
                .uuid(entity.getUuid())
                .courseUuid(entity.getCourseUuid())
                .requiredForCourseUuid(entity.getRequiredForCourseUuid())
                .minimumScore(entity.getMinimumScore())
                .prerequisiteTypeUuid(entity.getPrerequisiteTypeUuid())
                .createdBy(entity.getCreatedBy())
                .lastModifiedBy(entity.getLastModifiedBy())
                .createdDate(entity.getCreatedDate())
                .lastModifiedDate(entity.getLastModifiedDate())
                .build();
    }

    /**
     * Converts a PrerequisiteDTO to a Prerequisite entity
     *
     * @param dto the PrerequisiteDTO to convert
     * @return the corresponding Prerequisite entity
     */
    public static Prerequisite toEntity(PrerequisiteDTO dto) {
        if (dto == null) {
            return null;
        }

        Prerequisite prerequisite = new Prerequisite();
        prerequisite.setUuid(dto.uuid());
        prerequisite.setCourseUuid(dto.courseUuid());
        prerequisite.setRequiredForCourseUuid(dto.requiredForCourseUuid());
        prerequisite.setMinimumScore(dto.minimumScore());
        prerequisite.setPrerequisiteTypeUuid(dto.prerequisiteTypeUuid());

        return prerequisite;
    }
}