package apps.sarafrika.elimika.bootstrap.dto;

import apps.sarafrika.elimika.notifications.spi.UnreadNotificationSummary;
import apps.sarafrika.elimika.tenancy.dto.UserDTO;
import apps.sarafrika.elimika.wallet.service.WalletBalanceSummary;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/** Everything the dashboard shell needs on first paint; optional sections are null when unavailable. */
@Schema(name = "SessionBootstrap", description = "Composite session bootstrap for the authenticated caller")
public record SessionBootstrapDTO(
        @JsonProperty("user")
        UserDTO user,
        @JsonProperty("profiles")
        RoleProfilesDTO profiles,
        @Schema(nullable = true, description = "Present only for organisation users with an affiliation")
        @JsonProperty("active_organisation")
        ActiveOrganisationDTO activeOrganisation,
        @Schema(nullable = true, description = "Null when the wallet could not be read")
        @JsonProperty("wallet")
        WalletBalanceSummary wallet,
        @Schema(nullable = true, description = "Null when notification counts could not be read")
        @JsonProperty("notifications")
        UnreadNotificationSummary notifications
) {
}
