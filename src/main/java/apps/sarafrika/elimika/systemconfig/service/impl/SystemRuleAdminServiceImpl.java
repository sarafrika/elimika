package apps.sarafrika.elimika.systemconfig.service.impl;

import apps.sarafrika.elimika.shared.exceptions.ResourceNotFoundException;
import apps.sarafrika.elimika.systemconfig.dto.SystemRuleRequest;
import apps.sarafrika.elimika.systemconfig.dto.SystemRuleResponse;
import apps.sarafrika.elimika.systemconfig.enums.RuleCategory;
import apps.sarafrika.elimika.systemconfig.enums.RuleScope;
import apps.sarafrika.elimika.systemconfig.enums.RuleStatus;
import apps.sarafrika.elimika.systemconfig.enums.RuleValueType;
import apps.sarafrika.elimika.systemconfig.model.SystemRule;
import apps.sarafrika.elimika.systemconfig.repository.SystemRuleRepository;
import apps.sarafrika.elimika.systemconfig.service.SystemRuleAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SystemRuleAdminServiceImpl implements SystemRuleAdminService {

    private final SystemRuleRepository systemRuleRepository;

    @Override
    public Page<SystemRuleResponse> listRules(RuleCategory category, RuleStatus status, Pageable pageable) {
        Specification<SystemRule> spec = null;
        if (category != null) {
            spec = byCategory(category);
        }
        if (status != null) {
            Specification<SystemRule> statusSpec = byStatus(status);
            spec = spec == null ? statusSpec : spec.and(statusSpec);
        }
        Page<SystemRule> page = spec == null
                ? systemRuleRepository.findAll(pageable)
                : systemRuleRepository.findAll(spec, pageable);
        return page.map(this::toResponse);
    }

    @Override
    public SystemRuleResponse getRule(UUID uuid) {
        return systemRuleRepository.findByUuid(uuid)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("System rule %s not found".formatted(uuid)));
    }

    private Specification<SystemRule> byCategory(RuleCategory category) {
        return (root, query, cb) -> cb.equal(root.get("category"), category);
    }

    private Specification<SystemRule> byStatus(RuleStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    @Override
    public SystemRuleResponse createRule(SystemRuleRequest request) {
        SystemRule rule = new SystemRule();
        applyRequest(rule, request);
        SystemRule saved = systemRuleRepository.save(rule);
        return toResponse(saved);
    }

    @Override
    public SystemRuleResponse updateRule(UUID uuid, SystemRuleRequest request) {
        SystemRule existing = systemRuleRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("System rule %s not found".formatted(uuid)));
        applyRequest(existing, request);
        SystemRule saved = systemRuleRepository.save(existing);
        return toResponse(saved);
    }

    private void applyRequest(SystemRule target, SystemRuleRequest request) {
        target.setCategory(request.category());
        target.setKey(request.key());
        target.setScope(Objects.requireNonNullElse(request.scope(), RuleScope.GLOBAL));
        target.setScopeReference(normalizeScopeReference(request.scopeReference()));
        target.setPriority(Objects.requireNonNullElse(request.priority(), 0));
        target.setStatus(Objects.requireNonNullElse(request.status(), RuleStatus.DRAFT));
        target.setValueType(Objects.requireNonNullElse(request.valueType(), RuleValueType.JSON));
        target.setPayload(request.valuePayload());
        target.setConditions(request.conditions());
        target.setEffectiveFrom(Objects.requireNonNullElse(request.effectiveFrom(), OffsetDateTime.now(ZoneOffset.UTC)));
        target.setEffectiveTo(request.effectiveTo());
    }

    private String normalizeScopeReference(String scopeReference) {
        if (!StringUtils.hasText(scopeReference)) {
            return null;
        }
        return scopeReference.trim();
    }

    private SystemRuleResponse toResponse(SystemRule rule) {
        return new SystemRuleResponse(
                rule.getUuid(),
                rule.getCategory(),
                rule.getKey(),
                rule.getScope(),
                rule.getScopeReference(),
                rule.getPriority(),
                rule.getStatus(),
                rule.getValueType(),
                rule.getPayload(),
                rule.getConditions(),
                rule.getEffectiveFrom(),
                rule.getEffectiveTo(),
                rule.getCreatedDate(),
                rule.getCreatedBy(),
                rule.getLastModifiedDate(),
                rule.getLastModifiedBy()
        );
    }
}
