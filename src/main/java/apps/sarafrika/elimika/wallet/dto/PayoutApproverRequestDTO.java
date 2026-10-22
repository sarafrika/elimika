package apps.sarafrika.elimika.wallet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(name = "PayoutApproverRequest", description = "An admin of the organisation to name as payout approver")
public record PayoutApproverRequestDTO(
        @Schema(description = "The admin's user uuid.", format = "uuid", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @JsonProperty("user_uuid") UUID userUuid
) {
}
