package apps.sarafrika.elimika.commerce.internal.service.impl;

import apps.sarafrika.elimika.shared.dto.commerce.PlatformFeeBreakdown;
import apps.sarafrika.elimika.systemconfig.dto.PlatformFeeConfig;
import apps.sarafrika.elimika.systemconfig.dto.RuleContext;
import apps.sarafrika.elimika.systemconfig.enums.PlatformFeeMode;
import apps.sarafrika.elimika.systemconfig.service.RuleEvaluationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

/**
 * Computes the platform fee for an order from the configured {@code PLATFORM_FEE} system rule.
 * Returns {@code null} when no rule is configured (no fee applied).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PlatformFeeCalculator {

    private final RuleEvaluationService ruleEvaluationService;

    public PlatformFeeBreakdown compute(BigDecimal orderTotal, String currencyCode) {
        if (orderTotal == null) {
            return null;
        }
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Optional<RuleEvaluationService.RuleMatch<PlatformFeeConfig>> match =
                ruleEvaluationService.resolvePlatformFeeMatch(RuleContext.builder().evaluationInstant(now).build());
        if (match.isEmpty() || match.get().payload() == null) {
            return null;
        }

        PlatformFeeConfig config = match.get().payload();
        UUID ruleUuid = match.get().rule() != null ? match.get().rule().getUuid() : null;
        String currency = config.currency() != null ? config.currency() : currencyCode;

        if (config.waiverActive(now)) {
            return new PlatformFeeBreakdown(BigDecimal.ZERO, currency, config.mode(), null, orderTotal, ruleUuid, now);
        }

        BigDecimal rate = config.mode() == PlatformFeeMode.PERCENTAGE
                ? (config.amount() == null ? BigDecimal.ZERO : config.amount())
                : null;
        BigDecimal fee = config.feeOn(orderTotal, now);

        return new PlatformFeeBreakdown(fee, currency, config.mode(), rate, orderTotal, ruleUuid, now);
    }
}
