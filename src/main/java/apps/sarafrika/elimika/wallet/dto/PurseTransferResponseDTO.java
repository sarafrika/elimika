package apps.sarafrika.elimika.wallet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

@Schema(name = "PurseTransferResponse", description = "Money moved between two of one person's purses")
public record PurseTransferResponseDTO(
        @Schema(description = "Shared by the TRANSFER_OUT row of the source purse and the TRANSFER_IN row of the "
                + "target purse")
        @JsonProperty("transfer_reference") UUID transferReference,
        @JsonProperty("amount") BigDecimal amount,
        @JsonProperty("currency_code") String currencyCode,
        @Schema(description = "The source purse's row after the move")
        @JsonProperty("from_wallet") WalletDTO fromWallet,
        @Schema(description = "The target purse's row after the move")
        @JsonProperty("to_wallet") WalletDTO toWallet
) {
}
