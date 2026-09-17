package apps.sarafrika.elimika.course.internal.training;

import apps.sarafrika.elimika.course.dto.TrainingRateFloorFlagsDTO;

import java.util.UUID;

/** Read-only facts about an application that live outside its own row; floor flags are set for the owner only. */
public record TrainingApplicationExtras(UUID pendingRateUpdateUuid, TrainingRateFloorFlagsDTO rateFloorFlags) {

    public static final TrainingApplicationExtras NONE = new TrainingApplicationExtras(null, null);

    public TrainingApplicationExtras withRateFloorFlags(TrainingRateFloorFlagsDTO flags) {
        return new TrainingApplicationExtras(pendingRateUpdateUuid, flags);
    }
}
