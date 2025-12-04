package apps.sarafrika.elimika.shared.spi;

import java.util.UUID;

/**
 * Cross-module contract for class capacity checks and waitlist eligibility.
 */
public interface ClassCapacityService {

    /**
     * Determines if any scheduled instance for the class definition can accept new enrollments.
     *
     * @param classDefinitionUuid class definition identifier
     * @return true if capacity is available, false otherwise
     */
    boolean hasCapacity(UUID classDefinitionUuid);

    /**
     * Indicates whether waitlisting is enabled for the class definition.
     *
     * @param classDefinitionUuid class definition identifier
     * @return true if waitlisting is allowed, false otherwise
     */
    boolean isWaitlistEnabled(UUID classDefinitionUuid);
}
