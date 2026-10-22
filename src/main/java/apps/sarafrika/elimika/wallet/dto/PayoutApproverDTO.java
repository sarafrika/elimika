package apps.sarafrika.elimika.wallet.dto;

import apps.sarafrika.elimika.wallet.entity.PayoutApprover;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(name = "PayoutApprover", description = "An organisation admin who may manage payout numbers and withdraw")
public record PayoutApproverDTO(
        @Schema(description = "**[READ-ONLY]** The admin.", format = "uuid", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty("user_uuid") UUID userUuid,
        @Schema(description = "**[READ-ONLY]** Who named them.", format = "uuid", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty("added_by_user_uuid") UUID addedByUserUuid,
        @Schema(description = "**[READ-ONLY]** When.", format = "date-time", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty("created_date") LocalDateTime createdDate
) {

    public static PayoutApproverDTO from(PayoutApprover approver) {
        return new PayoutApproverDTO(approver.getUserUuid(), approver.getAddedByUserUuid(), approver.getCreatedDate());
    }
}
