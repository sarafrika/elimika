package apps.sarafrika.elimika.wallet.dto;

import apps.sarafrika.elimika.wallet.enums.WalletTransactionType;
import apps.sarafrika.elimika.wallet.service.SaleSplitRole;
import apps.sarafrika.elimika.wallet.service.WalletPurse;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

@Schema(name = "SaleSplitTransaction", description = "A wallet SALE or PLATFORM_FEE row written for a sale split")
public record SaleSplitTransactionDTO(
        @JsonProperty("uuid") UUID uuid,
        @JsonProperty("wallet_uuid") UUID walletUuid,
        @JsonProperty("transaction_type") WalletTransactionType transactionType,
        @Schema(description = "The leg the row pays: COURSE_CREATOR, INSTRUCTOR or ORGANISATION")
        @JsonProperty("role") SaleSplitRole role,
        @Schema(description = "The purse the row moved")
        @JsonProperty("purse") WalletPurse purse,
        @JsonProperty("reference") String reference,
        @JsonProperty("amount") BigDecimal amount
) {
}
