package apps.sarafrika.elimika.systemconfig.enums;

/**
 * Lifecycle stages for a rule. Only {@link #ACTIVE} rules influence runtime decisions.
 */
public enum RuleStatus {
    DRAFT,
    ACTIVE,
    INACTIVE
}
