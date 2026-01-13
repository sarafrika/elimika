package apps.sarafrika.elimika.wallet.dto;

import apps.sarafrika.elimika.wallet.enums.WalletTransactionType;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(
        name = "WalletTransaction",
        description = "Wallet ledger entry capturing balance changes"
)
public record WalletTransactionDTO(

        @Schema(
                description = "**[READ-ONLY]** Unique identifier for the transaction.",
                format = "uuid",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty("uuid")
        UUID uuid,

        @Schema(
                description = "**[READ-ONLY]** Wallet UUID linked to this transaction.",
                format = "uuid",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty("wallet_uuid")
        UUID walletUuid,

        @Schema(
                description = "**[READ-ONLY]** Transaction category for this wallet entry.",
                example = "DEPOSIT",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty("transaction_type")
        WalletTransactionType transactionType,

        @Schema(
                description = "**[READ-ONLY]** Transaction amount.",
                example = "500.00",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty("amount")
        BigDecimal amount,

        @Schema(
                description = "**[READ-ONLY]** ISO currency code for the transaction.",
                example = "KES",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty("currency_code")
        String currencyCode,

        @Schema(
                description = "**[READ-ONLY]** Wallet balance before the transaction.",
                example = "1000.00",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty("balance_before")
        BigDecimal balanceBefore,

        @Schema(
                description = "**[READ-ONLY]** Wallet balance after the transaction.",
                example = "1500.00",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty("balance_after")
        BigDecimal balanceAfter,

        @Schema(
                description = "**[READ-ONLY]** External reference identifier, if provided.",
                example = "PAY-2025-0001",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty("reference")
        String reference,

        @Schema(
                description = "**[READ-ONLY]** Human-readable description of the transaction.",
                example = "Wallet deposit via M-Pesa",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty("description")
        String description,

        @Schema(
                description = "**[READ-ONLY]** Transfer group UUID when the transaction is part of a wallet transfer.",
                format = "uuid",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty("transfer_reference")
        UUID transferReference,

        @Schema(
                description = "**[READ-ONLY]** Counterparty user UUID when the transaction involves another wallet.",
                format = "uuid",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty("counterparty_user_uuid")
        UUID counterpartyUserUuid,

        @Schema(
                description = "**[READ-ONLY]** Timestamp when the transaction was recorded.",
                format = "date-time",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty("created_date")
        LocalDateTime createdDate
) {
}
