package apps.sarafrika.elimika.payout.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Records that an organisation has paid an instructor off-platform.
 * <p>
 * The reference is mandatory. The platform does not move this money and therefore holds no proof of
 * its own; a settlement with no reference is an unevidenced assertion that the debt disappeared.
 */
@Schema(
        name = "InstructorObligationSettlementRequest",
        description = "Evidence that an organisation has paid an instructor outside the platform"
)
public record InstructorObligationSettlementRequestDTO(

        @Schema(
                description = "The organisation's own reference for the payment: bank reference, mobile money code, payroll run id",
                requiredMode = Schema.RequiredMode.REQUIRED,
                example = "MPESA-QGH7XK2P1L"
        )
        @JsonProperty("settlement_reference")
        @NotBlank(message = "A settlement reference is required")
        @Size(max = 128, message = "Settlement reference must not exceed 128 characters")
        String settlementReference,

        @Schema(description = "Optional note about the payment")
        @JsonProperty("note")
        String note

) {
}
