package apps.sarafrika.elimika.booking.dto;

import apps.sarafrika.elimika.shared.validation.ValidTimeRange;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(
        name = "CreateBookingRequest",
        description = "Request payload for creating a booking for an instructor and course"
)
@ValidTimeRange(startField = "startTime", endField = "endTime", message = "End time must be after start time")
public record CreateBookingRequestDTO(

        @Schema(description = "UUID of the student creating the booking", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Student UUID is required")
        @JsonProperty("student_uuid")
        UUID studentUuid,

        @Schema(description = "UUID of the course being booked", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Course UUID is required")
        @JsonProperty("course_uuid")
        UUID courseUuid,

        @Schema(description = "UUID of the instructor for the session", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Instructor UUID is required")
        @JsonProperty("instructor_uuid")
        UUID instructorUuid,

        @Schema(description = "Start time for the requested session", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Start time is required")
        @JsonProperty("start_time")
        LocalDateTime startTime,

        @Schema(description = "End time for the requested session", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "End time is required")
        @JsonProperty("end_time")
        LocalDateTime endTime,

        @Schema(description = "Agreed price for the session", example = "50.00", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @DecimalMin(value = "0.0", inclusive = false, message = "Price amount must be positive")
        @JsonProperty("price_amount")
        BigDecimal priceAmount,

        @Schema(description = "ISO currency code (e.g., USD, KES)", example = "USD", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Pattern(regexp = "^[A-Za-z]{3}$", message = "Currency must be a 3-letter ISO code")
        @JsonProperty("currency")
        String currency,

        @Schema(description = "Optional purpose or note for this booking", maxLength = 500)
        @Size(max = 500, message = "Purpose must not exceed 500 characters")
        @JsonProperty("purpose")
        String purpose
) {
}
