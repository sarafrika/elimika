package apps.sarafrika.elimika.tenancy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

/**
 * Request payload for setting/replacing an organisation member's org-scoped domain (role).
 *
 * @param domainName the org-scoped domain to assign; must be one of
 *                   {@code organisation_user}, {@code admin}, {@code instructor}, {@code student}
 * @param branchUuid optional training branch to scope the assignment to; may be {@code null}
 */
@Schema(
        name = "SetOrganisationUserDomainRequest",
        description = "Sets/replaces an organisation member's org-scoped domain (role).",
        example = """
                {
                    "domain_name": "admin",
                    "branch_uuid": null
                }
                """
)
public record SetOrganisationUserDomainRequestDTO(

        @Schema(
                description = "**[REQUIRED]** Org-scoped domain to assign to the member. " +
                        "Valid values: 'organisation_user', 'admin', 'instructor', 'student'.",
                example = "admin",
                allowableValues = {"organisation_user", "admin", "instructor", "student"},
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "domain_name is required")
        @JsonProperty("domain_name")
        String domainName,

        @Schema(
                description = "**[OPTIONAL]** Training branch to scope the assignment to. " +
                        "Must belong to the organisation when provided; null for an organisation-wide role.",
                example = "550e8400-e29b-41d4-a716-446655440002",
                format = "uuid",
                nullable = true,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @JsonProperty("branch_uuid")
        UUID branchUuid
) {
}
