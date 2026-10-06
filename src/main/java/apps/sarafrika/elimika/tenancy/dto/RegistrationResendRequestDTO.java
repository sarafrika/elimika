package apps.sarafrika.elimika.tenancy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "RegistrationResendRequest", description = "Asks for the set-password email again")
public record RegistrationResendRequestDTO(

        @NotBlank
        @Email
        @JsonProperty("email")
        String email,

        @JsonProperty("captcha_token")
        String captchaToken
) {
}
