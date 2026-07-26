package apps.sarafrika.elimika.tenancy.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

@Schema(name = "CreateSkillsFundSourceRequest", description = "Payload to add a funding source.")
public record CreateSkillsFundSourceRequestDTO(

        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("name")
        @NotBlank(message = "Source name is required")
        String name,

        @JsonProperty("source_type")
        String sourceType,

        @JsonProperty("amount")
        BigDecimal amount
) {
}
