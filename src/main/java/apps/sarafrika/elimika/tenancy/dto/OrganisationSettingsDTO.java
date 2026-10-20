package apps.sarafrika.elimika.tenancy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(name = "OrganisationSettings", description = "Settings an organisation chooses for itself")
public record OrganisationSettingsDTO(
        @JsonProperty(value = "organisation_uuid", access = JsonProperty.Access.READ_ONLY) UUID organisationUuid,
        @Schema(description = "Hours an instructor may still edit a submitted session report before it locks", example = "48")
        @JsonProperty("session_report_grace_hours") @NotNull @Min(1) @Max(720) Integer sessionReportGraceHours
) {
}
