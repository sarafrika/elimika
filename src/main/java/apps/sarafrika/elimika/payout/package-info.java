/**
 * Payout module records what earners are owed and, where the platform holds the money, pays them.
 * <p>
 * Two obligations live here, and they are deliberately different in kind.
 * <p>
 * <strong>Marketplace earnings.</strong> When a commerce order is captured the module reacts to
 * {@code OrderCompletedEvent}, resolves the earning party and their revenue share per purchased item,
 * and applies idempotent wallet credits. The platform took the buyer's money, so it settles this
 * itself.
 * <p>
 * <strong>Instructor obligations.</strong> When a class session is delivered the module reacts to
 * {@code ClassSessionCompletedEvent} and writes one {@code instructor_obligations} row per session,
 * snapshotting the training fee that stood at that moment. This money is <em>not</em> settled through
 * a wallet: the organisation pays its instructor by its own means and records the fact here against
 * its own reference. The platform holds no organisation float and no legal basis to take custody of
 * an organisation's payroll, so it tracks the debt rather than carrying it. The columns are a strict
 * subset of what on-platform settlement would need, so that remains an additive change.
 * <p>
 * Both are persistent Spring Modulith event listeners, so work that fails is left as an incomplete
 * row in {@code event_publication} rather than lost, and is retried on a schedule and on restart.
 * Neither ever runs on the caller's thread, so a payout failure cannot fail an order or stop a class
 * from being marked complete.
 * <p>
 * The module deliberately does not depend on {@code classes}, {@code timetabling} or {@code tenancy}:
 * it hears about sessions through {@code shared} events, reads class attributes through
 * {@code shared} lookup SPIs, and is read back by {@code classes} through
 * {@code shared.spi.payout}. Organisation authorization is applied at the controller through
 * {@code @organisationSecurityService}, which is a SpEL bean reference and so introduces no
 * compile-time edge to {@code tenancy}.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Payout",
        allowedDependencies = {
                "shared",
                "course::course-spi",
                "instructor::instructor-spi",
                "wallet::wallet-spi"
        }
)
package apps.sarafrika.elimika.payout;
