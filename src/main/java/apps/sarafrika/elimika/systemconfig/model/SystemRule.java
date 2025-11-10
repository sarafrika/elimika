package apps.sarafrika.elimika.systemconfig.model;

import apps.sarafrika.elimika.shared.model.BaseEntity;
import apps.sarafrika.elimika.systemconfig.enums.RuleCategory;
import apps.sarafrika.elimika.systemconfig.enums.RuleScope;
import apps.sarafrika.elimika.systemconfig.enums.RuleStatus;
import apps.sarafrika.elimika.systemconfig.enums.RuleValueType;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

/**
 * Represents a configurable rule that can be evaluated at runtime to orchestrate platform-wide policies.
 */
@Entity
@Table(name = "system_rules")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SystemRule extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_category")
    private RuleCategory category;

    @Column(name = "rule_key")
    private String key;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_scope")
    private RuleScope scope;

    /**
     * Free-form identifier that helps narrow the scope (tenant UUID, ISO country code, etc.).
     */
    @Column(name = "scope_reference")
    private String scopeReference;

    @Column(name = "priority")
    private Integer priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_status")
    private RuleStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "value_type")
    private RuleValueType valueType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "value_payload")
    private JsonNode payload;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "conditions")
    private JsonNode conditions;

    @Column(name = "effective_from")
    private OffsetDateTime effectiveFrom;

    @Column(name = "effective_to")
    private OffsetDateTime effectiveTo;
}
