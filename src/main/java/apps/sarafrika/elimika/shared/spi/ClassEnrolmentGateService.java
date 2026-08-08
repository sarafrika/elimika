package apps.sarafrika.elimika.shared.spi;

import java.util.Optional;
import java.util.UUID;

/**
 * Cross-module contract for the compliance rules that decide whether a learner may join a class.
 * <p>
 * Enrolment happens after payment is captured, so any rule discovered at enrolment time is a charge
 * with nothing delivered. Commerce consults this before taking money.
 */
public interface ClassEnrolmentGateService {

    /**
     * Why this buyer may not join, judged from the records the platform already holds — their
     * recorded date of birth against the course's age limits, seats, and existing enrolment.
     * <p>
     * Takes the buyer's <em>user</em> UUID so the caller needs no knowledge of student profiles;
     * the student behind the user is resolved on the other side of this contract.
     *
     * @return the reason to show the learner, or empty when they may join
     */
    Optional<String> findEnrolmentBlocker(UUID classDefinitionUuid, UUID userUuid);
}
