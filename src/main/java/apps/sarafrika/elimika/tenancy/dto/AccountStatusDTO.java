package apps.sarafrika.elimika.tenancy.dto;

import apps.sarafrika.elimika.tenancy.util.enums.AccountState;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(name = "AccountStatus", description = "Whether the caller may use a dashboard yet, and the state of each domain they asked for")
public record AccountStatusDTO(

        @JsonProperty("user_uuid")
        UUID userUuid,

        @Schema(description = "ACTIVE once any domain is approved; PENDING_APPROVAL while every requested domain awaits review")
        @JsonProperty("account_state")
        AccountState accountState,

        @Schema(description = "Domains the caller may act in now", example = "[\"student\"]")
        @JsonProperty("approved_domains")
        List<String> approvedDomains,

        @JsonProperty("domain_applications")
        List<DomainApplicationDTO> domainApplications
) {
}
