package apps.sarafrika.elimika.course.dto.response;

import apps.sarafrika.elimika.course.model.Course;

public record CourseResponseDTO(
        Long id,
        String name,
        String code,
        String description,
        String difficultyLevel,
        int minAge,
        int maxAge
) {
    public static CourseResponseDTO from(Course course) {
        return new CourseResponseDTO(
                course.getId(),
                course.getName(),
                course.getCode(),
                course.getDescription(),
                course.getDifficultyLevel(),
                course.getMinAge(),
                course.getMaxAge()
        );
    }
}
