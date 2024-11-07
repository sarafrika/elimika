package apps.sarafrika.elimika.course.dto.response;

import apps.sarafrika.elimika.course.persistence.Course;
import apps.sarafrika.elimika.shared.utils.enums.DifficultyLevel;

public record CourseResponseDTO(
        Long id,

        String name,

        String code,

        String description,

        DifficultyLevel difficultyLevel,

        int minAge,

        int maxAge,

        CoursePricingResponseDTO pricing
) {
    public static CourseResponseDTO from(Course course, CoursePricingResponseDTO pricing) {

        return new CourseResponseDTO(
                course.getId(),
                course.getName(),
                course.getCode(),
                course.getDescription(),
                DifficultyLevel.valueOf(course.getDifficultyLevel()),
                course.getMinAge(),
                course.getMaxAge(),
                pricing
        );
    }
}
