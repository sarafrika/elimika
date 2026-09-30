package apps.sarafrika.elimika.shared.spi;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

/**
 * Tells a module which of its learners are minors, without handing it anyone's date of birth.
 * <p>
 * Lives in {@code shared} because the course module needs the answer but may not depend on the student
 * module (student already depends on course). The student module implements it: it maps students to
 * users and asks tenancy, which owns {@code users.dob}, which of those users are under age.
 */
public interface MinorLearnerLookupService {

    /** Age at which a learner stops counting as a minor. */
    int AGE_OF_MAJORITY = 18;

    /**
     * The subset of {@code studentUuids} who are younger than {@link #AGE_OF_MAJORITY} on {@code asOf}.
     * A learner with no date of birth on record, or an unknown student uuid, is not returned: a missing
     * date of birth counts as an adult.
     */
    Set<UUID> findMinorStudentUuids(Collection<UUID> studentUuids, LocalDate asOf);
}
