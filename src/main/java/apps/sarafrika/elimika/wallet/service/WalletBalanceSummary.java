package apps.sarafrika.elimika.wallet.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

/** A wallet's balance in one currency; {@code wallet_uuid} is null while the user has no wallet yet. */
@Schema(name = "WalletBalanceSummary", description = "Read-only wallet balance for one currency")
public record WalletBalanceSummary(
        @JsonProperty("wallet_uuid")
        UUID walletUuid,
        @JsonProperty("currency_code")
        String currencyCode,
        @JsonProperty("balance_amount")
        BigDecimal balanceAmount
) {
}
