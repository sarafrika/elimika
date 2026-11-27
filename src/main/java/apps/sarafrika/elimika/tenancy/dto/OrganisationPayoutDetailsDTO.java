package apps.sarafrika.elimika.tenancy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(name = "OrganisationPayoutDetails",
        description = "The M-Pesa number an organisation wants to be paid on. Checked by the platform admin who "
                + "verifies the organisation; no code is sent.")
public record OrganisationPayoutDetailsDTO(
        @Schema(description = "**[READ-ONLY]** Organisation these details belong to.", format = "uuid",
                accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "organisation_uuid", access = JsonProperty.Access.READ_ONLY) UUID organisationUuid,

        @Schema(description = "**[REQUIRED]** Kenyan M-Pesa number in any common form: 07…, 01…, 2547…, +254 7…. "
                + "Returned as 2547XXXXXXXX.", example = "0712345678", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @JsonProperty("mpesa_number") String mpesaNumber,

        @Schema(description = "**[READ-ONLY]** When the number was last changed.", format = "date-time",
                accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "updated_date", access = JsonProperty.Access.READ_ONLY) LocalDateTime updatedDate
) {
}
