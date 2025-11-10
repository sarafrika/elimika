package apps.sarafrika.elimika.systemconfig.service;

import apps.sarafrika.elimika.systemconfig.dto.AgeGateConfig;
import apps.sarafrika.elimika.systemconfig.dto.AgeGateDecision;
import apps.sarafrika.elimika.systemconfig.dto.PlatformFeeConfig;
import apps.sarafrika.elimika.systemconfig.dto.RuleContext;
import apps.sarafrika.elimika.systemconfig.model.SystemRule;
import apps.sarafrika.elimika.systemconfig.enums.RuleCategory;

import java.time.LocalDate;
import java.util.Optional;

public interface RuleEvaluationService {

    Optional<PlatformFeeConfig> resolvePlatformFee(RuleContext context);
    Optional<RuleMatch<PlatformFeeConfig>> resolvePlatformFeeMatch(RuleContext context);

    Optional<AgeGateConfig> resolveAgeGate(RuleContext context);

    AgeGateDecision evaluateAgeGate(LocalDate dateOfBirth, RuleContext context);

    record RuleMatch<T>(SystemRule rule, T payload, RuleCategory category) {}
}
