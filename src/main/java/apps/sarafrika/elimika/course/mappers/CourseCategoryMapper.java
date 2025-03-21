package apps.sarafrika.elimika.course.mappers;

import apps.sarafrika.elimika.course.dto.CourseCategoryDTO;
import apps.sarafrika.elimika.course.model.CourseCategory;

/**
 * Utility class for mapping between CourseCategory entities and DTOs
 */
public class CourseCategoryMapper {

    /**
     * Private constructor to prevent instantiation
     */
    private CourseCategoryMapper() {
        // Utility class - do not instantiate
    }

    /**
     * Converts a CourseCategory entity to a CourseCategoryDTO
     *
     * @param entity the CourseCategory entity to convert
     * @return the corresponding CourseCategoryDTO
     */
    public static CourseCategoryDTO toDto(CourseCategory entity) {
        if (entity == null) {
            return null;
        }

        return new CourseCategoryDTO(
                entity.getCourseId(),
                entity.getCategoryId(),
                entity.getCreatedBy(),
                entity.getLastModifiedBy(),
                entity.getCreatedDate(),
                entity.getLastModifiedDate()
        );
    }

    /**
     * Converts a CourseCategoryDTO to a CourseCategory entity
     *
     * @param dto the CourseCategoryDTO to convert
     * @return the corresponding CourseCategory entity
     */
    public static CourseCategory toEntity(CourseCategoryDTO dto) {
        if (dto == null) {
            return null;
        }

        CourseCategory courseCategory = new CourseCategory();
        courseCategory.setCourseId(dto.courseUuid());
        courseCategory.setCategoryId(dto.categoryUuid());

        return courseCategory;
    }
}