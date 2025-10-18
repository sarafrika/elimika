@org.springframework.modulith.ApplicationModule(
        type = ApplicationModule.Type.OPEN,
        allowedDependencies = {"authentication :: spi", "tenancy"}
)
package apps.sarafrika.elimika.shared;

import org.springframework.modulith.ApplicationModule;