package apps.sarafrika.elimika.course.internal;

import java.util.UUID;

/** A learner's level for a programme skill moved into a level that counts as achieved. */
public record LearnerSkillAchievedEvent(UUID studentUuid, UUID programUuid, UUID skillUuid, String levelName,
                                        int levelRank, UUID evidenceUuid) {
}
