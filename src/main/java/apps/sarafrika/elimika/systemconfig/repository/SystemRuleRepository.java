package apps.sarafrika.elimika.systemconfig.repository;

import apps.sarafrika.elimika.systemconfig.enums.RuleCategory;
import apps.sarafrika.elimika.systemconfig.enums.RuleScope;
import apps.sarafrika.elimika.systemconfig.enums.RuleStatus;
import apps.sarafrika.elimika.systemconfig.model.SystemRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SystemRuleRepository extends JpaRepository<SystemRule, Long>, JpaSpecificationExecutor<SystemRule> {
    List<SystemRule> findByCategoryAndStatusOrderByPriorityDescEffectiveFromDesc(RuleCategory category, RuleStatus status);

    List<SystemRule> findByCategoryAndScopeAndStatusOrderByPriorityDescEffectiveFromDesc(
            RuleCategory category,
            RuleScope scope,
            RuleStatus status
    );

    Optional<SystemRule> findByUuid(UUID uuid);
}
