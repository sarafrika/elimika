/** User-owned professional profile and skills wallet shared by every domain; other modules use {@code profile-spi}. */
@ApplicationModule(
        allowedDependencies = {"shared", "skills :: skills-spi"}
)
package apps.sarafrika.elimika.profile;

import org.springframework.modulith.ApplicationModule;
