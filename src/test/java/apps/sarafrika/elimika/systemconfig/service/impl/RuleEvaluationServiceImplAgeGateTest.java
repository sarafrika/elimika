package apps.sarafrika.elimika.systemconfig.service.impl;

import apps.sarafrika.elimika.systemconfig.dto.AgeGateDecision;
import apps.sarafrika.elimika.systemconfig.dto.RuleContext;
import apps.sarafrika.elimika.systemconfig.enums.RuleCategory;
import apps.sarafrika.elimika.systemconfig.enums.RuleScope;
import apps.sarafrika.elimika.systemconfig.enums.RuleStatus;
import apps.sarafrika.elimika.systemconfig.model.SystemRule;
import apps.sarafrika.elimika.systemconfig.repository.SystemRuleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The student onboarding age gate now admits adults: the seed's upper bound was raised from 18 to 120.
 */
class RuleEvaluationServiceImplAgeGateTest {

    private static final String KEY = "student.onboarding.age_gate";
    private static final OffsetDateTime NOW = OffsetDateTime.of(2026, 10, 1, 12, 0, 0, 0, ZoneOffset.UTC);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SystemRuleRepository repository = mock(SystemRuleRepository.class);
    private final RuleEvaluationServiceImpl service = new RuleEvaluationServiceImpl(repository, objectMapper);

    @Test
    void anAdultPassesTheRaisedGate() throws Exception {
        givenGlobalGate("{\"minAge\":5,\"maxAge\":120}");

        AgeGateDecision decision = service.evaluateAgeGate(NOW.toLocalDate().minusYears(34), context());

        assertThat(decision.allowed()).isTrue();
    }

    @Test
    void aChildBelowTheMinimumIsStillRejected() throws Exception {
        givenGlobalGate("{\"minAge\":5,\"maxAge\":120}");

        AgeGateDecision decision = service.evaluateAgeGate(NOW.toLocalDate().minusYears(3), context());

        assertThat(decision.allowed()).isFalse();
    }

    @Test
    void aMinorPassesTheRaisedGate() throws Exception {
        givenGlobalGate("{\"minAge\":5,\"maxAge\":120}");

        AgeGateDecision decision = service.evaluateAgeGate(NOW.toLocalDate().minusYears(12), context());

        assertThat(decision.allowed()).isTrue();
    }

    @Test
    void anAdminCustomisedCapIsStillHonoured() throws Exception {
        givenGlobalGate("{\"minAge\":5,\"maxAge\":30}");

        AgeGateDecision decision = service.evaluateAgeGate(LocalDate.of(1970, 1, 1), context());

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.reason()).contains("30");
    }

    private void givenGlobalGate(String payload) throws Exception {
        SystemRule rule = new SystemRule();
        rule.setCategory(RuleCategory.AGE_GATE);
        rule.setKey(KEY);
        rule.setScope(RuleScope.GLOBAL);
        rule.setStatus(RuleStatus.ACTIVE);
        rule.setPriority(0);
        rule.setEffectiveFrom(NOW.minusYears(1));
        rule.setPayload(objectMapper.readTree(payload));
        when(repository.findByCategoryAndStatusOrderByPriorityDescEffectiveFromDesc(RuleCategory.AGE_GATE, RuleStatus.ACTIVE))
                .thenReturn(List.of(rule));
    }

    private static RuleContext context() {
        return new RuleContext(KEY, null, null, null, null, NOW);
    }
}
