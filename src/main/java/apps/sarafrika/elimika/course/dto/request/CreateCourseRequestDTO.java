package apps.sarafrika.elimika.course.dto.request;

public record CreateCourseRequestDTO(
        String name,
        String description,
        String difficultyLevel,
        int minAge,
        int maxAge
) {
}
