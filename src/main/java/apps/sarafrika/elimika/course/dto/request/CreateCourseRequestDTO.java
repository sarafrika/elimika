package apps.sarafrika.elimika.course.dto.request;

import apps.sarafrika.elimika.course.dto.response.CategoryResponseDTO;
import apps.sarafrika.elimika.shared.utils.enums.DifficultyLevel;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public record CreateCourseRequestDTO(
        String name,

        String description,

        String thumbnailUrl,

        DifficultyLevel difficultyLevel,

        BigDecimal durationHours,

        int minAge,

        int maxAge,

        PricingRequestDTO pricing,

        List<CreateCourseLearningObjectiveRequestDTO> learningObjectives,

        List<UpdateCourseCategoryRequestDTO> categories,

        Set<Long>instructorIds
) {
}

