package apps.sarafrika.elimika.course.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateCategoryRequestDTO(

        @NotNull @NotBlank
        String name,

        String description
) {
}
