package apps.sarafrika.elimika.wallet.dto;

import apps.sarafrika.elimika.shared.otp.IssuedCode;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

@Schema(name = "VerificationCode", description = "Where a one-time code was sent and how long it works")
public record VerificationCodeDTO(
        @Schema(description = "**[READ-ONLY]** Masked number the SMS went to.", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty("sent_to") String sentTo,
        @Schema(description = "**[READ-ONLY]** When the code stops working.", format = "date-time", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty("expires_at") OffsetDateTime expiresAt,
        @Schema(description = "**[READ-ONLY]** When another code may be requested.", format = "date-time",
                accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty("resend_available_at") OffsetDateTime resendAvailableAt
) {

    public static VerificationCodeDTO from(IssuedCode code) {
        return new VerificationCodeDTO(code.sentTo(), code.expiresAt(), code.resendAvailableAt());
    }
}
