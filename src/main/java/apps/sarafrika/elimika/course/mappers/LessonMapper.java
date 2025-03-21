package apps.sarafrika.elimika.course.mappers;

import apps.sarafrika.elimika.course.dto.LessonDTO;
import apps.sarafrika.elimika.course.model.Lesson;

import java.util.ArrayList;

/**
 * Utility class for mapping between Lesson entities and DTOs
 */
public class LessonMapper {

    /**
     * Private constructor to prevent instantiation
     */
    private LessonMapper() {
        // Utility class - do not instantiate
    }

    /**
     * Converts a Lesson entity to a LessonDTO
     *
     * @param entity the Lesson entity to convert
     * @return the corresponding LessonDTO
     */
    public static LessonDTO toDto(Lesson entity) {
        if (entity == null) {
            return null;
        }

        return LessonDTO.builder()
                .uuid(entity.getUuid())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .lessonOrder(entity.getLessonOrder())
                .isPublished(entity.isPublished())
                .courseUuid(entity.getCourseUuid())
                .content(new ArrayList<>()) // needs to be populated separately
                .resources(new ArrayList<>()) // needs to be populated separately
                .createdBy(entity.getCreatedBy())
                .lastModifiedBy(entity.getLastModifiedBy())
                .createdDate(entity.getCreatedDate())
                .lastModifiedDate(entity.getLastModifiedDate())
                .build();
    }

    /**
     * Converts a LessonDTO to a Lesson entity
     *
     * @param dto the LessonDTO to convert
     * @return the corresponding Lesson entity
     */
    public static Lesson toEntity(LessonDTO dto) {
        if (dto == null) {
            return null;
        }

        Lesson lesson = new Lesson();
        lesson.setUuid(dto.uuid());
        lesson.setTitle(dto.title());
        lesson.setDescription(dto.description());
        lesson.setLessonOrder(dto.lessonOrder());
        lesson.setPublished(dto.isPublished());
        lesson.setCourseUuid(dto.courseUuid());

        return lesson;
    }
}