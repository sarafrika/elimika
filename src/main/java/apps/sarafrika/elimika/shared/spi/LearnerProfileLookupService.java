package apps.sarafrika.elimika.shared.spi;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

/**
 * The few learner facts course recommendations need from the student module, without the course module
 * depending on it (student already depends on course) and without any date of birth leaving tenancy.
 */
public interface LearnerProfileLookupService {

    /** The student profile of a user, if they have one. */
    Optional<UUID> findStudentUuidByUserUuid(UUID userUuid);

    /**
     * The learner's age in whole years on {@code asOf}. Empty when the student is unknown or has no date
     * of birth on record. Only the age is returned; the date of birth stays in tenancy.
     */
    OptionalInt findLearnerAge(UUID studentUuid, LocalDate asOf);

    /** Skills (taxonomy uuids) the learner has declared as goals, oldest first. */
    List<UUID> findSkillGoalUuids(UUID studentUuid);

    /**
     * Whether {@code guardianUserUuid} holds an active guardian link to the student whose share scope
     * covers academics ({@code FULL} or {@code ACADEMICS}).
     */
    boolean guardianCanViewAcademics(UUID guardianUserUuid, UUID studentUuid);
}
