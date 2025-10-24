/**
 * Commerce module orchestrates purchasing workflows backed by Medusa.
 * Submodules expose functionality via named interfaces while persistence and
 * integration details remain internal.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Commerce",
        allowedDependencies = {"shared", "tenancy", "commerce.purchase :: commerce-purchase-spi"}
)
package apps.sarafrika.elimika.commerce;
