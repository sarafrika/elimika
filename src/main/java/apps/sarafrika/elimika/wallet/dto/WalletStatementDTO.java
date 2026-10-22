package apps.sarafrika.elimika.wallet.dto;

import apps.sarafrika.elimika.wallet.service.WalletMonthStatement;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

@Schema(name = "WalletStatement", description = "One calendar month (UTC) of a wallet's activity")
public record WalletStatementDTO(
        @Schema(description = "**[READ-ONLY]** The month, as YYYY-MM.", example = "2026-10",
                accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty("month")
        String month,

        @Schema(description = "**[READ-ONLY]** Currency of every amount here.", example = "KES",
                accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty("currency_code")
        String currencyCode,

        @Schema(description = "**[READ-ONLY]** Balance when the month began.", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty("opening_balance")
        BigDecimal openingBalance,

        @Schema(description = "**[READ-ONLY]** Everything that raised the balance.", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty("money_in")
        BigDecimal moneyIn,

        @Schema(description = "**[READ-ONLY]** Everything that lowered the balance.", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty("money_out")
        BigDecimal moneyOut,

        @Schema(description = "**[READ-ONLY]** Balance when the month ended, or now for the current month.",
                accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty("closing_balance")
        BigDecimal closingBalance,

        @Schema(description = "**[READ-ONLY]** Number of transactions in the month.", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty("transaction_count")
        long transactionCount
) {

    public static WalletStatementDTO from(WalletMonthStatement statement, String currencyCode) {
        return new WalletStatementDTO(statement.month().toString(), currencyCode, statement.openingBalance(),
                statement.moneyIn(), statement.moneyOut(), statement.closingBalance(), statement.transactionCount());
    }
}
