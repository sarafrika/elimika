package apps.sarafrika.elimika.wallet.dto;

import apps.sarafrika.elimika.shared.sms.Msisdn;
import apps.sarafrika.elimika.wallet.entity.PayoutMethod;
import apps.sarafrika.elimika.wallet.enums.PayoutMethodStatus;
import apps.sarafrika.elimika.wallet.enums.PayoutMethodType;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Schema(name = "PayoutMethod", description = "A number a wallet's withdrawals can be sent to")
public record PayoutMethodDTO(
        @Schema(description = "**[READ-ONLY]** Payout method identifier.", format = "uuid", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty("uuid") UUID uuid,
        @Schema(description = "**[READ-ONLY]** Payout rail.", example = "MPESA", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty("method_type") PayoutMethodType methodType,
        @Schema(description = "**[READ-ONLY]** The number with most digits hidden; the full number never leaves the server.",
                example = "+254 7•• ••• 678",
                accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty("masked_msisdn") String maskedMsisdn,
        @Schema(description = "**[READ-ONLY]** Owner's nickname for it.", example = "Personal", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty("label") String label,
        @Schema(description = "**[READ-ONLY]** Where it is in verification.", example = "VERIFIED", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty("status") PayoutMethodStatus status,
        @Schema(description = "**[READ-ONLY]** Withdrawals go here unless another is picked.", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty("is_default") boolean isDefault,
        @Schema(description = "**[READ-ONLY]** When its code was confirmed.", format = "date-time", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty("verified_at") OffsetDateTime verifiedAt,
        @Schema(description = "**[READ-ONLY]** When it can first receive money.", format = "date-time", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty("unlock_at") OffsetDateTime unlockAt,
        @Schema(description = "**[READ-ONLY]** Verified but still inside the hold after adding it.", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty("on_hold") boolean onHold,
        @Schema(description = "**[READ-ONLY]** Name M-Pesa returned on the first payout; null until then.",
                accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty("mpesa_registered_name") String mpesaRegisteredName
) {

    public static PayoutMethodDTO from(PayoutMethod method) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        boolean onHold = method.getStatus() == PayoutMethodStatus.VERIFIED && !method.isUsable(now);
        return new PayoutMethodDTO(method.getUuid(), method.getMethodType(), Msisdn.mask(method.getMsisdn()), method.getLabel(), method.getStatus(), method.isDefaultMethod(),
                method.getVerifiedAt(), method.getUnlockAt(), onHold, method.getMpesaRegisteredName());
    }
}
