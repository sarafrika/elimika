package apps.sarafrika.elimika.wallet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

@Schema(name = "PayoutBatchWithdrawalsRequest", description = "Withdrawals waiting for review to put in a draft batch")
public record PayoutBatchWithdrawalsRequestDTO(
        @Size(max = 500) @JsonProperty("withdrawal_uuids") List<UUID> withdrawalUuids
) {
}
