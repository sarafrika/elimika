package apps.sarafrika.elimika.course.internal.training;

import java.util.UUID;

/** Read-only facts about an application that live outside its own row. */
public record TrainingApplicationExtras(UUID pendingRateUpdateUuid) {

    public static final TrainingApplicationExtras NONE = new TrainingApplicationExtras(null);
}
