package apps.sarafrika.elimika.tenancy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(name = "SkillsFundSource", description = "A funding source for an organisation's skills fund.")
public record SkillsFundSourceDTO(

        @Schema(accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "uuid", access = JsonProperty.Access.READ_ONLY)
        UUID uuid,

        @JsonProperty("organisation_uuid")
        UUID organisationUuid,

        @Schema(description = "Source name.")
        @JsonProperty("name")
        String name,

        @Schema(description = "Source type (e.g. Government Grant, Sponsor/Donor).", nullable = true)
        @JsonProperty("source_type")
        String sourceType,

        @Schema(description = "Amount contributed.")
        @JsonProperty("amount")
        BigDecimal amount,

        @Schema(accessMode = Schema.AccessMode.READ_ONLY)
        @JsonProperty(value = "created_date", access = JsonProperty.Access.READ_ONLY)
        LocalDateTime createdDate
) {
}
