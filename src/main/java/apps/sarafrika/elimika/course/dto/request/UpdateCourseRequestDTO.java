package apps.sarafrika.elimika.course.dto.request;

import apps.sarafrika.elimika.shared.utils.enums.DifficultyLevel;

public record UpdateCourseRequestDTO(
        String name,

        String description,

        DifficultyLevel difficultyLevel,

        int minAge,

        int maxAge,

        UpdateCoursePricingRequestDTO pricing
) {
}