package apps.sarafrika.elimika.student.service;

import apps.sarafrika.elimika.student.dto.LearnerSkillGoalDTO;

import java.util.List;
import java.util.UUID;

/** A learner's declared skill goals, used by course recommendations to find their skill gap. */
public interface LearnerSkillGoalService {

    /** The learner's goals, oldest first. 404 for an unknown student. */
    List<LearnerSkillGoalDTO> getGoals(UUID studentUuid);

    /**
     * Replaces the learner's goals with {@code skillUuids}. Every skill must exist in the taxonomy; a
     * newly added one must be active; duplicates are rejected (400). Goals kept from before keep their
     * original date.
     */
    List<LearnerSkillGoalDTO> replaceGoals(UUID studentUuid, List<UUID> skillUuids);
}
