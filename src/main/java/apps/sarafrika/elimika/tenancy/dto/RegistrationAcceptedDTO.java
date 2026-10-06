package apps.sarafrika.elimika.tenancy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/** Same answer whether or not the email was registered, so the endpoint reveals nobody. */
@Schema(name = "RegistrationAccepted")
public record RegistrationAcceptedDTO(

        @Schema(example = "If the address can be registered, a link to set your password is on its way.")
        @JsonProperty("message")
        String message
) {
}
