package apps.sarafrika.elimika.course.dto.response;

import apps.sarafrika.elimika.course.persistence.Course;
import apps.sarafrika.elimika.shared.utils.enums.DifficultyLevel;

import java.math.BigDecimal;
import java.util.List;

public record CourseResponseDTO(
        Long id,

        String name,

        String code,

        String description,

        BigDecimal durationHours,

        DifficultyLevel difficultyLevel,

        int minAge,

        int maxAge,

        int classLimit,

        PricingResponseDTO pricing,

        List<CourseLearningObjectiveResponseDTO> learningObjectives,

        List<CategoryResponseDTO> categories
) {
    public static CourseResponseDTO from(Course course, PricingResponseDTO pricing, List<CourseLearningObjectiveResponseDTO> learningObjectives, List<CategoryResponseDTO> categories) {

        return new CourseResponseDTO(
                course.getId(),
                course.getName(),
                course.getCode(),
                course.getDescription(),
                course.getDurationHours(),
                DifficultyLevel.valueOf(course.getDifficultyLevel()),
                course.getMinAge(),
                course.getMaxAge(),
                course.getClassLimit(),
                pricing,
                learningObjectives,
                categories
        );
    }
}
