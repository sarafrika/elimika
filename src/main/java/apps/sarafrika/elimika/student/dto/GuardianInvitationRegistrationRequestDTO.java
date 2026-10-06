package apps.sarafrika.elimika.student.dto;

import apps.sarafrika.elimika.shared.utils.validation.ValidPhoneNumber;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Creates an account for an invited guardian; the email is always the invited one. */
@Schema(name = "GuardianInvitationRegistrationRequest",
        description = "Details an invited guardian supplies to create their account from the invitation link.")
public record GuardianInvitationRegistrationRequestDTO(

        @Schema(description = "**[REQUIRED]** First name", example = "Mary", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name cannot exceed 100 characters")
        @JsonProperty("first_name")
        String firstName,

        @Schema(description = "**[REQUIRED]** Last name", example = "Doe", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name cannot exceed 100 characters")
        @JsonProperty("last_name")
        String lastName,

        @Schema(description = "**[OPTIONAL]** Mobile number", example = "+254712345678", nullable = true)
        @Size(max = 20, message = "Phone number cannot exceed 20 characters")
        @ValidPhoneNumber(mobileOnly = true)
        @JsonProperty("phone_number")
        String phoneNumber,

        @Schema(description = "**[REQUIRED]** Terms of use accepted", example = "true",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @AssertTrue(message = "The terms of use must be accepted")
        @JsonProperty("terms_accepted")
        boolean termsAccepted
) {
}
