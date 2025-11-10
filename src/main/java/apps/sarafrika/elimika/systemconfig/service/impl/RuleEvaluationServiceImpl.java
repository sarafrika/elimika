package apps.sarafrika.elimika.systemconfig.service.impl;

import apps.sarafrika.elimika.systemconfig.dto.AgeGateConfig;
import apps.sarafrika.elimika.systemconfig.dto.AgeGateDecision;
import apps.sarafrika.elimika.systemconfig.dto.PlatformFeeConfig;
import apps.sarafrika.elimika.systemconfig.dto.RuleContext;
import apps.sarafrika.elimika.systemconfig.enums.RuleCategory;
import apps.sarafrika.elimika.systemconfig.enums.RuleScope;
import apps.sarafrika.elimika.systemconfig.enums.RuleStatus;
import apps.sarafrika.elimika.systemconfig.model.SystemRule;
import apps.sarafrika.elimika.systemconfig.repository.SystemRuleRepository;
import apps.sarafrika.elimika.systemconfig.service.RuleEvaluationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Period;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RuleEvaluationServiceImpl implements RuleEvaluationService {

    private final SystemRuleRepository systemRuleRepository;
    private final ObjectMapper objectMapper;

    private static final Comparator<SystemRule> RULE_COMPARATOR = Comparator
            .comparing((Function<SystemRule, Integer>) rule -> Optional.ofNullable(rule.getPriority()).orElse(0))
            .reversed()
            .thenComparing(rule -> scopeWeight(rule.getScope()), Comparator.reverseOrder())
            .thenComparing(SystemRule::getEffectiveFrom, Comparator.nullsLast(Comparator.reverseOrder()));

    @Override
    public Optional<PlatformFeeConfig> resolvePlatformFee(RuleContext context) {
        return resolvePlatformFeeMatch(context).map(RuleEvaluationService.RuleMatch::payload);
    }

    @Override
    public Optional<RuleEvaluationService.RuleMatch<PlatformFeeConfig>> resolvePlatformFeeMatch(RuleContext context) {
        return resolveRuleMatch(RuleCategory.PLATFORM_FEE, context, PlatformFeeConfig.class);
    }

    @Override
    public Optional<AgeGateConfig> resolveAgeGate(RuleContext context) {
        return resolveRuleMatch(RuleCategory.AGE_GATE, context, AgeGateConfig.class)
                .map(RuleEvaluationService.RuleMatch::payload);
    }

    @Override
    public AgeGateDecision evaluateAgeGate(LocalDate dateOfBirth, RuleContext context) {
        Optional<AgeGateConfig> configuration = resolveAgeGate(context);
        if (configuration.isEmpty()) {
            return AgeGateDecision.allow();
        }
        if (dateOfBirth == null) {
            return AgeGateDecision.rejected("Date of birth is required for onboarding");
        }

        AgeGateConfig config = configuration.get();
        OffsetDateTime evaluationTime = context.resolvedEvaluationInstant();
        int age = Period.between(dateOfBirth, evaluationTime.toLocalDate()).getYears();

        if (config.minAge() != null && age < config.minAge()) {
            return AgeGateDecision.rejected("Student below minimum age of " + config.minAge());
        }

        if (config.maxAge() != null && age > config.maxAge()) {
            return AgeGateDecision.rejected("Student above maximum age of " + config.maxAge());
        }

        String region = normalizeValue(context.regionCode());
        if (!config.allowedRegions().isEmpty()) {
            if (!StringUtils.hasText(region) || !normalizeSet(config.allowedRegions()).contains(region)) {
                return AgeGateDecision.rejected("Region not permitted for onboarding");
            }
        }
        if (StringUtils.hasText(region) && normalizeSet(config.blockedRegions()).contains(region)) {
            return AgeGateDecision.rejected("Region currently blocked for onboarding");
        }

        Set<String> demographicTags = normalizeSet(context.demographicTags());
        if (!config.allowedDemographics().isEmpty()) {
            Set<String> allowedDemographics = normalizeSet(config.allowedDemographics());
            boolean matches = demographicTags.stream().anyMatch(allowedDemographics::contains);
            if (!matches) {
                return AgeGateDecision.rejected("Student demographic not permitted for onboarding");
            }
        }
        Set<String> blockedDemographics = normalizeSet(config.blockedDemographics());
        boolean blocked = demographicTags.stream().anyMatch(blockedDemographics::contains);
        if (blocked) {
            return AgeGateDecision.rejected("Student demographic currently blocked for onboarding");
        }

        return AgeGateDecision.allow();
    }

    private <T> Optional<RuleEvaluationService.RuleMatch<T>> resolveRuleMatch(
            RuleCategory category,
            RuleContext context,
            Class<T> targetClass
    ) {
        return resolveRuleWithFallback(category, context)
                .flatMap(rule -> convertPayload(rule.getPayload(), targetClass)
                        .map(payload -> new RuleEvaluationService.RuleMatch<>(rule, payload, category)));
    }

    private Optional<SystemRule> resolveRuleWithFallback(RuleCategory category, RuleContext context) {
        Optional<SystemRule> direct = resolveRule(category, context);
        if (direct.isPresent() || !StringUtils.hasText(context.ruleKey())) {
            return direct;
        }
        return resolveRule(category, context.toBuilder().ruleKey(null).build());
    }

    private Optional<SystemRule> resolveRule(RuleCategory category, RuleContext context) {
        OffsetDateTime evaluationTime = context.resolvedEvaluationInstant();
        return systemRuleRepository.findByCategoryAndStatusOrderByPriorityDescEffectiveFromDesc(category, RuleStatus.ACTIVE)
                .stream()
                .filter(rule -> matchesKey(rule, context.ruleKey()))
                .filter(rule -> matchesScope(rule, context))
                .filter(rule -> isActive(rule, evaluationTime))
                .sorted(RULE_COMPARATOR)
                .findFirst();
    }

    private boolean matchesKey(SystemRule rule, String key) {
        if (!StringUtils.hasText(key)) {
            return true;
        }
        return key.equalsIgnoreCase(rule.getKey());
    }

    private boolean matchesScope(SystemRule rule, RuleContext context) {
        RuleScope scope = Optional.ofNullable(rule.getScope()).orElse(RuleScope.GLOBAL);
        String reference = normalizeValue(rule.getScopeReference());
        return switch (scope) {
            case GLOBAL -> true;
            case TENANT -> StringUtils.hasText(reference) && reference.equalsIgnoreCase(context.tenantId());
            case REGION -> StringUtils.hasText(reference) && reference.equalsIgnoreCase(context.regionCode());
            case DEMOGRAPHIC -> StringUtils.hasText(reference) && normalizeSet(context.demographicTags()).contains(reference);
            case SEGMENT -> StringUtils.hasText(reference) && normalizeSet(context.segments()).contains(reference);
        };
    }

    private boolean isActive(SystemRule rule, OffsetDateTime evaluationTime) {
        boolean afterStart = rule.getEffectiveFrom() == null || !evaluationTime.isBefore(rule.getEffectiveFrom());
        boolean beforeEnd = rule.getEffectiveTo() == null || evaluationTime.isBefore(rule.getEffectiveTo()) || evaluationTime.isEqual(rule.getEffectiveTo());
        return afterStart && beforeEnd;
    }

    private <T> Optional<T> convertPayload(JsonNode payload, Class<T> targetClass) {
        if (payload == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.treeToValue(payload, targetClass));
        } catch (JsonProcessingException ex) {
            log.error("Failed to convert system rule payload into {}", targetClass.getSimpleName(), ex);
            return Optional.empty();
        }
    }

    private static int scopeWeight(RuleScope scope) {
        RuleScope safeScope = scope == null ? RuleScope.GLOBAL : scope;
        return switch (safeScope) {
            case GLOBAL -> 0;
            case REGION -> 1;
            case DEMOGRAPHIC -> 2;
            case SEGMENT -> 3;
            case TENANT -> 4;
        };
    }

    private Set<String> normalizeSet(Set<String> input) {
        if (input == null || input.isEmpty()) {
            return Set.of();
        }
        return input.stream()
                .filter(StringUtils::hasText)
                .map(this::normalizeValue)
                .collect(Collectors.toUnmodifiableSet());
    }

    private String normalizeValue(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }
}
