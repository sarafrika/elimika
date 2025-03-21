package apps.sarafrika.elimika.course.mappers;

import apps.sarafrika.elimika.course.dto.LessonResourceDTO;
import apps.sarafrika.elimika.course.model.LessonResource;

/**
 * Utility class for mapping between LessonResource entities and DTOs
 */
public class LessonResourceMapper {

    /**
     * Private constructor to prevent instantiation
     */
    private LessonResourceMapper() {
        // Utility class - do not instantiate
    }

    /**
     * Converts a LessonResource entity to a LessonResourceDTO
     *
     * @param entity the LessonResource entity to convert
     * @return the corresponding LessonResourceDTO
     */
    public static LessonResourceDTO toDto(LessonResource entity) {
        if (entity == null) {
            return null;
        }

        return LessonResourceDTO.builder()
                .uuid(entity.getUuid())
                .title(entity.getTitle())
                .resourceUrl(entity.getResourceUrl())
                .displayOrder(entity.getDisplayOrder())
                .lessonUuid(entity.getLessonUuid())
                .createdBy(entity.getCreatedBy())
                .lastModifiedBy(entity.getLastModifiedBy())
                .createdDate(entity.getCreatedDate())
                .lastModifiedDate(entity.getLastModifiedDate())
                .build();
    }

    /**
     * Converts a LessonResourceDTO to a LessonResource entity
     *
     * @param dto the LessonResourceDTO to convert
     * @return the corresponding LessonResource entity
     */
    public static LessonResource toEntity(LessonResourceDTO dto) {
        if (dto == null) {
            return null;
        }

        LessonResource resource = new LessonResource();
        resource.setUuid(dto.uuid());
        resource.setTitle(dto.title());
        resource.setResourceUrl(dto.resourceUrl());
        resource.setDisplayOrder(dto.displayOrder());
        resource.setLessonUuid(dto.lessonUuid());

        return resource;
    }
}