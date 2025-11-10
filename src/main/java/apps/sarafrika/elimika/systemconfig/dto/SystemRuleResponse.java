package apps.sarafrika.elimika.systemconfig.dto;

import apps.sarafrika.elimika.systemconfig.enums.RuleCategory;
import apps.sarafrika.elimika.systemconfig.enums.RuleScope;
import apps.sarafrika.elimika.systemconfig.enums.RuleStatus;
import apps.sarafrika.elimika.systemconfig.enums.RuleValueType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "SystemRuleResponse")
public record SystemRuleResponse(
        UUID uuid,
        RuleCategory category,
        String key,
        RuleScope scope,
        String scopeReference,
        Integer priority,
        RuleStatus status,
        RuleValueType valueType,
        JsonNode valuePayload,
        JsonNode conditions,
        OffsetDateTime effectiveFrom,
        OffsetDateTime effectiveTo,
        LocalDateTime createdDate,
        String createdBy,
        LocalDateTime updatedDate,
        String updatedBy
) {
}
