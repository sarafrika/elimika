/**
 * Central module for platform-wide configuration rules (fees, eligibility constraints, etc.).
 * Declared as an OPEN module so other feature packages can interact with it directly.
 */
@ApplicationModule(
        type = ApplicationModule.Type.OPEN,
        allowedDependencies = {"shared"}
)
package apps.sarafrika.elimika.systemconfig;

import org.springframework.modulith.ApplicationModule;
