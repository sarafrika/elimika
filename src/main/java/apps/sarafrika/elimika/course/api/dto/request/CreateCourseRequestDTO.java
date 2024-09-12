package apps.sarafrika.elimika.course.api.dto.request;

import apps.sarafrika.elimika.shared.utils.enums.DifficultyLevel;

import java.util.Set;

public record CreateCourseRequestDTO(
        String name,

        String description,

        DifficultyLevel difficultyLevel,

        int minAge,

        int maxAge,

        Set<Long> instructors

) {
}

