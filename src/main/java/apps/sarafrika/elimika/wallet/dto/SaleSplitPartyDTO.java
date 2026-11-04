package apps.sarafrika.elimika.wallet.dto;

import apps.sarafrika.elimika.shared.spi.revenue.SaleLegStatus;
import apps.sarafrika.elimika.wallet.service.SaleSplitRole;
import apps.sarafrika.elimika.wallet.service.WalletOwnerType;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

@Schema(name = "SaleSplitParty", description = "One party's leg of a sale split; named, never with contact details")
public record SaleSplitPartyDTO(
        @Schema(description = "COURSE_CREATOR, INSTRUCTOR or ORGANISATION; null when no delivery party was known")
        @JsonProperty("role") SaleSplitRole role,
        @Schema(description = "Display name of the person or organisation; null when nobody was resolved")
        @JsonProperty("name") String name,
        @Schema(description = "USER or ORGANISATION; null when nobody was resolved")
        @JsonProperty("owner_type") WalletOwnerType ownerType,
        @Schema(description = "Set for the viewer's own leg and for admins; null for the other party")
        @JsonProperty("owner_uuid") UUID ownerUuid,
        @Schema(description = "PAID, or why the leg was not paid: NO_EARNER, NO_SHARE or NOT_SPLIT")
        @JsonProperty("status") SaleLegStatus status,
        @Schema(description = "This party's share of the line before the platform fee", example = "15000.00")
        @JsonProperty("gross") BigDecimal gross,
        @Schema(description = "Platform fee charged on this share", example = "1500.00")
        @JsonProperty("fee") BigDecimal fee,
        @Schema(description = "What this party received", example = "13500.00")
        @JsonProperty("net") BigDecimal net
) {
}
