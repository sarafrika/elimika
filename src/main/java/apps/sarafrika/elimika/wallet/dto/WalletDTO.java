package apps.sarafrika.elimika.wallet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(
        name = "Wallet",
        description = "User wallet summary for a specific currency"
)
public record WalletDTO(

        @Schema(
                description = "**[READ-ONLY]** Unique identifier for the wallet.",
                format = "uuid",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty("uuid")
        UUID uuid,

        @Schema(
                description = "**[READ-ONLY]** User UUID that owns this wallet.",
                format = "uuid",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty("user_uuid")
        UUID userUuid,

        @Schema(
                description = "**[READ-ONLY]** ISO currency code for this wallet balance.",
                example = "KES",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty("currency_code")
        String currencyCode,

        @Schema(
                description = "**[READ-ONLY]** Current wallet balance.",
                example = "1500.00",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty("balance_amount")
        BigDecimal balanceAmount,

        @Schema(
                description = "**[READ-ONLY]** Wallet creation timestamp.",
                format = "date-time",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty("created_date")
        LocalDateTime createdDate,

        @Schema(
                description = "**[READ-ONLY]** Wallet last update timestamp.",
                format = "date-time",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty("updated_date")
        LocalDateTime updatedDate
) {
}
