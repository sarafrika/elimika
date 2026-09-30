/**
 * Skills taxonomy: the admin-curated list of skills that courses, marketplace jobs and instructor
 * profiles are tagged with.
 * <p>
 * A leaf module. It owns the {@code skills} table and knows nothing about who tags with it; other
 * modules reach it only through {@code skills :: skills-spi} and keep their own tag tables.
 */
@ApplicationModule(
        allowedDependencies = {"shared"}
)
package apps.sarafrika.elimika.skills;

import org.springframework.modulith.ApplicationModule;
