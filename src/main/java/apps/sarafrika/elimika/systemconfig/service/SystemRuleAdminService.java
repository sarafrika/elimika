package apps.sarafrika.elimika.systemconfig.service;

import apps.sarafrika.elimika.systemconfig.dto.SystemRuleRequest;
import apps.sarafrika.elimika.systemconfig.dto.SystemRuleResponse;
import apps.sarafrika.elimika.systemconfig.enums.RuleCategory;
import apps.sarafrika.elimika.systemconfig.enums.RuleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface SystemRuleAdminService {
    Page<SystemRuleResponse> listRules(RuleCategory category, RuleStatus status, Pageable pageable);

    SystemRuleResponse getRule(UUID uuid);

    SystemRuleResponse createRule(SystemRuleRequest request);

    SystemRuleResponse updateRule(UUID uuid, SystemRuleRequest request);
}
