package apps.sarafrika.elimika.booking.dto;

import apps.sarafrika.elimika.shared.enums.LocationType;
import apps.sarafrika.elimika.shared.enums.SessionFormat;
import apps.sarafrika.elimika.shared.utils.enums.RateBasis;
import apps.sarafrika.elimika.shared.validation.ValidTimeRange;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(
        name = "CreateBookingRequest",
        description = "Request payload for creating a booking for an instructor and course. The server prices it "
                + "from the instructor's approved rate card for the chosen format, delivery and basis."
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

        @Schema(description = "Private (INDIVIDUAL) or GROUP training; picks the rate card row the booking is priced from",
                allowableValues = {"INDIVIDUAL", "GROUP"}, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Training format is required")
        @JsonProperty("training_format")
        SessionFormat trainingFormat,

        @Schema(description = "How the session is delivered; HYBRID is priced from the in-person rates",
                allowableValues = {"ONLINE", "IN_PERSON", "HYBRID"}, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Delivery mode is required")
        @JsonProperty("delivery_mode")
        LocationType deliveryMode,

        @Schema(description = "Unit the instructor's approved rate is charged in",
                allowableValues = {"per_hour", "per_session", "per_day"}, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Rate basis is required")
        @JsonProperty("rate_basis")
        RateBasis rateBasis,

        @Schema(description = "IANA timezone deciding the class day a per-day rate is charged on. Defaults to UTC.",
                example = "Africa/Nairobi", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @JsonProperty("timezone")
        String timezone,

        @Schema(description = "Optional purpose or note for this booking", maxLength = 500)
        @Size(max = 500, message = "Purpose must not exceed 500 characters")
        @JsonProperty("purpose")
        String purpose
) {
}
