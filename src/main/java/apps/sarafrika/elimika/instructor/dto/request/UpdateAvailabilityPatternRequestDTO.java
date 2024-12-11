package apps.sarafrika.elimika.instructor.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record UpdateAvailabilityPatternRequestDTO(
        @NotNull
        Long id,

        @NotNull @NotBlank
        String patternType,

        @NotNull
        LocalDate startDate,

        LocalDate endDate,

        LocalDateTime createdAt,

        String createdBy,

        LocalDateTime updatedAt,

        String updatedBy

) {
}
