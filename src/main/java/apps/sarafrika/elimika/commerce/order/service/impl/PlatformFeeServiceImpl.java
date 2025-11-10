package apps.sarafrika.elimika.commerce.order.service.impl;

import apps.sarafrika.elimika.commerce.medusa.dto.MedusaOrderResponse;
import apps.sarafrika.elimika.commerce.order.service.PlatformFeeService;
import apps.sarafrika.elimika.shared.dto.commerce.CheckoutRequest;
import apps.sarafrika.elimika.shared.dto.commerce.PlatformFeeBreakdown;
import apps.sarafrika.elimika.systemconfig.dto.PlatformFeeConfig;
import apps.sarafrika.elimika.systemconfig.dto.RuleContext;
import apps.sarafrika.elimika.systemconfig.enums.PlatformFeeMode;
import apps.sarafrika.elimika.systemconfig.service.RuleEvaluationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Currency;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlatformFeeServiceImpl implements PlatformFeeService {

    private static final String PLATFORM_FEE_RULE_KEY = "commerce.platform_fee";
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final RuleEvaluationService ruleEvaluationService;

    @Override
    public Optional<PlatformFeeBreakdown> calculateFee(MedusaOrderResponse medusaOrder, CheckoutRequest checkoutContext) {
        if (medusaOrder == null) {
            return Optional.empty();
        }

        RuleContext context = RuleContext.builder()
                .ruleKey(PLATFORM_FEE_RULE_KEY)
                .regionCode(medusaOrder.getRegionId())
                .evaluationInstant(Optional.ofNullable(medusaOrder.getCreatedAt())
                        .orElse(OffsetDateTime.now(ZoneOffset.UTC)))
                .build();

        BigDecimal baseAmount = resolveBaseAmount(medusaOrder);
        if (baseAmount == null) {
            return Optional.empty();
        }

        return ruleEvaluationService.resolvePlatformFeeMatch(context)
                .flatMap(match -> calculateFeeAmount(match.payload(), baseAmount, context.resolvedEvaluationInstant())
                        .map(amount -> new PlatformFeeBreakdown(
                                amount,
                                determineCurrency(medusaOrder, match.payload()),
                                match.payload().mode(),
                                determineRate(match.payload()),
                                baseAmount,
                                match.rule().getUuid(),
                                context.resolvedEvaluationInstant()
                        )));
    }

    private Optional<BigDecimal> calculateFeeAmount(
            PlatformFeeConfig config,
            BigDecimal baseAmount,
            OffsetDateTime evaluationInstant
    ) {
        if (baseAmount == null || config == null || config.mode() == null) {
            return Optional.empty();
        }

        BigDecimal feeAmount = switch (config.mode()) {
            case FLAT -> config.amount();
            case PERCENTAGE -> {
                BigDecimal rate = config.amount();
                if (rate == null) {
                    yield null;
                }
                yield baseAmount.multiply(rate).divide(HUNDRED, 6, RoundingMode.HALF_UP);
            }
        };

        if (feeAmount == null) {
            return Optional.empty();
        }

        feeAmount = applyModifier(feeAmount, config.waiver(), evaluationInstant);
        feeAmount = applyModifier(feeAmount, config.discount(), evaluationInstant);
        return Optional.of(feeAmount.max(BigDecimal.ZERO));
    }

    private BigDecimal applyModifier(
            BigDecimal amount,
            PlatformFeeConfig.TimeBoundModifier modifier,
            OffsetDateTime instant
    ) {
        if (modifier == null || !modifier.isActive(instant)) {
            return amount;
        }
        BigDecimal updated = amount;
        if (modifier.percentage() != null) {
            updated = updated.subtract(updated.multiply(modifier.percentage()).divide(HUNDRED, 6, RoundingMode.HALF_UP));
        }
        if (modifier.amount() != null) {
            updated = updated.subtract(modifier.amount());
        }
        return updated;
    }

    private BigDecimal resolveBaseAmount(MedusaOrderResponse medusaOrder) {
        Long totalMinor = medusaOrder.getTotal();
        if (totalMinor == null) {
            return null;
        }
        int scale = resolveCurrencyScale(medusaOrder.getCurrencyCode());
        return BigDecimal.valueOf(totalMinor, scale);
    }

    private String determineCurrency(MedusaOrderResponse medusaOrder, PlatformFeeConfig config) {
        if (StringUtils.hasText(medusaOrder.getCurrencyCode())) {
            return medusaOrder.getCurrencyCode();
        }
        return config.currency();
    }

    private int resolveCurrencyScale(String currencyCode) {
        if (!StringUtils.hasText(currencyCode)) {
            return 2;
        }
        try {
            return Currency.getInstance(currencyCode).getDefaultFractionDigits();
        } catch (IllegalArgumentException ex) {
            log.warn("Unknown currency code {} when resolving platform fee scale", currencyCode);
            return 2;
        }
    }

    private BigDecimal determineRate(PlatformFeeConfig config) {
        if (config.mode() == PlatformFeeMode.PERCENTAGE) {
            return config.amount();
        }
        return null;
    }
}
