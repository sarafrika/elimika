package apps.sarafrika.elimika.shared.spi;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Finds a learner a guardian is already linked to by name and date of birth, so a second route to the same
 * child (the guardian adding them, an organisation inviting the guardian for them) reuses that learner.
 * <p>
 * Lives in {@code shared} because tenancy needs the answer but may not depend on the student module (student
 * already depends on tenancy). The student module implements it.
 */
public interface GuardianChildLookupService {

    /**
     * The user of a learner linked to {@code guardianUserUuid} (active or pending) whose name matches,
     * ignoring case and repeated spaces, and whose date of birth is {@code dob}.
     */
    Optional<UUID> findChildUserUuid(UUID guardianUserUuid, String firstName, String lastName, LocalDate dob);
}
