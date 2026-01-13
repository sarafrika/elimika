package apps.sarafrika.elimika.wallet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

@Schema(
        name = "WalletTransferResponse",
        description = "Summary of a wallet transfer operation"
)
public record WalletTransferResponse(

        @Schema(
                description = "**[READ-ONLY]** Transfer reference UUID linking both wallet entries.",
                format = "uuid",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty("transfer_reference")
        UUID transferReference,

        @Schema(
                description = "**[READ-ONLY]** Amount transferred.",
                example = "250.00",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty("amount")
        BigDecimal amount,

        @Schema(
                description = "**[READ-ONLY]** ISO currency code for the transfer.",
                example = "KES",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty("currency_code")
        String currencyCode,

        @Schema(
                description = "**[READ-ONLY]** Updated source wallet summary.",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty("source_wallet")
        WalletDTO sourceWallet,

        @Schema(
                description = "**[READ-ONLY]** Updated target wallet summary.",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty("target_wallet")
        WalletDTO targetWallet
) {
}
