package apps.sarafrika.elimika.systemconfig.dto;

import lombok.Builder;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Set;

@Builder(toBuilder = true)
public record RuleContext(
        String ruleKey,
        String tenantId,
        String regionCode,
        Set<String> demographicTags,
        Set<String> segments,
        OffsetDateTime evaluationInstant
) {

    public OffsetDateTime resolvedEvaluationInstant() {
        return evaluationInstant == null ? OffsetDateTime.now(ZoneOffset.UTC) : evaluationInstant;
    }

    public Set<String> demographicTags() {
        return demographicTags == null ? Collections.emptySet() : demographicTags;
    }

    public Set<String> segments() {
        return segments == null ? Collections.emptySet() : segments;
    }
}
