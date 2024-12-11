package apps.sarafrika.elimika.instructor.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateAvailabilityPatternRequestDTO(
        @NotNull @NotBlank
        String patternType,

        @NotNull
        LocalDate startDate,

        LocalDate endDate
) {
}
