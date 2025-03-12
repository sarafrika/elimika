package apps.sarafrika.elimika.course.dto.request;

import apps.sarafrika.elimika.shared.utils.enums.DifficultyLevel;

import java.math.BigDecimal;
import java.util.List;

public record UpdateCourseRequestDTO(
        String name,

        String description,

        String thumbnailUrl,

        BigDecimal durationHours,

        DifficultyLevel difficultyLevel,

        int minAge,

        int maxAge,

        PricingRequestDTO pricing,

        List<UpdateCourseLearningObjectiveRequestDTO> learningObjectives,

        List<UpdateCourseCategoryRequestDTO> categories
) {
}