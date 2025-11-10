package apps.sarafrika.elimika.systemconfig.enums;

/**
 * Defines how broadly a rule applies. Use {@link #GLOBAL} for catch-all defaults and
 * refine with tenant, region or demographic scopes when overrides are necessary.
 */
public enum RuleScope {
    GLOBAL,
    TENANT,
    REGION,
    DEMOGRAPHIC,
    SEGMENT
}
