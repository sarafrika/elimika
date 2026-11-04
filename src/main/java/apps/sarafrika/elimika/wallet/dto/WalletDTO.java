package apps.sarafrika.elimika.wallet.dto;

import apps.sarafrika.elimika.wallet.service.WalletOwnerType;
import apps.sarafrika.elimika.wallet.service.WalletPurse;
import apps.sarafrika.elimika.wallet.service.WalletPurseSummary;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(
        name = "Wallet",
        description = "User or organisation wallet summary for a specific currency"
)
public record WalletDTO(

        @Schema(
                description = "**[READ-ONLY]** Unique identifier for the wallet row. On the owner's wallet, which sums "
                        + "every purse, the owner's oldest purse row (the row the wallet had before purses); each "
                        + "purse's own row is purses[].wallet_uuid. Null while the owner has no row.",
                format = "uuid",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty("uuid")
        UUID uuid,

        @Schema(
                description = "**[READ-ONLY]** User UUID that owns this wallet; null for an organisation wallet.",
                format = "uuid",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty("user_uuid")
        UUID userUuid,

        @Schema(
                description = "**[READ-ONLY]** Who owns this wallet.",
                example = "USER",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty("owner_type")
        WalletOwnerType ownerType,

        @Schema(
                description = "**[READ-ONLY]** UUID of the owning user or organisation.",
                format = "uuid",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty("owner_uuid")
        UUID ownerUuid,

        @Schema(
                description = "**[READ-ONLY]** ISO currency code for this wallet balance.",
                example = "KES",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        @JsonProperty("currency_code")
        String currencyCode,

        @Schema(
                description = "**[READ-ONLY]** Current balance: the purse's, or the sum of every purse.",
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
        LocalDateTime updatedDate,

        @Schema(description = "**[READ-ONLY]** Lifetime sale shares credited, net of reversals. Null where not computed.",
                accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty("shares_total")
        BigDecimal sharesTotal,

        @Schema(description = "**[READ-ONLY]** Lifetime platform fees charged on those shares.",
                accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty("platform_fees_total")
        BigDecimal platformFeesTotal,

        @Schema(description = "**[READ-ONLY]** Number of class sales credited, net of reversals.",
                accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty("sales_count")
        Long salesCount,

        @Schema(description = "**[READ-ONLY]** Lifetime money transferred out to other users.",
                accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty("transferred_out_total")
        BigDecimal transferredOutTotal,

        @Schema(description = "**[READ-ONLY]** Money held for withdrawals still being paid out; not part of the balance.",
                accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty("held_amount")
        BigDecimal heldAmount,

        @Schema(description = "**[READ-ONLY]** What can be withdrawn or spent now.",
                accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty("available_amount")
        BigDecimal availableAmount,

        @Schema(description = "**[READ-ONLY]** Whether the caller may withdraw from this wallet.",
                accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty("can_withdraw")
        Boolean canWithdraw,

        @Schema(description = "**[READ-ONLY]** The purse this row is; null when the wallet sums every purse.",
                accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty("purse")
        WalletPurse purse,

        @Schema(description = "**[READ-ONLY]** available_amount plus held_amount. Null where not computed.",
                accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty("account_balance")
        BigDecimal accountBalance,

        @Schema(description = "**[READ-ONLY]** Each purse with its own balance, in the order COURSE_CREATOR, "
                + "INSTRUCTOR, STUDENT (EARNINGS for an organisation). The top-level figures are their sums. Null "
                + "where not computed.", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty("purses")
        List<WalletPurseSummary> purses
) {
}
