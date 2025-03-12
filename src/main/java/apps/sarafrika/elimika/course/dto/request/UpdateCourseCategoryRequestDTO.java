package apps.sarafrika.elimika.course.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateCourseCategoryRequestDTO(
        Long id,

        @NotNull @NotBlank
        String name,

        String description
) {
}
