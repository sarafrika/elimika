package apps.sarafrika.elimika.wallet.dto;

import apps.sarafrika.elimika.wallet.service.WalletPurse;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

@Schema(name = "PurseTransferRequest", description = "Move money between two of one person's own purses; no fee")
public record PurseTransferRequestDTO(
        @Schema(description = "COURSE_CREATOR or INSTRUCTOR; STUDENT is spend-only and refused with 400",
                example = "COURSE_CREATOR", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Choose the purse to move from")
        @JsonProperty("from_purse") WalletPurse fromPurse,
        @Schema(example = "INSTRUCTOR", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Choose the purse to move to")
        @JsonProperty("to_purse") WalletPurse toPurse,
        @Schema(example = "500.00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Amount is required")
        @Positive(message = "Amount must be greater than zero")
        @JsonProperty("amount") BigDecimal amount,
        @Schema(description = "ISO currency code; the platform currency when left out", example = "KES")
        @Size(max = 3, message = "Currency code must not exceed 3 characters")
        @JsonProperty("currency_code") String currencyCode,
        @Schema(description = "Your reference for the move; one is made up when left out")
        @Size(max = 128, message = "Reference must not exceed 128 characters")
        @JsonProperty("reference") String reference,
        @Schema(description = "Shown on both purse statements; says which purses when left out")
        @Size(max = 500, message = "Description must not exceed 500 characters")
        @JsonProperty("description") String description
) {
}
