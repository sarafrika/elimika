package apps.sarafrika.elimika.course.dto.response;

import apps.sarafrika.elimika.course.persistence.CourseCategory;

public record CourseCategoryResponseDTO(Long id, Long courseId, Long categoryId) {

    public static CourseCategoryResponseDTO from(CourseCategory courseCategory) {

        return new CourseCategoryResponseDTO(courseCategory.getId(), courseCategory.getCourseId(), courseCategory.getCategoryId());
    }
}
