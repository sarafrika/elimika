package apps.sarafrika.elimika.course.api.dto.response;

import apps.sarafrika.elimika.course.domain.Course;
import apps.sarafrika.elimika.shared.utils.enums.DifficultyLevel;

import java.util.Set;
import java.util.stream.Collectors;

public record CourseResponseDTO(
        Long id,
        String name,
        String code,
        String description,
        DifficultyLevel difficultyLevel,
        int minAge,
        int maxAge,
        Set<InstructorResponseDTO> instructors
) {
    public static CourseResponseDTO from(Course course) {

        return new CourseResponseDTO(
                course.getId(),
                course.getName(),
                course.getCode(),
                course.getDescription(),
                DifficultyLevel.valueOf(course.getDifficultyLevel()),
                course.getMinAge(),
                course.getMaxAge(),
                course.getInstructors().stream().map(InstructorResponseDTO::from).collect(Collectors.toSet()));
    }
}
