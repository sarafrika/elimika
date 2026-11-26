package apps.sarafrika.elimika.student.dto;

import apps.sarafrika.elimika.shared.utils.validation.ValidPhoneNumber;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Creates an account for an invited guardian; the email must be the invited one. */
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

        @Schema(description = "**[REQUIRED]** The invited email address, typed by the guardian to confirm it. "
                + "Must match the invitation (case-insensitive); the invitation only shows it masked.",
                example = "mary.doe@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        @Size(max = 50, message = "Email cannot exceed 50 characters")
        @JsonProperty("email")
        String email,

        @Schema(description = "**[OPTIONAL]** Mobile number", example = "+254712345678", nullable = true)
        @Size(max = 20, message = "Phone number cannot exceed 20 characters")
        @ValidPhoneNumber(mobileOnly = true)
        @JsonProperty("phone_number")
        String phoneNumber,

        @Schema(description = "**[REQUIRED]** Date of birth. Guardians must be adults.", example = "1985-04-12",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Date of birth is required")
        @Past(message = "Date of birth must be in the past")
        @JsonProperty("dob")
        LocalDate dob,

        @Schema(description = "**[OPTIONAL]** Gender", example = "FEMALE", nullable = true,
                allowableValues = {"MALE", "FEMALE", "PREFER_NOT_TO_SAY"})
        @JsonProperty("gender")
        String gender,

        @Schema(description = "**[REQUIRED]** Password for signing in with the email. Keycloak's password policy "
                + "also applies; a password it rejects answers 400.", example = "S3cure-passphrase",
                requiredMode = Schema.RequiredMode.REQUIRED, accessMode = Schema.AccessMode.WRITE_ONLY,
                format = "password")
        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
        @JsonProperty(value = "password", access = JsonProperty.Access.WRITE_ONLY)
        String password,

        @Schema(description = "**[REQUIRED]** Terms of use accepted", example = "true",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @AssertTrue(message = "The terms of use must be accepted")
        @JsonProperty("terms_accepted")
        boolean termsAccepted
) {

    /** Keeps the password out of logs. */
    @Override
    public String toString() {
        return "GuardianInvitationRegistrationRequestDTO[firstName=%s, lastName=%s, email=%s, phoneNumber=%s, dob=%s, gender=%s, password=***, termsAccepted=%s]"
                .formatted(firstName, lastName, email, phoneNumber, dob, gender, termsAccepted);
    }
}
