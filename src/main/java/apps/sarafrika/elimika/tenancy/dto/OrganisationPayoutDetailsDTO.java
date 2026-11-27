package apps.sarafrika.elimika.tenancy.dto;

import apps.sarafrika.elimika.tenancy.util.enums.OrganisationPayoutChannel;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(name = "OrganisationPayoutDetails",
        description = "Where an organisation wants to be paid: an M-Pesa Paybill or Till. Checked by the platform "
                + "admin who verifies the organisation; no code is sent.")
public record OrganisationPayoutDetailsDTO(
        @Schema(description = "**[READ-ONLY]** Organisation these details belong to.", format = "uuid",
                accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "organisation_uuid", access = JsonProperty.Access.READ_ONLY) UUID organisationUuid,

        @Schema(description = "**[REQUIRED]** `MPESA_PAYBILL` or `MPESA_TILL`.", example = "MPESA_PAYBILL",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @JsonProperty("channel") OrganisationPayoutChannel channel,

        @Schema(description = "**[REQUIRED]** Paybill business number or Till number, 5 to 7 digits.", example = "522522",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Pattern(regexp = "^\\s*[0-9]{5,7}\\s*$", message = "Enter the 5 to 7 digit Paybill or Till number")
        @JsonProperty("business_number") String businessNumber,

        @Schema(description = "**[PAYBILL ONLY]** Account to credit on the Paybill; required for `MPESA_PAYBILL`, "
                + "must be omitted for `MPESA_TILL`.", example = "1234567890")
        @Size(max = 20)
        @JsonProperty("account_number") String accountNumber,

        @Schema(description = "**[REQUIRED]** Name the Paybill or Till is registered under.", example = "Acme Academy Ltd",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank
        @Size(max = 120)
        @JsonProperty("account_name") String accountName,

        @Schema(description = "**[READ-ONLY]** When the details were last changed.", format = "date-time",
                accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "updated_date", access = JsonProperty.Access.READ_ONLY) LocalDateTime updatedDate
) {
}
