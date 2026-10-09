package apps.sarafrika.elimika.bootstrap.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/** The organisation an organisation user's dashboard opens on: the first active affiliation. */
@Schema(name = "BootstrapActiveOrganisation", description = "Summary of the caller's active organisation")
public record ActiveOrganisationDTO(
        @JsonProperty("organisation_uuid")
        UUID organisationUuid,
        @JsonProperty("organisation_name")
        String organisationName,
        @JsonProperty("domain_in_organisation")
        String domainInOrganisation,
        @JsonProperty("branch_uuid")
        UUID branchUuid,
        @JsonProperty("branch_name")
        String branchName,
        @JsonProperty("active")
        boolean active
) {
}
