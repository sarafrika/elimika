package apps.sarafrika.elimika.booking.dto;

import apps.sarafrika.elimika.shared.enums.BookingStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(
        name = "BookingResponse",
        description = "Booking details returned to clients"
)
public record BookingResponseDTO(

        @Schema(description = "Unique booking identifier", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("uuid")
        UUID uuid,

        @Schema(description = "UUID of the student who created the booking", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("student_uuid")
        UUID studentUuid,

        @Schema(description = "UUID of the course tied to the booking", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("course_uuid")
        UUID courseUuid,

        @Schema(description = "UUID of the instructor for the session", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("instructor_uuid")
        UUID instructorUuid,

        @Schema(description = "Start time for the session", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("start_time")
        LocalDateTime startTime,

        @Schema(description = "End time for the session", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("end_time")
        LocalDateTime endTime,

        @Schema(description = "Current status of the booking", requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("status")
        BookingStatus status,

        @Schema(description = "Price amount agreed for the booking")
        @JsonProperty("price_amount")
        BigDecimal priceAmount,

        @Schema(description = "ISO currency code for the booking price")
        @JsonProperty("currency")
        String currency,

        @Schema(description = "Payment session identifier from the payment engine")
        @JsonProperty("payment_session_id")
        String paymentSessionId,

        @Schema(description = "Payment reference supplied by the payment engine")
        @JsonProperty("payment_reference")
        String paymentReference,

        @Schema(description = "Payment engine used for this booking")
        @JsonProperty("payment_engine")
        String paymentEngine,

        @Schema(description = "When the hold on the slot expires if unpaid")
        @JsonProperty("hold_expires_at")
        LocalDateTime holdExpiresAt,

        @Schema(description = "UUID of the availability block created for this booking")
        @JsonProperty("availability_block_uuid")
        UUID availabilityBlockUuid,

        @Schema(description = "UUID of the scheduled class instance created for this booking")
        @JsonProperty("scheduled_instance_uuid")
        UUID scheduledInstanceUuid,

        @Schema(description = "UUID of the enrollment created for this booking")
        @JsonProperty("enrollment_uuid")
        UUID enrollmentUuid,

        @Schema(description = "Purpose or note for this booking")
        @JsonProperty("purpose")
        String purpose,

        @Schema(description = "Creation timestamp")
        @JsonProperty("created_date")
        LocalDateTime createdDate,

        @Schema(description = "Last update timestamp")
        @JsonProperty("updated_date")
        LocalDateTime updatedDate
) {
}
