package apps.sarafrika.elimika.wallet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

@Schema(
        name = "WalletCreditRequest",
        description = "Payload for crediting a user's wallet"
)
public record WalletCreditRequest(

        @Schema(
                description = "**[REQUIRED]** Amount to credit the wallet.",
                example = "500.00",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be greater than zero")
        @JsonProperty("amount")
        BigDecimal amount,

        @Schema(
                description = "**[OPTIONAL]** ISO currency code. Defaults to platform currency when omitted.",
                example = "KES"
        )
        @Size(max = 3, message = "Currency code must not exceed 3 characters")
        @JsonProperty("currency_code")
        String currencyCode,

        @Schema(
                description = "**[OPTIONAL]** External reference for the credit source.",
                example = "PAY-2025-0001"
        )
        @Size(max = 128, message = "Reference must not exceed 128 characters")
        @JsonProperty("reference")
        String reference,

        @Schema(
                description = "**[OPTIONAL]** Description for the credit entry.",
                example = "Wallet deposit via M-Pesa"
        )
        @JsonProperty("description")
        String description
) {
}
