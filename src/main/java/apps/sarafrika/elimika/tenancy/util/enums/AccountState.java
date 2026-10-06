package apps.sarafrika.elimika.tenancy.util.enums;

/**
 * What the UI should show a signed-in user. Derived from their domain mappings, never stored.
 */
public enum AccountState {

    /** At least one domain is approved, so a dashboard is available. */
    ACTIVE,

    /** Nothing approved yet and at least one domain awaits review. */
    PENDING_APPROVAL,

    /** Nothing approved or pending; an approved domain was withdrawn. */
    SUSPENDED,

    /** Nothing approved or pending; every request was turned down. */
    REJECTED,

    /** The user holds no domain and has not asked for one. */
    NO_DOMAIN
}
