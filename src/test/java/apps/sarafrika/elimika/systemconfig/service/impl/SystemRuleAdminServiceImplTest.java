package apps.sarafrika.elimika.systemconfig.service.impl;

import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.systemconfig.dto.SystemRuleRequest;
import apps.sarafrika.elimika.systemconfig.dto.SystemRuleResponse;
import apps.sarafrika.elimika.systemconfig.enums.RuleCategory;
import apps.sarafrika.elimika.systemconfig.enums.RuleScope;
import apps.sarafrika.elimika.systemconfig.enums.RuleStatus;
import apps.sarafrika.elimika.systemconfig.model.SystemRule;
import apps.sarafrika.elimika.systemconfig.repository.SystemRuleRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemRuleAdminServiceImplTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mock
    private SystemRuleRepository systemRuleRepository;

    @InjectMocks
    private SystemRuleAdminServiceImpl systemRuleAdminService;

    private JsonNode payload;

    @BeforeEach
    void setUp() throws Exception {
        payload = OBJECT_MAPPER.readTree("""
                {"minAge":5,"maxAge":18}
                """);
    }

    @Test
    void createRulePersistsWithDefaults() {
        SystemRuleRequest request = new SystemRuleRequest(
                RuleCategory.AGE_GATE,
                "student.onboarding.age_gate",
                null,
                null,
                null,
                RuleStatus.ACTIVE,
                null,
                payload,
                null,
                null,
                null
        );

        when(systemRuleRepository.save(any(SystemRule.class))).thenAnswer(invocation -> {
            SystemRule entity = invocation.getArgument(0);
            entity.setUuid(UUID.randomUUID());
            entity.setCreatedDate(LocalDateTime.now());
            entity.setCreatedBy("tester");
            entity.setLastModifiedDate(LocalDateTime.now());
            entity.setLastModifiedBy("tester");
            return entity;
        });

        SystemRuleResponse response = systemRuleAdminService.createRule(request);

        assertThat(response.uuid()).isNotNull();
        assertThat(response.scope()).isEqualTo(RuleScope.GLOBAL);
        assertThat(response.status()).isEqualTo(RuleStatus.ACTIVE);
        assertThat(response.valuePayload()).isEqualTo(payload);
    }

    @Test
    void listRulesAppliesFilters() {
        SystemRule rule = new SystemRule();
        rule.setUuid(UUID.randomUUID());
        rule.setCategory(RuleCategory.AGE_GATE);
        rule.setKey("student.onboarding.age_gate");
        rule.setScope(RuleScope.GLOBAL);
        rule.setStatus(RuleStatus.ACTIVE);
        rule.setPayload(payload);
        rule.setEffectiveFrom(OffsetDateTime.now(ZoneOffset.UTC));
        rule.setCreatedDate(LocalDateTime.now());
        rule.setCreatedBy("system");
        Page<SystemRule> page = new PageImpl<>(List.of(rule));

        when(systemRuleRepository.findAll(ArgumentMatchers.<Specification<SystemRule>>any(), eq(Pageable.unpaged()))).thenReturn(page);

        Page<SystemRuleResponse> response = systemRuleAdminService.listRules(
                RuleCategory.AGE_GATE,
                RuleStatus.ACTIVE,
                Pageable.unpaged()
        );

        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getContent().getFirst().key()).isEqualTo("student.onboarding.age_gate");
    }

    @Test
    void updateRuleThrowsWhenMissing() {
        UUID uuid = UUID.randomUUID();
        when(systemRuleRepository.findByUuid(uuid)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> systemRuleAdminService.updateRule(uuid, new SystemRuleRequest(
                RuleCategory.AGE_GATE, "student.onboarding.age_gate", null,
                null, null, null, null, payload, null, null, null
        ))).isInstanceOf(ResourceNotFoundException.class);
    }
}
