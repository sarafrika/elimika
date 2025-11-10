package apps.sarafrika.elimika.commerce.order.service.impl;

import apps.sarafrika.elimika.commerce.medusa.dto.MedusaOrderResponse;
import apps.sarafrika.elimika.shared.dto.commerce.PlatformFeeBreakdown;
import apps.sarafrika.elimika.systemconfig.dto.PlatformFeeConfig;
import apps.sarafrika.elimika.systemconfig.enums.PlatformFeeMode;
import apps.sarafrika.elimika.systemconfig.enums.RuleCategory;
import apps.sarafrika.elimika.systemconfig.model.SystemRule;
import apps.sarafrika.elimika.systemconfig.service.RuleEvaluationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlatformFeeServiceImplTest {

    @Mock
    private RuleEvaluationService ruleEvaluationService;

    @InjectMocks
    private PlatformFeeServiceImpl platformFeeService;

    private MedusaOrderResponse medusaOrder;
    private OffsetDateTime createdAt;

    @BeforeEach
    void setUp() {
        createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        medusaOrder = new MedusaOrderResponse();
        medusaOrder.setTotal(12500L);
        medusaOrder.setCurrencyCode("USD");
        medusaOrder.setRegionId("region_123");
        medusaOrder.setCreatedAt(createdAt);
    }

    @Test
    void shouldCalculateFlatPlatformFee() {
        PlatformFeeConfig config = new PlatformFeeConfig(
                PlatformFeeMode.FLAT,
                BigDecimal.valueOf(15),
                "USD",
                null,
                null
        );
        when(ruleEvaluationService.resolvePlatformFeeMatch(any()))
                .thenReturn(Optional.of(buildMatch(config)));

        Optional<PlatformFeeBreakdown> breakdown = platformFeeService.calculateFee(medusaOrder, null);

        assertThat(breakdown).isPresent();
        assertThat(breakdown.get().amount()).isEqualByComparingTo("15");
        assertThat(breakdown.get().currency()).isEqualTo("USD");
    }

    @Test
    void shouldApplyPercentageAndWaiverModifiers() {
        PlatformFeeConfig.TimeBoundModifier waiver = new PlatformFeeConfig.TimeBoundModifier(
                BigDecimal.valueOf(5),
                null,
                createdAt.minusDays(1),
                createdAt.plusDays(1)
        );
        PlatformFeeConfig config = new PlatformFeeConfig(
                PlatformFeeMode.PERCENTAGE,
                BigDecimal.valueOf(2.5),
                "USD",
                waiver,
                null
        );
        when(ruleEvaluationService.resolvePlatformFeeMatch(any()))
                .thenReturn(Optional.of(buildMatch(config)));

        Optional<PlatformFeeBreakdown> breakdown = platformFeeService.calculateFee(medusaOrder, null);

        assertThat(breakdown).isPresent();
        // Order total 125.00, 2.5% = 3.125, minus 5 waiver floors at 0
        assertThat(breakdown.get().amount()).isEqualByComparingTo("0");
        assertThat(breakdown.get().ruleUuid()).isNotNull();
    }

    private RuleEvaluationService.RuleMatch<PlatformFeeConfig> buildMatch(PlatformFeeConfig config) {
        SystemRule rule = new SystemRule();
        rule.setUuid(UUID.randomUUID());
        return new RuleEvaluationService.RuleMatch<>(rule, config, RuleCategory.PLATFORM_FEE);
    }
}
