package apps.sarafrika.elimika.systemconfig.dto;

import apps.sarafrika.elimika.systemconfig.enums.RuleCategory;
import apps.sarafrika.elimika.systemconfig.enums.RuleScope;
import apps.sarafrika.elimika.systemconfig.enums.RuleStatus;
import apps.sarafrika.elimika.systemconfig.enums.RuleValueType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "SystemRuleRequest")
public record SystemRuleRequest(
        @NotNull
        @Schema(description = "Rule category grouping", requiredMode = Schema.RequiredMode.REQUIRED)
        RuleCategory category,

        @NotBlank
        @Size(max = 128)
        @Schema(description = "Unique key within the category", example = "student.onboarding.age_gate", requiredMode = Schema.RequiredMode.REQUIRED)
        String key,

        @Schema(description = "Scope the rule applies to", defaultValue = "GLOBAL")
        RuleScope scope,

        @Size(max = 128)
        @Schema(description = "Optional reference identifier for the scope (tenant UUID, country code, etc.)")
        String scopeReference,

        @Schema(description = "Priority used when multiple rules match", defaultValue = "0")
        Integer priority,

        @Schema(description = "Current lifecycle status", defaultValue = "DRAFT")
        RuleStatus status,

        @Schema(description = "Payload interpretation hint", defaultValue = "JSON")
        RuleValueType valueType,

        @NotNull
        @Schema(description = "Rule payload describing configuration", requiredMode = Schema.RequiredMode.REQUIRED)
        JsonNode valuePayload,

        @Schema(description = "Optional JSON condition block")
        JsonNode conditions,

        @Schema(description = "Start of the effective window. Defaults to now if omitted.")
        OffsetDateTime effectiveFrom,

        @Schema(description = "Optional end of the effective window")
        OffsetDateTime effectiveTo
) {
}
