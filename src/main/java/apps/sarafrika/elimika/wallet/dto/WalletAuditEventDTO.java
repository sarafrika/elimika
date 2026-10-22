package apps.sarafrika.elimika.wallet.dto;

import apps.sarafrika.elimika.wallet.entity.WalletAuditEvent;
import apps.sarafrika.elimika.wallet.enums.WalletAuditEventType;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Schema(name = "WalletAuditEvent", description = "One append-only entry in a wallet's audit trail")
public record WalletAuditEventDTO(
        @Schema(description = "**[READ-ONLY]** Event identifier.", format = "uuid", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty("uuid")
        UUID uuid,

        @Schema(description = "**[READ-ONLY]** What happened.", example = "SALE_SHARE_CREDITED",
                accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty("event_type")
        WalletAuditEventType eventType,

        @Schema(description = "**[READ-ONLY]** SYSTEM for automatic changes, USER when a person made it.",
                example = "SYSTEM", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty("actor_kind")
        String actorKind,

        @Schema(description = "**[READ-ONLY]** The person who made the change; null for the system.",
                format = "uuid", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty("actor_user_uuid")
        UUID actorUserUuid,

        @Schema(description = "**[READ-ONLY]** Currency the event concerns.", example = "KES",
                accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty("currency_code")
        String currencyCode,

        @Schema(description = "**[READ-ONLY]** Reference of the underlying record.", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty("reference")
        String reference,

        @Schema(description = "**[READ-ONLY]** Why it happened, in words.", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty("reason")
        String reason,

        @Schema(description = "**[READ-ONLY]** Values before the change.", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty("previous_values")
        Map<String, Object> previousValues,

        @Schema(description = "**[READ-ONLY]** Values after the change.", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty("updated_values")
        Map<String, Object> updatedValues,

        @Schema(description = "**[READ-ONLY]** When it happened.", format = "date-time", accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty("created_date")
        LocalDateTime createdDate
) {

    public static WalletAuditEventDTO from(WalletAuditEvent event) {
        return new WalletAuditEventDTO(event.getUuid(), event.getEventType(),
                event.getActorUserUuid() == null ? "SYSTEM" : "USER", event.getActorUserUuid(),
                event.getCurrencyCode(), event.getReference(), event.getReason(), event.getPreviousValues(),
                event.getUpdatedValues(), event.getCreatedDate());
    }
}
