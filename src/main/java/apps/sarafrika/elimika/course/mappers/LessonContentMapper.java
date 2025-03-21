package apps.sarafrika.elimika.course.mappers;

import apps.sarafrika.elimika.course.dto.LessonContentDTO;
import apps.sarafrika.elimika.course.model.LessonContent;

/**
 * Utility class for mapping between LessonContent entities and DTOs
 */
public class LessonContentMapper {

    /**
     * Private constructor to prevent instantiation
     */
    private LessonContentMapper() {
        // Utility class - do not instantiate
    }

    /**
     * Converts a LessonContent entity to a LessonContentDTO
     *
     * @param entity the LessonContent entity to convert
     * @return the corresponding LessonContentDTO
     */
    public static LessonContentDTO toDto(LessonContent entity) {
        if (entity == null) {
            return null;
        }

        return LessonContentDTO.builder()
                .uuid(entity.getUuid())
                .title(entity.getTitle())
                .content(entity.getContent())
                .displayOrder(entity.getDisplayOrder())
                .duration(entity.getDuration())
                .lessonUuid(entity.getLessonUuid())
                .contentTypeUuid(entity.getContentTypeUuid())
                .createdBy(entity.getCreatedBy())
                .lastModifiedBy(entity.getLastModifiedBy())
                .createdDate(entity.getCreatedDate())
                .lastModifiedDate(entity.getLastModifiedDate())
                .build();
    }

    /**
     * Converts a LessonContentDTO to a LessonContent entity
     *
     * @param dto the LessonContentDTO to convert
     * @return the corresponding LessonContent entity
     */
    public static LessonContent toEntity(LessonContentDTO dto) {
        if (dto == null) {
            return null;
        }

        LessonContent content = new LessonContent();
        content.setUuid(dto.uuid());
        content.setTitle(dto.title());
        content.setContent(dto.content());
        content.setDisplayOrder(dto.displayOrder());
        content.setDuration(dto.duration());
        content.setLessonUuid(dto.lessonUuid());
        content.setContentTypeUuid(dto.contentTypeUuid());

        return content;
    }
}