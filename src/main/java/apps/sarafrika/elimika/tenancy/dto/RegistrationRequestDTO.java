package apps.sarafrika.elimika.tenancy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(name = "RegistrationRequest", description = """
        Starts a self-registration. Personal details are written to Keycloak only; Elimika records the
        chosen domain, which a platform admin approves before any dashboard opens. Keycloak then emails
        a link to set a password and verify the address.
        """)
public record RegistrationRequestDTO(

        @NotBlank
        @Size(max = 50)
        @JsonProperty("first_name")
        String firstName,

        @Size(max = 50)
        @JsonProperty("middle_name")
        String middleName,

        @NotBlank
        @Size(max = 50)
        @JsonProperty("last_name")
        String lastName,

        @NotBlank
        @Email
        @Size(max = 50)
        @JsonProperty("email")
        String email,

        @NotBlank
        @Pattern(regexp = "^\\+?[0-9 ]{7,20}$", message = "Phone number must contain 7 to 20 digits")
        @JsonProperty("phone_number")
        String phoneNumber,

        @Past
        @JsonProperty("dob")
        LocalDate dob,

        @Schema(allowableValues = {"MALE", "FEMALE", "PREFER_NOT_TO_SAY"})
        @JsonProperty("gender")
        String gender,

        @NotBlank
        @Schema(description = "The domain to register into", example = "course_creator",
                allowableValues = {"student", "instructor", "course_creator", "organisation_user"})
        @JsonProperty("domain")
        String domain,

        @AssertTrue(message = "The terms of use must be accepted")
        @JsonProperty("terms_accepted")
        boolean termsAccepted,

        @Schema(description = "Turnstile token, required when captcha verification is enabled")
        @JsonProperty("captcha_token")
        String captchaToken
) {
}
