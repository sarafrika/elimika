/**
 * The user-owned professional profile and skills wallet, shared by every domain a user holds.
 * Instructor and course creator profiles read and write it through {@code profile :: profile-spi}.
 */
@ApplicationModule(
        allowedDependencies = {"shared", "skills :: skills-spi"}
)
package apps.sarafrika.elimika.profile;

import org.springframework.modulith.ApplicationModule;
